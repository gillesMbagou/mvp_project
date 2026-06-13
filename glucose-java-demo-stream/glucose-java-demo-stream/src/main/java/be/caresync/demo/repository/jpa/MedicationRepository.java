package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.patient.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByPatientId(String patientId);
    List<Medication> findByPatientIdAndOngoingTrue(String patientId);
}
