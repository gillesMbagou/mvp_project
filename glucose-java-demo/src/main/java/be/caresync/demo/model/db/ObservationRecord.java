package be.caresync.demo.model.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Observation médicale persistée en base H2.
 * Générée par le DataSeeder (historique 7 jours)
 * et au fil de l'eau par le simulateur temps réel.
 */
@Entity
@Table(name = "observations",
       indexes = {
           @Index(name = "idx_patient_ts",  columnList = "patient_id, observed_at"),
           @Index(name = "idx_device_ts",   columnList = "device_serial, observed_at"),
           @Index(name = "idx_severity",    columnList = "severity")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String  patientId;
    private String  deviceSerial;
    private String  deviceType;
    private String  loincCode;

    // "value" is a reserved keyword in H2 — mapped to obs_value
    @Column(name = "obs_value")
    private double  value;

    private String  unit;
    private String  severity;      // NORMAL / INFORMATIVE / URGENTE / CRITIQUE
    private String  source;        // SEEDER_HISTORICAL / CGM_SIMULATOR / ...

    @Column(name = "observed_at")
    private Instant observedAt;
}
