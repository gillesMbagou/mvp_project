package be.caresync.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Événement d'alerte clinique publié sur Kafka par l'AlertProcessor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCreatedEvent {

    private String alertId;
    private String patientId;
    private String deviceSerial;
    private String deviceType;
    private String loincCode;
    private String severity;
    private String message;
    private String triggeredValue;
    private String thresholdType;
    private Instant createdAt;
}
