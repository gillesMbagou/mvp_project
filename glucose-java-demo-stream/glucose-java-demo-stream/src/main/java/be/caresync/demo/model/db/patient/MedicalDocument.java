package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "medical_documents", indexes = {
    @Index(name = "idx_doc_patient",     columnList = "patient_id"),
    @Index(name = "idx_doc_type",        columnList = "document_type"),
    @Index(name = "idx_doc_uploaded_at", columnList = "uploaded_at DESC")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status")
    @Builder.Default
    private OcrStatus ocrStatus = OcrStatus.PENDING;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "search_index", columnDefinition = "TEXT")
    private String searchIndex;

    public enum DocumentType {
        ORDONNANCE, COMPTE_RENDU, IMAGERIE_DICOM, BIOLOGIE,
        COURRIER, CONSENTEMENT, AUTRE
    }

    public enum OcrStatus { PENDING, PROCESSING, DONE, FAILED }
}
