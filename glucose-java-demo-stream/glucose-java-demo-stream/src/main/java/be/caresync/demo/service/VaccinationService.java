package be.caresync.demo.service;

import be.caresync.demo.model.db.patient.Vaccination;
import be.caresync.demo.repository.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VaccinationService {

    private final VaccinationRepository vaccinationRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Vaccination> getVaccinations(String patientId) {
        return vaccinationRepo.findByPatientIdOrderByAdministrationDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<Vaccination> getById(Long id) {
        return vaccinationRepo.findById(id);
    }

    @Transactional
    public Vaccination record(Vaccination vaccination) {
        Vaccination saved = vaccinationRepo.save(vaccination);
        auditService.log("RECORD_VACCINATION", "Vaccination", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }
}
