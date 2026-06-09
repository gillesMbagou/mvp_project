package be.caresync.demo.model.db.prescription;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescriptions_patient", columnList = "patient_id"),
        @Index(name = "idx_prescriptions_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "prescribing_doctor_name")
    private String prescribingDoctorName;

    @Column(name = "prescribing_doctor_rpps")
    private String prescribingDoctorRpps;

    @Column(name = "prescription_date")
    private LocalDate prescriptionDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;

    @Builder.Default
    private boolean renewable = false;

    @Column(name = "renewal_count")
    @Builder.Default
    private int renewalCount = 0;

    @Column(name = "max_renewals")
    @Builder.Default
    private int maxRenewals = 0;

    @Column(name = "last_renewal_date")
    private LocalDate lastRenewalDate;

    @Column(name = "eidas_signature")
    private String eidasSignature;

    @Column(name = "efarma_transmitted")
    @Builder.Default
    private boolean efarmaTransmitted = false;

    @Column(name = "efarma_transmitted_at")
    private Instant efarmaTransmittedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum PrescriptionStatus { DRAFT, ACTIVE, DISPENSED, EXPIRED, CANCELLED, RENEWED }
}
