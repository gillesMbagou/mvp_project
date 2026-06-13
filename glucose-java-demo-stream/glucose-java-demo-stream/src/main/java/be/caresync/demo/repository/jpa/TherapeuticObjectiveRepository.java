package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.careplan.TherapeuticObjective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TherapeuticObjectiveRepository extends JpaRepository<TherapeuticObjective, Long> {
    List<TherapeuticObjective> findByPatientId(String patientId);
    List<TherapeuticObjective> findByCarePlanId(Long carePlanId);
}
