package be.caresync.demo.service;

import be.caresync.demo.model.db.rgpd.PatientConsent;
import be.caresync.demo.repository.jpa.PatientConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final PatientConsentRepository consentRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PatientConsent> getConsents(String patientId) {
        return consentRepo.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<PatientConsent> getConsent(String patientId, PatientConsent.ConsentType type) {
        return consentRepo.findByPatientIdAndConsentType(patientId, type);
    }

    @Transactional
    public PatientConsent grantConsent(PatientConsent consent) {
        consent.setGranted(true);
        consent.setGrantedAt(Instant.now());
        consent.setRevokedAt(null);
        consent.setCreatedAt(Instant.now());
        PatientConsent saved = consentRepo.save(consent);
        auditService.log("GRANT_CONSENT", "PatientConsent",
                consent.getConsentType().name(), consent.getPatientId(), null, null);
        return saved;
    }

    @Transactional
    public void revokeConsent(String patientId, PatientConsent.ConsentType type) {
        consentRepo.findByPatientIdAndConsentType(patientId, type).ifPresent(c -> {
            c.setGranted(false);
            c.setRevokedAt(Instant.now());
            consentRepo.save(c);
            auditService.log("REVOKE_CONSENT", "PatientConsent", type.name(), patientId, null, null);
        });
    }
}
