package be.caresync.demo.repository;

import be.caresync.demo.model.db.patient.ClinicalContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalContextRepository extends JpaRepository<ClinicalContext, String> {
    // patientId est la PK — findById(patientId) suffit pour la lookup 1:1
}
