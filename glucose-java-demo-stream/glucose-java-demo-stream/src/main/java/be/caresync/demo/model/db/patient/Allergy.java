package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "allergies", indexes = {
    @Index(name = "idx_allergy_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String substance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllergySeverity severity;

    private String reaction;
    private String notes;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum AllergySeverity { MILD, MODERATE, SEVERE, ANAPHYLACTIC }
}
