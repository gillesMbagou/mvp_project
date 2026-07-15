package be.caresync.analytics.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Copie locale (base "caresync_analytics") des observations IoT, alimentée
 * par ObservationIngestConsumer depuis le topic Kafka caresync.iot.observations.
 * AnalyticsResource interroge cette table via SQL natif (time_bucket
 * TimescaleDB), pas via Panache — les colonnes ci-dessous doivent donc rester
 * alignées avec les noms utilisés dans ces requêtes natives.
 */
@Entity
@Table(name = "observations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Observation extends PanacheEntityBase {

    @Id
    private String id;

    private String patientId;

    @Column(nullable = false)
    private String deviceType;

    private String loincCode;
    private double value;
    private String unit;
    private String severity;
    private Instant observedAt;
}
