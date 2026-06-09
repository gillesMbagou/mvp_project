package be.caresync.demo.service;

import be.caresync.demo.model.db.patient.BiologyResult;
import be.caresync.demo.repository.BiologyResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BiologyResultService {

    private final BiologyResultRepository biologyRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BiologyResult> getResults(String patientId) {
        return biologyRepo.findByPatientIdOrderBySamplingDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<BiologyResult> getResultsByLoinc(String patientId, String loincCode) {
        return biologyRepo.findByPatientIdAndLoincCode(patientId, loincCode);
    }

    @Transactional(readOnly = true)
    public Optional<BiologyResult> getById(Long id) {
        return biologyRepo.findById(id);
    }

    @Transactional
    public BiologyResult add(BiologyResult result) {
        result.setCreatedAt(Instant.now());
        BiologyResult saved = biologyRepo.save(result);
        auditService.log("ADD_BIOLOGY_RESULT", "BiologyResult", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }
}
