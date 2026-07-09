// caresync-q-gateway/src/main/java/be/caresync/gateway/GatewayProxy.java
package be.caresync.gateway;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Reverse-proxy Quarkus via Vert.x HTTP Client.
 *
 * Responsabilités :
 *   1. Valider le JWT Keycloak (quarkus-oidc)
 *   2. Appliquer le rate limiting Redis (sliding window)
 *   3. Router vers le bon microservice
 *   4. Propager X-User-Id, X-Tenant-Id, X-Request-Id
 *   5. Circuit breaking (SmallRye Fault Tolerance)
 *
 * Différence avec Spring Cloud Gateway :
 *   Spring → déclaratif YAML, filtres auto, Redis intégré
 *   Quarkus → impératif, Vert.x HTTP proxy, contrôle total
 */
@ApplicationScoped
public class GatewayProxy {

    private static final Logger log = Logger.getLogger(GatewayProxy.class);

    @Inject Vertx vertx;
    @Inject JsonWebToken jwt;              // quarkus-oidc injecte le token validé
    @Inject RateLimiterService rateLimiter;
    @Inject CircuitRegistry circuits;

    // Table de routing : préfixe → service interne
    private static final Map<String, ServiceTarget> ROUTES = Map.ofEntries(
        Map.entry("/api/v1/etablissements", new ServiceTarget("caresync-q-etablissement", 8091)),
        Map.entry("/api/v1/patients",       new ServiceTarget("caresync-q-patient",       8097)),
        Map.entry("/api/v1/dossiers",       new ServiceTarget("caresync-q-dossier",       8096)),
        Map.entry("/api/v1/care-plans",     new ServiceTarget("caresync-q-careplan",      8094)),
        Map.entry("/api/v1/prescriptions",  new ServiceTarget("caresync-q-prescription",  8095)),
        Map.entry("/api/v1/iot",            new ServiceTarget("caresync-q-iot",           8092)),
        Map.entry("/api/stream",            new ServiceTarget("caresync-q-iot",           8092)),
        Map.entry("/api/v1/alertes",        new ServiceTarget("caresync-q-alert",         8093)),
        Map.entry("/api/v1/messages",       new ServiceTarget("caresync-q-messaging",     8098)),
        Map.entry("/api/v1/analytics",      new ServiceTarget("caresync-q-analytics",     8099)),
        Map.entry("/api/v1/audit",          new ServiceTarget("caresync-q-audit",         8100))
    );

    /**
     * EventSource (SSE côté navigateur) ne peut pas fixer de header custom —
     * le frontend passe donc le JWT en query param (?token=...) pour /api/stream/*.
     * quarkus-oidc + quarkus.http.auth.permission.api.policy=authenticated
     * n'inspectent que le header Authorization standard : sans cette réécriture,
     * toute connexion SSE via EventSource échoue en 401 avant même d'atteindre
     * proxy() ci-dessous. Priorité basse = exécuté tôt, avant la policy d'auth.
     */
    @RouteFilter(1000)
    void bearerFromQueryParam(RoutingContext ctx) {
        if (ctx.request().getHeader("Authorization") == null) {
            String token = ctx.request().getParam("token");
            if (token != null && !token.isBlank()) {
                ctx.request().headers().add("Authorization", "Bearer " + token);
            }
        }
        ctx.next();
    }

    // Pas de filtre "methods" : @Route matche toutes les méthodes HTTP par défaut (GET/POST/PUT/PATCH/DELETE/...)
    @Route(path = "/api/*")
    public void proxy(RoutingContext ctx) {
        String path   = ctx.request().path();
        String userId = jwt.getSubject();               // validé par quarkus-oidc
        String tenant = jwt.getClaim("tenant_id");

        // 1. Rate limiting Redis (sliding window par userId, limite selon le rôle)
        rateLimiter.checkLimit(userId, path, jwt.getGroups()).subscribe().with(
            allowed -> {
                if (!allowed) {
                    ctx.response().setStatusCode(429)
                        .putHeader("Retry-After", "1")
                        .end("{\"error\":\"Too Many Requests\"}");
                    return;
                }
                // 2. Trouver le service cible
                ServiceTarget target = resolveTarget(path);
                if (target == null) {
                    ctx.response().setStatusCode(404)
                        .end("{\"error\":\"Route introuvable\"}");
                    return;
                }
                // 3. Circuit breaker + forward
                if (circuits.isOpen(target.host())) {
                    ctx.response().setStatusCode(503)
                        .end("{\"error\":\"Service temporairement indisponible\"}");
                    return;
                }
                forwardRequest(ctx, target, userId, tenant);
            },
            err -> ctx.response().setStatusCode(500).end()
        );
    }

    private void forwardRequest(RoutingContext ctx, ServiceTarget target,
                                 String userId, String tenant) {
        // /api/stream/* (SSE) reste ouvert indéfiniment côté client — un idleTimeout
        // de 30s couperait la connexion dès qu'aucune observation n'arrive pendant
        // 30s, ce qui est courant hors simulation active de dispositifs IoT.
        boolean isStream = ctx.request().path().startsWith("/api/stream");
        HttpClient client = vertx.createHttpClient(
            new HttpClientOptions()
                .setDefaultHost(target.host())
                .setDefaultPort(target.port())
                .setConnectTimeout(3000)
                .setIdleTimeout(isStream ? 0 : 30)
        );

        String requestId = java.util.UUID.randomUUID().toString();

        client.request(
            HttpMethod.valueOf(ctx.request().method().name()),
            ctx.request().path() + (ctx.request().query() != null
                ? "?" + ctx.request().query() : "")
        ).onSuccess(req -> {
            // Copier les headers de la requête originale
            ctx.request().headers().forEach(h ->
                req.putHeader(h.getKey(), h.getValue()));

            // Enrichir avec les headers extraits du JWT
            req.putHeader("X-User-Id",    userId);
            req.putHeader("X-Tenant-Id",  tenant);
            req.putHeader("X-Request-Id", requestId);
            req.putHeader("X-User-Roles", String.join(",", jwt.getGroups()));

            // Supprimer Cookie (services stateless)
            req.headers().remove("Cookie");

            // Forwarder le body si présent
            if (ctx.body().buffer() != null) {
                req.send(ctx.body().buffer()).onSuccess(resp -> {
                    forwardResponse(ctx, resp, target, requestId, isStream);
                }).onFailure(err -> handleUpstreamError(ctx, target, err));
            } else {
                req.send().onSuccess(resp -> {
                    forwardResponse(ctx, resp, target, requestId, isStream);
                }).onFailure(err -> handleUpstreamError(ctx, target, err));
            }
        }).onFailure(err -> handleUpstreamError(ctx, target, err));
    }

    private void forwardResponse(RoutingContext ctx,
                                  io.vertx.core.http.HttpClientResponse resp,
                                  ServiceTarget target, String requestId, boolean isStream) {
        circuits.recordSuccess(target.host());

        ctx.response().setStatusCode(resp.statusCode());
        resp.headers().forEach(h -> ctx.response().putHeader(h.getKey(), h.getValue()));
        ctx.response().putHeader("X-Request-Id", requestId);

        if (isStream) {
            // resp.body() bufferise la réponse entière avant de répondre : pour un
            // flux SSE, le body ne se termine jamais, donc le client (EventSource)
            // ne reçoit ni headers ni données tant que la connexion vit. pipeTo()
            // relaie les chunks au fil de l'eau.
            //
            // Vert.x n'envoie les en-têtes qu'au premier write() sur la réponse —
            // sans donnée immédiate (cas normal tant qu'aucun événement Kafka
            // n'arrive), le client resterait bloqué sans même recevoir le
            // 200/Content-Type. setChunked(true) + un write() vide forcent l'envoi
            // immédiat des en-têtes avant que pipeTo() ne prenne le relais.
            //
            // Cache-Control: no-transform — sans Content-Length (cas normal d'un
            // flux chunked infini), les middlewares de compression à la Connect/
            // Express (ex. celui du dev-server Vite utilisé par `ng serve`)
            // bufferisent la réponse entière avant de décider s'ils compressent,
            // ce qui bloque indéfiniment un flux SSE traversant un tel proxy.
            // no-transform leur signale explicitement de ne pas y toucher.
            ctx.response().putHeader("Cache-Control", "no-cache, no-transform");
            ctx.response().setChunked(true);
            ctx.response().write(io.vertx.core.buffer.Buffer.buffer());
            resp.pipeTo(ctx.response());
        } else {
            // Réponses REST classiques : bufferiser tout le body avant de répondre
            // reste le comportement le plus simple/robuste (taille connue à l'avance,
            // pas de round-trip supplémentaire pour forcer le flush des en-têtes).
            resp.body().onSuccess(ctx.response()::end);
        }

        log.debugf("[%s] %s → %s:%d → %d",
            requestId, ctx.request().path(),
            target.host(), target.port(), resp.statusCode());
    }

    private void handleUpstreamError(RoutingContext ctx, ServiceTarget target, Throwable err) {
        circuits.recordFailure(target.host());
        log.errorf("Upstream error [%s]: %s", target.host(), err.getMessage());
        ctx.response().setStatusCode(503)
            .putHeader("Content-Type", "application/json")
            .end("{\"error\":\"" + target.host() + " indisponible\",\"retryIn\":\"5s\"}");
    }

    private ServiceTarget resolveTarget(String path) {
        return ROUTES.entrySet().stream()
            .filter(e -> path.startsWith(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public record ServiceTarget(String host, int port) {}
}