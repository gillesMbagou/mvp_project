package be.caresync.demo.service;

import be.caresync.demo.model.db.patient.*;
import be.caresync.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final AllergyRepository allergyRepo;
    private final MedicalHistoryRepository historyRepo;
    private final ContraindicationRepository contraindicationRepo;
    private final MedicationRepository medicationRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Allergy> getAllergies(String patientId) {
        return allergyRepo.findByPatientIdAndActiveTrue(patientId);
    }

    @Transactional
    public Allergy addAllergy(Allergy allergy) {
        allergy.setCreatedAt(Instant.now());
        Allergy saved = allergyRepo.save(allergy);
        auditService.log("ADD_ALLERGY", "Allergy", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional
    public void removeAllergy(Long id, String patientId) {
        allergyRepo.findById(id).ifPresent(a -> {
            a.setActive(false);
            allergyRepo.save(a);
            auditService.log("REMOVE_ALLERGY", "Allergy", String.valueOf(id), patientId, null, null);
        });
    }

    @Transactional(readOnly = true)
    public List<MedicalHistory> getMedicalHistory(String patientId) {
        return historyRepo.findByPatientId(patientId);
    }

    @Transactional
    public MedicalHistory addMedicalHistory(MedicalHistory history) {
        history.setCreatedAt(Instant.now());
        MedicalHistory saved = historyRepo.save(history);
        auditService.log("ADD_HISTORY", "MedicalHistory", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Contraindication> getContraindications(String patientId) {
        return contraindicationRepo.findByPatientIdAndActiveTrue(patientId);
    }

    @Transactional
    public Contraindication addContraindication(Contraindication ci) {
        ci.setReportedAt(Instant.now());
        Contraindication saved = contraindicationRepo.save(ci);
        auditService.log("ADD_CONTRAINDICATION", "Contraindication", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Medication> getMedications(String patientId) {
        return medicationRepo.findByPatientIdAndOngoingTrue(patientId);
    }

    @Transactional
    public Medication addMedication(Medication medication) {
        Medication saved = medicationRepo.save(medication);
        auditService.log("ADD_MEDICATION", "Medication", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }
}
