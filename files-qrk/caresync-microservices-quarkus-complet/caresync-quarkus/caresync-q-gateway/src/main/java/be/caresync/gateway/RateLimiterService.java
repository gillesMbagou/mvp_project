// caresync-q-gateway/src/main/java/be/caresync/gateway/RateLimiterService.java
package be.caresync.gateway;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Set;

/**
 * Rate limiting sliding window via Redis.
 *
 * Algorithme : Token Bucket simplifié
 *   - Clé Redis : "rl:{userId}:{window_second}"
 *   - Expire dans 2 secondes (sliding)
 *   - Limite : configurable par rôle (médecin = 60/s, infirmier = 30/s)
 */
@ApplicationScoped
public class RateLimiterService {

    @Inject ReactiveRedisDataSource redis;

    private static final int LIMIT_MEDECIN   = 60;
    private static final int LIMIT_INFIRMIER = 30;
    private static final int LIMIT_DEFAULT   = 20;

    public Uni<Boolean> checkLimit(String userId, String path, Set<String> roles) {
        // Fenêtre de 1 seconde
        long window  = System.currentTimeMillis() / 1000;
        String key   = "rl:" + userId + ":" + window;
        int limit    = resolveLimit(path, roles);

        ReactiveValueCommands<String, Long> cmds = redis.value(Long.class);
        ReactiveKeyCommands<String> keyCmds = redis.key();

        return cmds.incr(key)
            .onItem().call(count -> {
                if (count == 1) {
                    // Première requête de la fenêtre → poser le TTL
                    return keyCmds.pexpire(key, Duration.ofSeconds(2));
                }
                return Uni.createFrom().voidItem();
            })
            .map(count -> count <= limit)
            .onFailure().recoverWithItem(true); // fail-open si Redis down
    }

    private int resolveLimit(String path, Set<String> roles) {
        // Les endpoints SSE sont exemptés (connexion persistante)
        if (path.startsWith("/api/stream")) return Integer.MAX_VALUE;
        if (path.startsWith("/api/v1/audit")) return 10;  // audit = flux faible

        if (roles != null) {
            if (roles.contains("medecin"))   return LIMIT_MEDECIN;
            if (roles.contains("infirmier")) return LIMIT_INFIRMIER;
        }
        return LIMIT_DEFAULT;
    }
}