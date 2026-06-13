package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.patient.Contraindication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContraindicationRepository extends JpaRepository<Contraindication, Long> {
    List<Contraindication> findByPatientId(String patientId);
    List<Contraindication> findByPatientIdAndActiveTrue(String patientId);
}
