package be.caresync.demo.model.db.careplan;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "care_plans", indexes = {
        @Index(name = "idx_care_plans_patient", columnList = "patient_id"),
        @Index(name = "idx_care_plans_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private CarePlanStatus status = CarePlanStatus.ACTIVE;

    @Column(name = "protocol_id")
    private Long protocolId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "assigned_professional")
    private String assignedProfessional;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum CarePlanStatus { DRAFT, ACTIVE, SUSPENDED, COMPLETED, CANCELLED }
}
