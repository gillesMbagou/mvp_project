package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.patient.BiologyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiologyResultRepository extends JpaRepository<BiologyResult, Long> {
    List<BiologyResult> findByPatientIdOrderBySamplingDateDesc(String patientId);
    List<BiologyResult> findByPatientIdAndLoincCode(String patientId, String loincCode);
}
