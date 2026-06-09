package be.caresync.demo.repository;

import be.caresync.demo.model.db.rgpd.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {
    List<PatientConsent> findByPatientId(String patientId);
    Optional<PatientConsent> findByPatientIdAndConsentType(String patientId, PatientConsent.ConsentType consentType);
    List<PatientConsent> findByPatientIdAndGrantedTrue(String patientId);
}
