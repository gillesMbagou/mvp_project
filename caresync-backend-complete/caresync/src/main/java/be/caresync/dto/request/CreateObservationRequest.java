package be.caresync.dto.request;

import be.caresync.domain.iot.DeviceType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// ── Observation manuelle (saisie médecin/infirmier) ───────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateObservationRequest {

    @NotNull
    private UUID        patientId;

    @NotNull
    private DeviceType  deviceType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal  value;

    @NotBlank
    private String      unit;

    private Instant     observedAt;   // null = now
    private String      note;
}
