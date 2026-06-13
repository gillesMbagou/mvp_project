package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.patient.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatientId(String patientId);
    List<MedicalHistory> findByPatientIdAndOngoingTrue(String patientId);
}
