package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "biology_results", indexes = {
    @Index(name = "idx_bio_patient_date", columnList = "patient_id, sampling_date DESC"),
    @Index(name = "idx_bio_loinc",        columnList = "loinc_code")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BiologyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(name = "loinc_code", nullable = false)
    private String loincCode;

    @NotBlank
    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "\"value\"")
    private Double value;
    private String unit;

    @Column(name = "reference_min")
    private Double referenceMin;

    @Column(name = "reference_max")
    private Double referenceMax;

    @Enumerated(EnumType.STRING)
    private ResultInterpretation interpretation;

    @NotNull
    @Column(name = "sampling_date", nullable = false)
    private LocalDate samplingDate;

    private String laboratory;

    @Column(name = "practitioner_email")
    private String practitionerEmail;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum ResultInterpretation {
        NORMAL, LOW, HIGH, CRITICAL_LOW, CRITICAL_HIGH
    }
}
