package be.caresync.demo.model.db.rgpd;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patient_consents", indexes = {
        @Index(name = "idx_consents_patient", columnList = "patient_id"),
        @Index(name = "idx_consents_type", columnList = "consent_type")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(nullable = false)
    @Builder.Default
    private boolean granted = false;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "collected_by")
    private String collectedBy;

    @Column(name = "collection_method")
    private String collectionMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum ConsentType {
        DATA_PROCESSING,
        SHARING_PROFESSIONALS,
        RESEARCH_ANONYMISED,
        TELEMEDICINE,
        ELECTRONIC_PRESCRIPTION,
        MARKETING_COMMUNICATIONS
    }
}
