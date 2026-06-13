package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.prescription.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<Prescription> findByPatientIdAndStatus(String patientId, Prescription.PrescriptionStatus status);
}
