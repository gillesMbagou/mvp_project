package be.caresync.messaging.kafka.events;

import be.caresync.domain.iot.DeviceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement Kafka publié sur caresync.iot.observations
 * à chaque réception d'une mesure IoT via MQTT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IoTObservationEvent {

    private UUID       eventId;
    private UUID       patientId; // Clé de partition → ordre garanti par patient
    private UUID       deviceId;
    private String     serialNumber;
    private DeviceType deviceType; // "GLUCOMETER", "PULSE_OXIMETER"...
    private String     loincCode;  // "2345-7" (glycémie capillaire)
    private BigDecimal value;
    private String     unit;
    private String     mqttTopic;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant    observedAt; // Timestamp de la mesure sur le capteur

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant    receivedAt; // Timestamp de réception MQTT

    private Long       latencyMs; // receivedAt - observedAt = latence réseau
    private String     rawPayload;
}
