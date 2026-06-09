package be.caresync.demo.api;

import be.caresync.demo.model.db.careplan.CarePlan;
import be.caresync.demo.model.db.careplan.CarePlanTask;
import be.caresync.demo.model.db.careplan.TherapeuticObjective;
import be.caresync.demo.service.CarePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/care-plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CarePlanController {

    private final CarePlanService carePlanService;

    // ── Plans de soins ────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}")
    public Flux<CarePlan> getByPatient(@PathVariable String patientId) {
        return Mono.fromCallable(() -> carePlanService.getCarePlans(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/{id}")
    public Mono<CarePlan> getById(@PathVariable Long id) {
        return Mono.fromCallable(() ->
                carePlanService.getById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan de soins introuvable : " + id)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CarePlan> create(@RequestBody @Valid CarePlan plan) {
        return Mono.fromCallable(() -> carePlanService.create(plan))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PatchMapping("/{id}/status")
    public Mono<CarePlan> updateStatus(
            @PathVariable Long id,
            @RequestParam CarePlan.CarePlanStatus status) {
        return Mono.fromCallable(() -> carePlanService.updateStatus(id, status))
            .subscribeOn(Schedulers.boundedElastic());
    }

    // ── Tâches ────────────────────────────────────────────────────────────────

    @GetMapping("/{carePlanId}/tasks")
    public Flux<CarePlanTask> getTasks(@PathVariable Long carePlanId) {
        return Mono.fromCallable(() -> carePlanService.getTasks(carePlanId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/{carePlanId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CarePlanTask> addTask(
            @PathVariable Long carePlanId,
            @RequestBody @Valid CarePlanTask task) {
        task.setCarePlanId(carePlanId);
        return Mono.fromCallable(() -> carePlanService.addTask(task))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PatchMapping("/tasks/{taskId}/complete")
    public Mono<CarePlanTask> completeTask(@PathVariable Long taskId) {
        return Mono.fromCallable(() -> carePlanService.completeTask(taskId))
            .subscribeOn(Schedulers.boundedElastic());
    }

    // ── Objectifs thérapeutiques ──────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/objectives")
    public Flux<TherapeuticObjective> getObjectives(@PathVariable String patientId) {
        return Mono.fromCallable(() -> carePlanService.getObjectives(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/objectives")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TherapeuticObjective> addObjective(
            @RequestBody @Valid TherapeuticObjective objective) {
        return Mono.fromCallable(() -> carePlanService.addObjective(objective))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PatchMapping("/objectives/{id}/value")
    public Mono<TherapeuticObjective> updateObjectiveValue(
            @PathVariable Long id,
            @RequestParam Double currentValue) {
        return Mono.fromCallable(() -> carePlanService.updateObjectiveValue(id, currentValue))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
