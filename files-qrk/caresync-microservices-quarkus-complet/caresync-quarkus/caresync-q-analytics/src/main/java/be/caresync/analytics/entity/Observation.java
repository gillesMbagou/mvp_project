package be.caresync.analytics.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Copie locale (base "caresync_analytics") des observations IoT, alimentée
 * par ObservationIngestConsumer depuis le topic Kafka caresync.iot.observations.
 * AnalyticsResource interroge cette table via SQL natif (time_bucket
 * TimescaleDB), pas via Panache — les colonnes ci-dessous doivent donc rester
 * alignées avec les noms utilisés dans ces requêtes natives.
 *
 * Clé primaire composite (id, observedAt) : TimescaleDB exige que toute
 * contrainte unique sur une hypertable inclue la colonne de partitionnement
 * ("observed_at"). Un @Id simple sur "id" seul fait échouer
 * create_hypertable() avec "cannot create a unique index without the column
 * ... used in partitioning" — vérifié en le reproduisant sur un cluster kind
 * de test avant ce correctif.
 */
@Entity
@Table(name = "observations")
@IdClass(Observation.ObservationId.class)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Observation extends PanacheEntityBase {

    @Id
    private String id;

    @Id
    private Instant observedAt;

    private String patientId;

    @Column(nullable = false)
    private String deviceType;

    private String loincCode;
    private double value;
    private String unit;
    private String severity;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ObservationId implements Serializable {
        private String id;
        private Instant observedAt;
    }
}
