package be.caresync.messaging.kafka.events;

import be.caresync.domain.alert.AlertType;
import be.caresync.domain.alert.Severity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AlertCreatedEvent {
    private UUID        alertId;
    private UUID        patientId;
    private String      patientName;
    private AlertType   type;
    private Severity    severity;
    private String      message;
    private String      triggeredValue;
    private String      thresholdValue;

    // Destinataires à notifier
    private String      nurseEmail;
    private String      doctorEmail;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant   createdAt;
}
