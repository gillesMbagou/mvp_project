package be.caresync.demo.repository;

import be.caresync.demo.model.db.patient.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    List<Allergy> findByPatientIdAndActiveTrue(String patientId);
    List<Allergy> findByPatientId(String patientId);
}
