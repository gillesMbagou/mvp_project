package be.caresync.demo.model.db;

import jakarta.persistence.*;
import lombok.*;

/**
 * Dispositif médical simulé associé à un patient.
 */
@Entity
@Table(name = "devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedDevice {

    @Id
    private String serial;

    private String patientId;
    private String deviceType;    // GLUCOMETER / PULSE_OXIMETER / BP_MONITOR / SCALE / SPIROMETER
    private String manufacturer;
    private String model;
    private String loincCode;
    private String unit;
    private String mqttTopicSuffix; // glucose / spo2 / heartrate / weight

    // Fréquence de mesure (en secondes)
    private int intervalSeconds;

    // Scénario actif
    private String currentScenario; // NORMAL / HYPOGLYCEMIE / HYPERGLYCEMIE / DECOMPENSATION...
    private boolean active;
}
