package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "vaccinations", indexes = {
    @Index(name = "idx_vaccination_patient", columnList = "patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName;

    private String manufacturer;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "administration_date")
    private LocalDate administrationDate;

    @Column(name = "next_dose_date")
    private LocalDate nextDoseDate;

    @Column(name = "practitioner_email")
    private String practitionerEmail;

    private String site;

    @Column(name = "vaccine_code")
    private String vaccineCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;
}
