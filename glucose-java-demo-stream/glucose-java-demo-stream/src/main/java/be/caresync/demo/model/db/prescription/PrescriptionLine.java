package be.caresync.demo.model.db.prescription;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "prescription_lines", indexes = {
        @Index(name = "idx_presc_lines_prescription", columnList = "prescription_id"),
        @Index(name = "idx_presc_lines_atc", columnList = "atc_code")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @NotBlank
    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "atc_code")
    private String atcCode;

    private String dosage;
    private String unit;
    private String frequency;
    private String route;

    @Column(name = "duration_days")
    private int durationDays;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "has_interaction_warning")
    @Builder.Default
    private boolean hasInteractionWarning = false;

    @Column(name = "interaction_details", columnDefinition = "TEXT")
    private String interactionDetails;
}
