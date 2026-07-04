package be.caresync.alert.processor;

import be.caresync.common.events.AlertCreatedEvent;
import be.caresync.common.events.IoTObservationEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.time.Instant;
import java.util.UUID;

/**
 * AlertProcessor — Moteur d'alertes cliniques en Quarkus.
 *
 * Comparaison avec Spring Boot :
 * ─────────────────────────────────────────────────────────────────
 * Spring Boot                        | Quarkus
 * @KafkaListener(topics="...",       | @Incoming("iot-observations")
 *   groupId="...", containerFactory) |
 * kafkaTemplate.send(...)            | @Outgoing("clinical-alerts")
 * if (alert) send(); else return;    | return Uni.createFrom.item() ou null
 * ConsumerRecord<K,V>               | directement le type V
 * ─────────────────────────────────────────────────────────────────
 *
 * Le pipeline @Incoming → @Outgoing est DÉCLARATIF.
 * SmallRye gère : consumer groups, acks, retry, backpressure.
 *
 * Configuration application.properties :
 *   mp.messaging.incoming.iot-observations.connector=smallrye-kafka
 *   mp.messaging.incoming.iot-observations.topic=caresync.iot.observations
 *   mp.messaging.incoming.iot-observations.group.id=caresync-alert-engine
 *   mp.messaging.incoming.iot-observations.failure-strategy=dead-letter-queue
 *   mp.messaging.incoming.iot-observations.dead-letter-queue.topic=caresync.iot.observations.dlq
 *
 *   mp.messaging.outgoing.clinical-alerts.connector=smallrye-kafka
 *   mp.messaging.outgoing.clinical-alerts.topic=caresync.alerts
 *   mp.messaging.outgoing.clinical-alerts.key.serializer=...StringSerializer
 */
@ApplicationScoped
@Slf4j
public class AlertProcessor {

    /**
     * Pipeline Kafka → Kafka.
     *
     * Retourner null = pas d'alerte (l'événement est filtré silencieusement).
     * Retourner un AlertCreatedEvent = publié automatiquement sur caresync.alerts.
     *
     * Spring Boot nécessite 30+ lignes pour le même résultat.
     */
    @Incoming("iot-observations")
    @Outgoing("clinical-alerts")
    public Uni<AlertCreatedEvent> evaluate(IoTObservationEvent obs) {
        String severity = evaluateSeverity(obs);

        if ("NORMAL".equals(severity)) {
            return Uni.createFrom().nullItem();  // Pas d'alerte → rien publié
        }

        AlertCreatedEvent alert = AlertCreatedEvent.builder()
                .alertId(UUID.randomUUID().toString())
                .patientId(obs.getPatientId())
                .deviceSerial(obs.getDeviceSerial())
                .deviceType(obs.getDeviceType())
                .loincCode(obs.getLoincCode())
                .severity(severity)
                .message(buildMessage(obs, severity))
                .triggeredValue("%.2f %s".formatted(obs.getValue(), obs.getUnit()))
                .thresholdType(getThreshold(obs))
                .createdAt(Instant.now())
                .build();

        log.warn("🔴 ALERTE {} | patient={} | {} {}",
                severity, obs.getPatientId(), obs.getValue(), obs.getUnit());

        return Uni.createFrom().item(alert);
    }

    // ── Évaluation clinique ───────────────────────────────────────────

    private String evaluateSeverity(IoTObservationEvent obs) {
        if (obs.getDeviceType() == null) return "NORMAL";
        return switch (obs.getDeviceType()) {
            case "GLUCOMETER" -> {
                if (obs.getValue() < 0.60 || obs.getValue() > 4.00) yield "CRITIQUE";
                if (obs.getValue() < 0.80 || obs.getValue() > 2.50) yield "URGENTE";
                yield "NORMAL";
            }
            case "PULSE_OXIMETER" -> {
                if (obs.getValue() < 85.0) yield "CRITIQUE";
                if (obs.getValue() < 90.0) yield "URGENTE";
                yield "NORMAL";
            }
            case "SCALE"      -> obs.getValue() > 84.0 ? "URGENTE" : "NORMAL";
            case "BP_MONITOR" -> obs.getValue() > 180.0 ? "CRITIQUE" :
                                  obs.getValue() > 160.0 ? "URGENTE" : "NORMAL";
            default -> "NORMAL";
        };
    }

    private String buildMessage(IoTObservationEvent obs, String severity) {
        return switch (obs.getDeviceType()) {
            case "GLUCOMETER" -> obs.getValue() < 0.80
                ? "Hypoglycémie %s : %.2f g/L".formatted(severity.toLowerCase(), obs.getValue())
                : "Hyperglycémie %s : %.2f g/L".formatted(severity.toLowerCase(), obs.getValue());
            case "PULSE_OXIMETER" ->
                "Désaturation SpO2 : %.0f%% (%s)".formatted(obs.getValue(), severity.toLowerCase());
            default ->
                "Valeur anormale : %.2f %s (%s)".formatted(obs.getValue(), obs.getUnit(), severity.toLowerCase());
        };
    }

    private String getThreshold(IoTObservationEvent obs) {
        return switch (obs.getDeviceType()) {
            case "GLUCOMETER" -> obs.getValue() < 0.80 ? "< 0.60-0.80 g/L" : "> 2.50-4.00 g/L";
            case "PULSE_OXIMETER" -> obs.getValue() < 85 ? "< 85% SpO2" : "< 90% SpO2";
            default -> "seuil clinique";
        };
    }
}
