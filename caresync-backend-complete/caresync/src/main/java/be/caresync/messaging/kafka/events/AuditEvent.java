package be.caresync.messaging.kafka.events;// Événement 4 : traçabilité RGPD/HDS
// Publié par : TOUS les services (via AuditProducer)
// Consommé par : AuditService uniquement

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
public class AuditEvent {
    private UUID eventId;
    private UUID    patientId;        // Null si action non liée à un patient
    private String  actorEmail;       // Qui a fait l'action
    private String  action;           // "ALERT_CREATED", "OBSERVATION_STORED"...
    private String  resourceType;     // "Alert", "Observation"...
    private UUID    resourceId;
    private String  details;          // JSON libre des détails

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}