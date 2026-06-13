package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.careplan.CarePlanTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarePlanTaskRepository extends JpaRepository<CarePlanTask, Long> {
    List<CarePlanTask> findByCarePlanId(Long carePlanId);
    List<CarePlanTask> findByPatientId(String patientId);
    List<CarePlanTask> findByCarePlanIdAndStatus(Long carePlanId, CarePlanTask.TaskStatus status);
}
