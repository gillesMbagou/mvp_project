package be.caresync.messaging.kafka.producer;

import be.caresync.messaging.kafka.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // Pourquoi passer une clé (patientId) ?
    //
    // Kafka utilise la clé pour choisir dans quelle partition écrire :
    //   partition = hash(clé) % nombre_de_partitions
    //
    // Résultat : TOUS les messages du patient-A vont toujours en partition 2.
    // Le consommateur de partition 2 les lit dans l'ordre.
    // Sans clé → round-robin → obs1 en partition 0, obs2 en partition 3 → ordre perdu.
    // ─────────────────────────────────────────────────────────────────────────

    public void publishObservation(IoTObservationEvent event) {
        String key = event.getPatientId().toString();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("caresync.iot.observations", key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // Kafka a épuisé ses tentatives — logguer pour investigation
                log.error("Échec définitif publication observation " +
                        "patientId={} : {}", key, ex.getMessage());
            } else {
                log.debug("Observation publiée — partition={} offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void publishAlert(AlertCreatedEvent event) {
        sendWithLogging("caresync.alerts",
                event.getPatientId().toString(), event,
                "alerte alertId=" + event.getAlertId());
    }

    public void publishEscalation(AlertEscalationEvent event) {
        sendWithLogging("caresync.alerts.escalation",
                event.getPatientId().toString(), event,
                "escalade level=" + event.getEscalationLevel());
    }

    public void publishAudit(AuditEvent event) {
        // L'audit n'a pas besoin d'ordre par patient — null key → round-robin
        sendWithLogging("caresync.audit", null, event, "audit " + event.getAction());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper générique avec logging uniformisé
    // ─────────────────────────────────────────────────────────────────────────

    private void sendWithLogging(String topic, String key, Object event, String ctx) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Kafka send échec — {} : {}", ctx, ex.getMessage());
                    else
                        log.info("Kafka ✓ — {} → topic={} partition={} offset={}",
                                ctx, topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                });
    }
}