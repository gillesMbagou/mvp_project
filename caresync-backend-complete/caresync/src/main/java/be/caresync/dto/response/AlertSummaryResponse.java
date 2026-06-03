// ── AlertSummaryResponse ──────────────────────────────────────────────────────
package be.caresync.dto.response;

import be.caresync.domain.alert.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AlertSummaryResponse {
    private UUID                  id;
    private UUID                  patientId;
    private String                patientName;
    private AlertType             type;
    private Severity              severity;
    private String                message;
    private Alert.AlertStatus     status;
    private Alert.EscalationLevel escalationLevel;
    private String                triggeredValue;
    private String                thresholdValue;
    private Instant               createdAt;
    private Instant               acknowledgedAt;
    private String                acknowledgedBy;
}
