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
    private UUID       patientId;
    private UUID       deviceId;
    private String     serialNumber;
    private DeviceType deviceType;
    private String     loincCode;
    private BigDecimal value;
    private String     unit;
    private String     mqttTopic;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant    observedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant    receivedAt;

    private Long       latencyMs;
    private String     rawPayload;
}
