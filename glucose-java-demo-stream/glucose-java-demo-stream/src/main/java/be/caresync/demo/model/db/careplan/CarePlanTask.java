package be.caresync.demo.model.db.careplan;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "care_plan_tasks", indexes = {
        @Index(name = "idx_care_plan_tasks_plan", columnList = "care_plan_id"),
        @Index(name = "idx_care_plan_tasks_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "care_plan_id", nullable = false)
    private Long carePlanId;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskFrequency frequency = TaskFrequency.ONCE;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "compliance_rate")
    private Double complianceRate;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum TaskStatus { PENDING, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED }
    public enum TaskFrequency { ONCE, DAILY, WEEKLY, BIWEEKLY, MONTHLY }
}
