package be.caresync.messaging.kafka.producer;

import be.caresync.messaging.kafka.events.AlertCreatedEvent;
import be.caresync.messaging.kafka.events.IoTObservationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC_OBSERVATIONS = "caresync.iot.observations";
    private static final String TOPIC_ALERTS        = "caresync.alerts";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Clé = patientId pour garantir l'ordre des messages par patient
    public void publishObservation(IoTObservationEvent event) {
        String key = event.getPatientId().toString();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_OBSERVATIONS, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Échec publication Kafka observation — key={} : {}", key, ex.getMessage());
            } else {
                log.debug("Observation publiée — partition={} offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void publishAlert(AlertCreatedEvent event) {
        String key = event.getPatientId().toString();
        kafkaTemplate.send(TOPIC_ALERTS, key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Échec publication alerte Kafka : {}", ex.getMessage());
                    else log.info("Alerte publiée Kafka — alertId={}", event.getAlertId());
                });
    }
}
