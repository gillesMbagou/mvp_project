package be.caresync.analytics.processor;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Convertit la table "observations" en hypertable TimescaleDB au démarrage
 * (idempotent via if_not_exists). Hibernate (generation=update) a déjà créé
 * la table à ce stade — @Observes StartupEvent se déclenche après
 * l'initialisation du bootstrap Hibernate.
 *
 * Si l'extension timescaledb n'est pas installée sur la base cible (ex. Dev
 * Services lancé sans image Timescale), on logue un avertissement au lieu de
 * planter l'application : les requêtes time_bucket() de AnalyticsResource
 * échoueront alors explicitement à l'appel, ce qui est plus facile à
 * diagnostiquer qu'un crash au démarrage.
 */
@ApplicationScoped
@Slf4j
public class TimescaleHypertableInitializer {

    @Inject
    EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        try {
            em.createNativeQuery("CREATE EXTENSION IF NOT EXISTS timescaledb").executeUpdate();
            em.createNativeQuery(
                    "SELECT create_hypertable('observations', 'observed_at', if_not_exists => true, migrate_data => true)"
            ).getSingleResult();
            log.info("Hypertable TimescaleDB 'observations' prête (partitionnée par observed_at).");
        } catch (Exception e) {
            log.warn("TimescaleDB indisponible sur cette base ({}) : les requêtes time_bucket() de "
                    + "AnalyticsResource échoueront jusqu'à ce que l'extension timescaledb soit installée.",
                    e.getMessage());
        }
    }
}
