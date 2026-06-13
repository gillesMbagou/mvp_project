package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.careplan.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {
    List<CarePlan> findByPatientId(String patientId);
    List<CarePlan> findByPatientIdAndStatus(String patientId, CarePlan.CarePlanStatus status);
}
