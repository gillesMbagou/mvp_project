package be.caresync.demo.service;

import be.caresync.demo.model.db.patient.Consultation;
import be.caresync.demo.repository.jpa.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Consultation> getConsultations(String patientId) {
        return consultationRepo.findByPatientIdOrderByConsultationDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<Consultation> getById(Long id) {
        return consultationRepo.findById(id);
    }

    @Transactional
    public Consultation create(Consultation consultation) {
        consultation.setCreatedAt(Instant.now());
        Consultation saved = consultationRepo.save(consultation);
        auditService.log("CREATE_CONSULTATION", "Consultation", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional
    public Consultation update(Long id, Consultation updated) {
        Consultation existing = consultationRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Consultation not found: " + id));
        existing.setDiagnosis(updated.getDiagnosis());
        existing.setIcd10Code(updated.getIcd10Code());
        existing.setCcamCode(updated.getCcamCode());
        existing.setTreatmentPlan(updated.getTreatmentPlan());
        existing.setDischargeDate(updated.getDischargeDate());
        return consultationRepo.save(existing);
    }
}
