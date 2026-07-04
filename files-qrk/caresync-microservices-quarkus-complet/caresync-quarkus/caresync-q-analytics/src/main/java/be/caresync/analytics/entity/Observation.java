package be.caresync.analytics.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Modèle de lecture de la table "observations" (propriété du service IoT).
 * Sert uniquement à donner à Hibernate ORM une entité JPA afin qu'il active
 * l'unité de persistance : AnalyticsResource interroge cette table via SQL
 * natif (time_bucket TimescaleDB), pas via Panache.
 */
@Entity
@Table(name = "observations")
public class Observation extends PanacheEntityBase {

    @Id
    private String id;
}
