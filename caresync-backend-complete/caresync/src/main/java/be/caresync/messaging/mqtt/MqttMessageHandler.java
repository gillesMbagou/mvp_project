package be.caresync.messaging.mqtt;

import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.IoTDevice;
import be.caresync.domain.iot.IoTDeviceRepository;
import be.caresync.messaging.kafka.events.IoTObservationEvent;
import be.caresync.messaging.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Point d'entrée des messages MQTT provenant des dispositifs IoT.
 * Chaque message est parsé, enrichi avec les métadonnées du dispositif,
 * puis publié sur Kafka pour traitement asynchrone.
 *
 * Format MQTT attendu (JSON) :
 * {
 *   "serial": "GLU-001",
 *   "value":  1.15,
 *   "unit":   "g/L",
 *   "ts":     1714900000000
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MqttMessageHandler {

    private final IoTDeviceRepository   deviceRepo;
    private final KafkaProducerService  kafkaProducer;
    private final ObjectMapper          objectMapper;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> message) {
        Instant received = Instant.now();
        String  topic    = (String) message.getHeaders().get("mqtt_receivedTopic");
        String  payload  = message.getPayload();

        log.debug("MQTT reçu — topic={} payload={}", topic, payload);

        try {
            JsonNode json       = objectMapper.readTree(payload);
            String   serial     = json.path("serial").asText();
            double   rawValue   = json.path("value").asDouble();
            String   unit       = json.path("unit").asText("?");
            long     tsMillis   = json.path("ts").asLong(received.toEpochMilli());
            Instant  observedAt = Instant.ofEpochMilli(tsMillis);

            // Résolution du dispositif
            IoTDevice device = deviceRepo.findBySerialNumber(serial)
                    .or(() -> deviceRepo.findByMqttTopic(topic))
                    .orElse(null);

            if (device == null) {
                log.warn("Dispositif inconnu — serial={} topic={}", serial, topic);
                return;
            }

            if (!device.isActive()) {
                log.warn("Dispositif inactif ignoré — serial={}", serial);
                return;
            }

            // Validation physiologique rapide avant Kafka
            DeviceType dt = device.getDeviceType();
            if (!dt.isPhysiologicallyValid(rawValue)) {
                log.error("Mesure hors plage physiologique rejetée — type={} value={}", dt, rawValue);
                return;
            }

            long latencyMs = received.toEpochMilli() - tsMillis;

            IoTObservationEvent event = IoTObservationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .patientId(device.getPatient().getId())
                    .deviceId(device.getId())
                    .serialNumber(serial)
                    .deviceType(dt)
                    .loincCode(dt.getLoincCode())
                    .value(BigDecimal.valueOf(rawValue))
                    .unit(unit.isBlank() ? dt.getUnit() : unit)
                    .mqttTopic(topic)
                    .observedAt(observedAt)
                    .receivedAt(received)
                    .latencyMs(latencyMs)
                    .rawPayload(payload)
                    .build();

            kafkaProducer.publishObservation(event);
            log.info("IoT → Kafka — patient={} type={} value={}{}  [{}ms]",
                    device.getPatient().getId(), dt, rawValue, unit, latencyMs);

        } catch (Exception ex) {
            log.error("Erreur traitement message MQTT — topic={} : {}", topic, ex.getMessage(), ex);
        }
    }
}
