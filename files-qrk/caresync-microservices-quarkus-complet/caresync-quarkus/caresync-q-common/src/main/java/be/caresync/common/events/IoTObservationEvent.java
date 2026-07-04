package be.caresync.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Événement d'observation IoT (glucomètre, oxymètre, balance, tensiomètre...) publié sur Kafka. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IoTObservationEvent {

    private String patientId;
    private String deviceSerial;
    private String deviceType;   // GLUCOMETER / PULSE_OXIMETER / SCALE / BP_MONITOR
    private String loincCode;
    private double value;
    private String unit;
    private long ts;             // epoch millis à l'émission par le device

    private long latencyMs;      // enrichi par IoTProcessor
    private String severity;     // enrichi par IoTProcessor

    public String computeSeverity() {
        if (deviceType == null) return "NORMAL";
        return switch (deviceType) {
            case "GLUCOMETER" -> {
                if (value < 0.60 || value > 4.00) yield "CRITIQUE";
                if (value < 0.80 || value > 2.50) yield "URGENTE";
                yield "NORMAL";
            }
            case "PULSE_OXIMETER" -> {
                if (value < 85.0) yield "CRITIQUE";
                if (value < 90.0) yield "URGENTE";
                yield "NORMAL";
            }
            case "SCALE" -> value > 84.0 ? "URGENTE" : "NORMAL";
            case "BP_MONITOR" -> value > 180.0 ? "CRITIQUE" : value > 160.0 ? "URGENTE" : "NORMAL";
            default -> "NORMAL";
        };
    }
}
