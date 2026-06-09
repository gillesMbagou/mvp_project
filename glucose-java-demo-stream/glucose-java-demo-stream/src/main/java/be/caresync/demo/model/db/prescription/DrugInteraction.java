package be.caresync.demo.model.db.prescription;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "drug_interactions", indexes = {
        @Index(name = "idx_drug_interactions_atc_a", columnList = "atc_code_a"),
        @Index(name = "idx_drug_interactions_atc_b", columnList = "atc_code_b")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "atc_code_a", nullable = false)
    private String atcCodeA;

    @NotBlank
    @Column(name = "drug_name_a", nullable = false)
    private String drugNameA;

    @NotBlank
    @Column(name = "atc_code_b", nullable = false)
    private String atcCodeB;

    @NotBlank
    @Column(name = "drug_name_b", nullable = false)
    private String drugNameB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionSeverity severity;

    @Column(columnDefinition = "TEXT")
    private String mechanism;

    @Column(columnDefinition = "TEXT")
    private String clinicalConsequence;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "theriaqueRef")
    private String theriaqueRef;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum InteractionSeverity { MINOR, MODERATE, MAJOR, CONTRAINDICATED }
}
