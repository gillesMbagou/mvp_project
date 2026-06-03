package be.caresync.domain.iot;

import be.caresync.domain.common.BaseEntity;
import be.caresync.domain.patient.Patient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Observation clinique au format FHIR R4 (simplifié).
 * Stocke les mesures issues des dispositifs IoT ou saisies manuellement.
 */
@Entity
@Table(name = "observations", indexes = {
    @Index(name = "idx_obs_patient_date",  columnList = "patient_id, observed_at DESC"),
    @Index(name = "idx_obs_loinc",         columnList = "loinc_code"),
    @Index(name = "idx_obs_device",        columnList = "device_id"),
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Observation extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private IoTDevice device;

    // Code LOINC (ex: "2345-7" pour glycémie capillaire)
    @Column(name = "loinc_code", nullable = false, length = 20)
    private String loincCode;

    @Column(name = "device_type")
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    // Valeur numérique principale
    @NotNull
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(nullable = false, length = 50)
    private String unit;

    // Timestamp de la mesure (peut différer de createdAt si off-line sync)
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ObservationSource source = ObservationSource.IOT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ObservationStatus status = ObservationStatus.FINAL;

    // Commentaire clinique optionnel
    @Column(columnDefinition = "TEXT")
    private String note;

    // Métadonnées MQTT
    @Column(name = "mqtt_topic")
    private String mqttTopic;

    @Column(name = "latency_ms")
    private Long latencyMs;

    public enum ObservationSource { IOT, MANUAL, LAB, IMAGING }
    public enum ObservationStatus  { PRELIMINARY, FINAL, CORRECTED, ENTERED_IN_ERROR }
}
