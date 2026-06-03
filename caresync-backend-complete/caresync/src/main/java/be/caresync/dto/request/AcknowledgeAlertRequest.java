package be.caresync.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AcknowledgeAlertRequest {
    @NotBlank
    private String comment;
}
