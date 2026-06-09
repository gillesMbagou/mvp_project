package be.caresync.demo.model;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlucoseObservation {

    private String  serial;
    private String  deviceName;
    private String  manufacturer;
    private String  model;
    private String  loincCode;      // "14743-9" = glucose interstitiel CGM
    private double  value;          // g/L
    private String  unit;           // "g/L"
    private int     sequence;
    private String  severity;       // NORMAL / INFORMATIVE / URGENTE / CRITIQUE
    private String  scenario;
    private String  source;         // CGM_SIMULATOR / LIBREVIEW_API / BLE_GATT

    private Instant observedAt;

    private long    ts;             // epoch millis
    private String  mqttTopic;
    private Long    latencyMs;
    private String  firmware;
    private int     batteryPct;

    // ── Évaluation clinique ───────────────────────────────────────────────────

    private static final double CRITICAL_LOW  = 0.60;
    private static final double WARNING_LOW   = 0.80;
    private static final double WARNING_HIGH  = 2.50;
    private static final double CRITICAL_HIGH = 4.00;

    public String computeSeverity() {
        if (value < CRITICAL_LOW || value > CRITICAL_HIGH) return "CRITIQUE";
        if (value < WARNING_LOW  || value > WARNING_HIGH)  return "URGENTE";
        if (value < 1.0          || value > 1.8)           return "INFORMATIVE";
        return "NORMAL";
    }

    public String severityIcon() {
        return switch (computeSeverity()) {
            case "CRITIQUE"    -> "🔴";
            case "URGENTE"     -> "🟠";
            case "INFORMATIVE" -> "🔵";
            default            -> "🟢";
        };
    }
}
