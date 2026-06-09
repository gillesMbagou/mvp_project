package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contraindications", indexes = {
    @Index(name = "idx_ci_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Contraindication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String substance;

    @Column(name = "atc_code")
    private String atcCode;

    private String reason;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
