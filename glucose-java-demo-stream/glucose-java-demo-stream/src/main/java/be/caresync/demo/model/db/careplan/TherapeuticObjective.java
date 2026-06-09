package be.caresync.demo.model.db.careplan;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "therapeutic_objectives", indexes = {
        @Index(name = "idx_therapeutic_obj_plan", columnList = "care_plan_id"),
        @Index(name = "idx_therapeutic_obj_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TherapeuticObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_plan_id")
    private Long carePlanId;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ObjectiveType type;

    @Column(name = "target_value")
    private Double targetValue;

    @Column(name = "current_value")
    private Double currentValue;

    private String unit;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ObjectiveStatus status = ObjectiveStatus.IN_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ObjectiveType { HBA1C, BLOOD_PRESSURE, BMI, WEIGHT, LDL_CHOLESTEROL, FASTING_GLUCOSE, EXERCISE, MEDICATION_ADHERENCE, OTHER }
    public enum ObjectiveStatus { IN_PROGRESS, ACHIEVED, PARTIALLY_ACHIEVED, NOT_ACHIEVED, ABANDONED }
}
