package be.caresync.common.vault;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vault.VaultKVSecretEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Peuple automatiquement le Vault de Dev Services au démarrage, uniquement en profil dev.
 * Évite de dépendre de {@code quarkus.vault.devservices.init-commands}, dont le format exact
 * (préfixe "vault" ajouté implicitement par testcontainers-vault) est fragile et non documenté.
 *
 * <p>Le chemin lu ici est une propriété applicative dédiée ({@code app.vault.demo-secret-path}),
 * volontairement distincte de {@code quarkus.vault.secret-config-kv-path} : cette dernière est lue
 * de façon "eager" par Quarkus au tout début du boot (avant que ce bean CDI ne s'exécute), et fait
 * échouer le démarrage avec une 404 si le chemin n'existe pas encore dans un Vault Dev Services
 * fraîchement créé. En passant par une propriété neutre lue paresseusement via l'API Java
 * {@link VaultKVSecretEngine}, on écrit le secret après coup sans jamais bloquer le boot.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class DevVaultSecretSeeder {

    private static final Logger LOG = Logger.getLogger(DevVaultSecretSeeder.class);

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @ConfigProperty(name = "app.vault.demo-secret-path")
    Optional<String> secretPath;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    void onStart(@Observes StartupEvent event) {
        LOG.infof("DevVaultSecretSeeder démarré (chemin configuré : %s)", secretPath);
        if (secretPath.isEmpty()) {
            LOG.warn("app.vault.demo-secret-path n'est pas défini, aucun secret de démo ne sera écrit.");
            return;
        }
        String path = secretPath.get();
        try {
            Map<String, String> existing = kvSecretEngine.readSecret(path);
            if (existing != null && !existing.isEmpty()) {
                LOG.infof("Vault dev : secret déjà présent sous '%s', rien à faire.", path);
                return;
            }
        } catch (Exception e) {
            LOG.debugf("Lecture initiale de '%s' impossible (%s) : normal si le chemin n'existe pas encore.", path, e.getMessage());
        }
        try {
            kvSecretEngine.writeSecret(path, Map.of("demo.vault.secret", applicationName + "-vault-dev-ok"));
            LOG.infof("Vault dev : secret de démonstration écrit sous '%s'.", path);
        } catch (Exception e) {
            LOG.errorf(e, "Échec de l'écriture du secret de démo Vault sous '%s'.", path);
        }
    }
}
