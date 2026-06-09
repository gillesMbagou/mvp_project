package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medical_histories", indexes = {
    @Index(name = "idx_history_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoryType type;

    @NotBlank
    @Column(nullable = false)
    private String condition;

    @Column(name = "icd10_code")
    private String icd10Code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Builder.Default
    private boolean ongoing = true;

    @Column(name = "treating_physician")
    private String treatingPhysician;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum HistoryType {
        MEDICAL, SURGICAL, FAMILIAL, PSYCHIATRIC, GYNAECOLOGICAL, OBSTETRICAL
    }
}
