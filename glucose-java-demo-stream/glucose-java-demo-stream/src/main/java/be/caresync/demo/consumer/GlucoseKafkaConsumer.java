package be.caresync.demo.consumer;

import be.caresync.demo.model.AlertEvent;
import be.caresync.demo.model.GlucoseObservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlucoseKafkaConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final double CRITICAL_LOW  = 0.60;
    private static final double WARNING_LOW   = 0.80;
    private static final double WARNING_HIGH  = 2.50;
    private static final double CRITICAL_HIGH = 4.00;

    // Durée pendant laquelle un état d'alerte est mémorisé dans Redis
    private static final Duration ALERT_STATE_TTL = Duration.ofMinutes(10);

    private static final String RED    = "[31m";
    private static final String YELLOW = "[33m";
    private static final String GREEN  = "[32m";
    private static final String BOLD   = "[1m";
    private static final String RESET  = "[0m";

    @KafkaListener(
        topics           = "glucose.raw",
        groupId          = "caresync-java-monitor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, GlucoseObservation> record) {
        GlucoseObservation obs = record.value();

        String severity = evaluate(obs.getValue());
        String color    = color(severity);

        System.out.printf(
            "%n%s%s─────────────────────────────────────────%s%n",
            BOLD, color, RESET
        );
        System.out.printf(
            "  %s  %s%s%s | Dispositif : %s%n",
            icon(severity), BOLD, color, obs.getDeviceName(), obs.getSerial(), RESET
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
        System.out.printf("  Timestamp : %s%n", obs.getObservedAt());

        handleAlertDedup(obs, severity, record);
    }

    // ── Déduplication Redis ────────────────────────────────────────────────────

    private void handleAlertDedup(GlucoseObservation obs, String severity,
                                   ConsumerRecord<String, GlucoseObservation> record) {
        String redisKey = "caresync:alert:state:" + obs.getSerial();

        // Lecture de l'état courant depuis Redis (bloquant — thread Kafka, pas Netty)
        String previousSeverity = redisTemplate.opsForValue().get(redisKey).block();

        if ("NORMAL".equals(severity)) {
            if (previousSeverity != null) {
                // Résolution de l'alerte : on supprime la clé
                redisTemplate.delete(redisKey).block();
                log.info("Alerte résolue pour {} (était {})", obs.getSerial(), previousSeverity);
            }
            return;
        }

        if (severity.equals(previousSeverity)) {
            // Même sévérité déjà enregistrée → on ne re-publie pas
            log.debug("Alerte {} déjà connue pour {} — ignorée (déduplication Redis)",
                severity, obs.getSerial());
            return;
        }

        // Nouvel état ou escalade → on met à jour Redis et on publie
        redisTemplate.opsForValue().set(redisKey, severity, ALERT_STATE_TTL).block();
        publishAlert(obs, severity, record);
    }

    // ── Évaluation clinique ────────────────────────────────────────────────────

    private String evaluate(double glucose) {
        if (glucose < CRITICAL_LOW || glucose > CRITICAL_HIGH) return "CRITIQUE";
        if (glucose < WARNING_LOW  || glucose > WARNING_HIGH)  return "URGENTE";
        return "NORMAL";
    }

    // ── Publication sur glucose.alerts ────────────────────────────────────────

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

        kafkaTemplate.send("glucose.alerts", obs.getSerial(), alert)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Erreur publication alerte : {}", ex.getMessage());
                } else {
                    System.out.printf(
                        "  %s→ Alerte publiée sur glucose.alerts (partition=%d offset=%d)%s%n",
                        RED,
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
}
