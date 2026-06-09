package be.caresync.messaging.kafka.events;// Événement 3 : une escalade a été déclenchée
// Publié par : AlertEscalationService
// Consommé par : NotificationService

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEscalationEvent {
    private UUID alertId;
    private UUID    patientId;
    private String  escalationLevel;   // "NURSE", "DOCTOR", "ON_CALL"
    private String  targetEmail;
    private String  message;
    private String  severity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant escalatedAt;
}