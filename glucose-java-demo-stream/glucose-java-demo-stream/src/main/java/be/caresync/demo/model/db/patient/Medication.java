package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medications", indexes = {
    @Index(name = "idx_medication_patient", columnList = "patient_id"),
    @Index(name = "idx_medication_atc",     columnList = "atc_code")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "atc_code")
    private String atcCode;

    private String dosage;
    private String unit;
    private String frequency;
    private String route;

    @Column(name = "prescribed_at")
    private LocalDate prescribedAt;

    @Column(name = "prescribed_by")
    private String prescribedBy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    private boolean ongoing = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
