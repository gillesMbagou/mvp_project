package be.caresync.demo.service;

import be.caresync.demo.model.db.patient.MedicalDocument;
import be.caresync.demo.repository.MedicalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalDocumentService {

    private final MedicalDocumentRepository documentRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MedicalDocument> getDocuments(String patientId) {
        return documentRepo.findByPatientIdOrderByUploadedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<MedicalDocument> getById(Long id) {
        return documentRepo.findById(id);
    }

    @Transactional
    public MedicalDocument upload(MedicalDocument document) {
        document.setUploadedAt(Instant.now());
        document.setOcrStatus(MedicalDocument.OcrStatus.PENDING);
        MedicalDocument saved = documentRepo.save(document);
        auditService.log("UPLOAD_DOCUMENT", "MedicalDocument", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional
    public MedicalDocument updateOcrResult(Long id, String ocrText) {
        MedicalDocument doc = documentRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Document not found: " + id));
        doc.setOcrText(ocrText);
        doc.setSearchIndex(ocrText);
        doc.setOcrStatus(MedicalDocument.OcrStatus.DONE);
        return documentRepo.save(doc);
    }
}
