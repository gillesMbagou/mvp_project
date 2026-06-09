package be.caresync.demo.service;

import be.caresync.demo.model.db.careplan.CarePlan;
import be.caresync.demo.model.db.careplan.CarePlanTask;
import be.caresync.demo.model.db.careplan.TherapeuticObjective;
import be.caresync.demo.repository.CarePlanRepository;
import be.caresync.demo.repository.CarePlanTaskRepository;
import be.caresync.demo.repository.TherapeuticObjectiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarePlanService {

    private final CarePlanRepository carePlanRepo;
    private final CarePlanTaskRepository taskRepo;
    private final TherapeuticObjectiveRepository objectiveRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlans(String patientId) {
        return carePlanRepo.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<CarePlan> getById(Long id) {
        return carePlanRepo.findById(id);
    }

    @Transactional
    public CarePlan create(CarePlan plan) {
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        CarePlan saved = carePlanRepo.save(plan);
        auditService.log("CREATE_CARE_PLAN", "CarePlan", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional
    public CarePlan updateStatus(Long id, CarePlan.CarePlanStatus status) {
        CarePlan plan = carePlanRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("CarePlan not found: " + id));
        plan.setStatus(status);
        plan.setUpdatedAt(Instant.now());
        return carePlanRepo.save(plan);
    }

    @Transactional
    public CarePlanTask addTask(CarePlanTask task) {
        task.setCreatedAt(Instant.now());
        CarePlanTask saved = taskRepo.save(task);
        auditService.log("ADD_CARE_PLAN_TASK", "CarePlanTask", String.valueOf(saved.getId()), saved.getPatientId(), null, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CarePlanTask> getTasks(Long carePlanId) {
        return taskRepo.findByCarePlanId(carePlanId);
    }

    @Transactional
    public CarePlanTask completeTask(Long taskId) {
        CarePlanTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Task not found: " + taskId));
        task.setStatus(CarePlanTask.TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        return taskRepo.save(task);
    }

    @Transactional
    public TherapeuticObjective addObjective(TherapeuticObjective objective) {
        objective.setCreatedAt(Instant.now());
        objective.setUpdatedAt(Instant.now());
        return objectiveRepo.save(objective);
    }

    @Transactional(readOnly = true)
    public List<TherapeuticObjective> getObjectives(String patientId) {
        return objectiveRepo.findByPatientId(patientId);
    }

    @Transactional
    public TherapeuticObjective updateObjectiveValue(Long id, Double currentValue) {
        TherapeuticObjective obj = objectiveRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Objective not found: " + id));
        obj.setCurrentValue(currentValue);
        obj.setUpdatedAt(Instant.now());
        return objectiveRepo.save(obj);
    }
}
