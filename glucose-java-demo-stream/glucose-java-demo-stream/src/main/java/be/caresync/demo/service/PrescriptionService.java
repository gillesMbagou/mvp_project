package be.caresync.demo.service;

import be.caresync.demo.model.db.prescription.*;
import be.caresync.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepo;
    private final PrescriptionLineRepository lineRepo;
    private final DrugInteractionService interactionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Prescription> getByPatient(String patientId) {
        return prescriptionRepo.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<Prescription> getById(Long id) {
        return prescriptionRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionLine> getLines(Long prescriptionId) {
        return lineRepo.findByPrescriptionId(prescriptionId);
    }

    @Transactional
    public Prescription create(Prescription prescription, List<PrescriptionLine> lines) {
        prescription.setCreatedAt(Instant.now());
        Prescription saved = prescriptionRepo.save(prescription);

        List<PrescriptionLine> savedLines = lines.stream().map(line -> {
            line.setPrescriptionId(saved.getId());
            return lineRepo.save(line);
        }).toList();

        interactionService.checkAndFlagInteractions(savedLines);

        auditService.log("CREATE_PRESCRIPTION", "Prescription", String.valueOf(saved.getId()),
                saved.getPatientId(), saved.getPrescribingDoctorName(), null);
        return saved;
    }

    @Transactional
    public Prescription renew(Long id) {
        Prescription original = prescriptionRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Prescription not found: " + id));
        if (!original.isRenewable() || original.getRenewalCount() >= original.getMaxRenewals()) {
            throw new IllegalStateException("Prescription cannot be renewed");
        }
        original.setRenewalCount(original.getRenewalCount() + 1);
        original.setLastRenewalDate(LocalDate.now());
        original.setStatus(Prescription.PrescriptionStatus.RENEWED);
        auditService.log("RENEW_PRESCRIPTION", "Prescription", String.valueOf(id), original.getPatientId(), null, null);
        return prescriptionRepo.save(original);
    }

    @Transactional
    public Prescription cancel(Long id) {
        Prescription prescription = prescriptionRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Prescription not found: " + id));
        prescription.setStatus(Prescription.PrescriptionStatus.CANCELLED);
        auditService.log("CANCEL_PRESCRIPTION", "Prescription", String.valueOf(id), prescription.getPatientId(), null, null);
        return prescriptionRepo.save(prescription);
    }
}
