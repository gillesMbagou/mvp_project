package be.caresync.demo.consumer;

import be.caresync.demo.model.GlucoseObservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Consommateur Kafka Spring Boot.
 * Remplace GlucoseMonitor.java standalone.
 *
 * - Consomme glucose.raw
 * - Évalue les seuils cliniques
 * - Publie les alertes sur glucose.alerts
 * - Affiche dans les logs avec couleurs ANSI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlucoseKafkaConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.topics.glucose-alerts:glucose.alerts}")
    private String alertsTopic;

    // Seuils cliniques (g/L)
    private static final double CRITICAL_LOW  = 0.60;
    private static final double WARNING_LOW   = 0.80;
    private static final double WARNING_HIGH  = 2.50;
    private static final double CRITICAL_HIGH = 4.00;

    // ANSI colors
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RESET  = "\u001B[0m";

    @KafkaListener(
        topics           = "${app.topics.glucose-raw:glucose.raw}",
        groupId          = "${spring.kafka.consumer.group-id:caresync-java-monitor}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, GlucoseObservation> record) {
        GlucoseObservation obs = record.value();

        String severity = evaluate(obs.getValue());
        String icon     = icon(severity);
        String color    = color(severity);

        // Affichage terminal
        System.out.printf(
            "%n%s%s─────────────────────────────────────────%s%n",
            BOLD, color, RESET
        );
        System.out.printf(
            "  %s  %s%s%s | Dispositif : %s%s%n",
            icon, BOLD, color, obs.getDeviceName(), obs.getSerial(), RESET
        );
        System.out.printf(
            "  Glycémie  : %s%.2f g/L%s  (%.0f mg/dL)%n",
            BOLD, obs.getValue(), RESET, obs.getValue() * 1000
        );
        System.out.printf(
            "  Évaluation: %s%s%s | LOINC %s%n",
            color, severity, RESET, obs.getLoincCode()
        );
        System.out.printf(
            "  Scénario  : %s | Source: %s | Séquence: #%d%n",
            obs.getScenario(), obs.getSource(), obs.getSequence()
        );
        System.out.printf(
            "  Kafka     : partition=%d  offset=%d  latence=%sms%n",
            record.partition(),
            record.offset(),
            obs.getLatencyMs() != null ? obs.getLatencyMs() : "?"
        );
        System.out.printf(
            "  Timestamp : %s%n", obs.getObservedAt()
        );

        // Publier une alerte si seuil dépassé
        if (!severity.equals("NORMAL")) {
            publishAlert(obs, severity, record);
        }
    }

    // Évaluation clinique

    private String evaluate(double glucose) {
        if (glucose < CRITICAL_LOW || glucose > CRITICAL_HIGH) return "CRITIQUE";
        if (glucose < WARNING_LOW  || glucose > WARNING_HIGH)  return "URGENTE";
        return "NORMAL";
    }

    // Publication de l'alerte sur glucose.alerts

    private void publishAlert(GlucoseObservation obs, String severity,
                              ConsumerRecord<String, GlucoseObservation> record) {
        var alert = new AlertEvent(
                java.util.UUID.randomUUID().toString(),
                obs.getSerial(),
                obs.getDeviceName(),
                severity,
                buildMessage(obs.getValue(), severity),
                "%.2f g/L".formatted(obs.getValue()),
                severity.equals("CRITIQUE")
                    ? (obs.getValue() < CRITICAL_LOW
                        ? "< " + CRITICAL_LOW + " g/L"
                        : "> " + CRITICAL_HIGH + " g/L")
                    : (obs.getValue() < WARNING_LOW
                        ? "< " + WARNING_LOW + " g/L"
                        : "> " + WARNING_HIGH + " g/L"),
                Instant.now().toString(),
                record.partition(),
                record.offset()
        );

        kafkaTemplate.send(alertsTopic, obs.getSerial(), alert)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Erreur publication alerte : " + ex.getMessage());
                    } else {
                        System.out.printf(
                            "  %s→ Alerte publiée sur %s (partition=%d offset=%d)%s%n",
                            RED,
                            alertsTopic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            RESET
                        );
                    }
                });
    }

    private String buildMessage(double value, String severity) {
        return switch (severity) {
            case "CRITIQUE" -> value < CRITICAL_LOW
                ? "Hypoglycémie sévère : %.2f g/L".formatted(value)
                : "Hyperglycémie critique : %.2f g/L".formatted(value);
            case "URGENTE" -> value < WARNING_LOW
                ? "Glycémie basse : %.2f g/L".formatted(value)
                : "Glycémie élevée : %.2f g/L".formatted(value);
            default -> "Glycémie : %.2f g/L".formatted(value);
        };
    }

    private String icon(String severity) {
        return switch (severity) {
            case "CRITIQUE" -> "🔴";
            case "URGENTE"  -> "🟠";
            default         -> "🟢";
        };
    }

    private String color(String severity) {
        return switch (severity) {
            case "CRITIQUE" -> RED;
            case "URGENTE"  -> YELLOW;
            default         -> GREEN;
        };
    }

    //  Record alerte (remplace le JSON manuel)

    record AlertEvent(
        String alertId,
        String deviceSerial,
        String deviceName,
        String severity,
        String message,
        String triggeredValue,
        String thresholdValue,
        String createdAt,
        int    sourcePartition,
        long   sourceOffset
    ) {}
}
