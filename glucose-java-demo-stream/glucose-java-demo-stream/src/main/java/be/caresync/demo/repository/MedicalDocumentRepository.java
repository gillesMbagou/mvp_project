package be.caresync.demo.repository;

import be.caresync.demo.model.db.patient.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    List<MedicalDocument> findByPatientIdOrderByUploadedAtDesc(String patientId);

    @Query("SELECT m FROM MedicalDocument m WHERE m.patientId = :patientId AND m.documentType = :documentType")
    List<MedicalDocument> findByPatientIdAndDocumentType(
            @Param("patientId") String patientId,
            @Param("documentType") MedicalDocument.DocumentType documentType
    );
}