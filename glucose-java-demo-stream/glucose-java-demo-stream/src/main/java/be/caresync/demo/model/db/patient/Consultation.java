package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "consultations", indexes = {
    @Index(name = "idx_consult_patient", columnList = "patient_id, consultation_date DESC")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationType type;

    @NotNull
    @Column(name = "consultation_date", nullable = false)
    private LocalDate consultationDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "practitioner_email")
    private String practitionerEmail;

    @Column(name = "practitioner_name")
    private String practitionerName;

    private String specialty;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    private String diagnosis;

    @Column(name = "icd10_code")
    private String icd10Code;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ccam_code")
    private String ccamCode;

    @Column(name = "facility_name")
    private String facilityName;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum ConsultationType {
        CONSULTATION, HOSPITALISATION, URGENCES, TELECONSULTATION
    }
}
