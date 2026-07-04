package be.caresync.iot.processor;

import be.caresync.common.events.IoTObservationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.*;

import java.time.Instant;

/**
 * IoTProcessor — SmallRye Reactive Messaging.
 *
 * Comparaison avec Spring Boot :
 * ─────────────────────────────────────────────────────────────────
 * Spring Boot (Spring Integration)    | Quarkus (SmallRye)
 * @ServiceActivator(inputChannel=...) | @Incoming("mqtt-glucose")
 * MqttPahoMessageDrivenChannelAdapter | quarkus-messaging-mqtt extension
 * KafkaTemplate.send(...)             | @Outgoing("observations-out")
 * Sinks.Many (Reactor)                | @Channel Multi<T> (Mutiny)
 * ─────────────────────────────────────────────────────────────────
 *
 * Configuration dans application.properties :
 *   mp.messaging.incoming.mqtt-glucose.connector=smallrye-mqtt
 *   mp.messaging.incoming.mqtt-glucose.host=localhost
 *   mp.messaging.incoming.mqtt-glucose.port=1883
 *   mp.messaging.incoming.mqtt-glucose.topic=caresync/devices/+/glucose
 *
 *   mp.messaging.outgoing.observations-out.connector=smallrye-kafka
 *   mp.messaging.outgoing.observations-out.topic=caresync.iot.observations
 */
@ApplicationScoped
@Slf4j
public class IoTProcessor {

    @Inject
    ObjectMapper objectMapper;

    /**
     * MQTT → Kafka bridge.
     *
     * SmallRye gère automatiquement :
     *   - La connexion MQTT (reconnexion auto)
     *   - La publication Kafka (retry, acks)
     *   - L'accusé de réception MQTT après succès Kafka
     *
     * C'est l'équivalent du MqttToKafkaBridge Spring Boot
     * mais en 10 lignes au lieu de 80.
     */
    @Incoming("mqtt-glucose")   // Source: MQTT via smallrye-mqtt
    @Outgoing("observations-out") // Destination: Kafka topic
    public IoTObservationEvent processGlucose(byte[] payload) {
        return enrichObservation(payload, "GLUCOMETER");
    }

    @Incoming("mqtt-spo2")
    @Outgoing("observations-out")
    public IoTObservationEvent processSpo2(byte[] payload) {
        return enrichObservation(payload, "PULSE_OXIMETER");
    }

    @Incoming("mqtt-weight")
    @Outgoing("observations-out")
    public IoTObservationEvent processWeight(byte[] payload) {
        return enrichObservation(payload, "SCALE");
    }

    private IoTObservationEvent enrichObservation(byte[] payload, String deviceType) {
        try {
            IoTObservationEvent obs = objectMapper.readValue(payload, IoTObservationEvent.class);

            // Enrichissement latence
            long latency = Instant.now().toEpochMilli() - obs.getTs();
            obs.setLatencyMs(latency);

            // Évaluation sévérité
            obs.setSeverity(obs.computeSeverity());

            log.debug("→ Kafka [{}] {} {} [{}] latence={}ms",
                    deviceType, obs.getValue(), obs.getUnit(), obs.getSeverity(), latency);

            return obs;

        } catch (Exception ex) {
            log.error("Erreur parsing MQTT payload : {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
