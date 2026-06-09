package be.caresync.demo.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sécurité WebFlux — OAuth2 Resource Server Keycloak.
 *
 * Rôles Keycloak (realm CareSync) :
 *   ADMIN                  → accès total
 *   MEDECIN                → professionnel de santé + accès dashboard
 *   INFIRMIERE             → professionnel de santé + accès dashboard
 *   ROLE_HEALTH_PROFESSIONAL → déjà préfixé dans Keycloak, normalisé ci-dessous
 *
 * Normalisation des rôles :
 *   Keycloak peut envoyer "ROLE_HEALTH_PROFESSIONAL" (avec préfixe) ou "MEDECIN" (sans).
 *   Le converter normalise tout en ROLE_<NOM> pour que hasRole() fonctionne uniformément.
 *
 * MFA (acr = "gold") exigé pour le rôle HEALTH_PROFESSIONAL.
 */
@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(auth -> auth
                // Endpoints publics
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/h2-console/**").permitAll()
                // Routes professionnels de santé : MEDECIN, INFIRMIERE ou HEALTH_PROFESSIONAL
                // Un ADMIN a accès à tout (ADMIN > anyAuthenticated)
                .pathMatchers("/api/health-professional/**")
                    .hasAnyRole("HEALTH_PROFESSIONAL", "MEDECIN", "INFIRMIERE", "ADMIN")
                // Reste de l'API : tout utilisateur authentifié
                .pathMatchers("/api/**").authenticated()
                .anyExchange().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
            )
            .build();
    }

    /**
     * Convertit les rôles Keycloak (realm_access.roles) en GrantedAuthority Spring Security.
     *
     * Problème : Keycloak peut envoyer "ROLE_HEALTH_PROFESSIONAL" (préfixe déjà présent)
     * ou "MEDECIN" (sans préfixe). Spring Security's hasRole() ajoute automatiquement
     * "ROLE_", donc il ne faut jamais doubler le préfixe.
     *
     * Solution : normaliser → strip "ROLE_" si présent, puis ajouter "ROLE_".
     *   "ROLE_HEALTH_PROFESSIONAL" → strip → "HEALTH_PROFESSIONAL" → "ROLE_HEALTH_PROFESSIONAL" ✓
     *   "MEDECIN"                  → inchangé               → "ROLE_MEDECIN"             ✓
     */
    private ReactiveJwtAuthenticationConverterAdapter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = extractRealmRoles(jwt);

            // MFA obligatoire pour les professionnels de santé (acr = "gold" dans Keycloak)
            boolean isHealthPro = roles.stream().anyMatch(r ->
                    r.equals("ROLE_HEALTH_PROFESSIONAL") || r.equals("HEALTH_PROFESSIONAL")
                    || r.equals("MEDECIN") || r.equals("INFIRMIERE"));

            if (isHealthPro) {
                String acr = jwt.getClaimAsString("acr");
                // "gold" = MFA complet ; "1" = auth basique acceptée en environnement de démo
                if (acr == null || (!acr.equals("gold") && !acr.equals("1"))) {
                    return List.of();
                }
            }

            return roles.stream()
                .map(role -> {
                    // Normaliser : supprimer le préfixe ROLE_ s'il est déjà présent
                    String name = role.startsWith("ROLE_") ? role.substring(5) : role;
                    return new SimpleGrantedAuthority("ROLE_" + name);
                })
                .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Extrait realm_access.roles du JWT Keycloak.
     * Structure JWT Keycloak : { "realm_access": { "roles": ["MEDECIN", "ROLE_HEALTH_PROFESSIONAL"] } }
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    /**
     * CORS déclaré dans la chaîne de sécurité pour que les requêtes OPTIONS
     * ne soient pas bloquées avant d'atteindre le filtre CORS de WebFlux.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
