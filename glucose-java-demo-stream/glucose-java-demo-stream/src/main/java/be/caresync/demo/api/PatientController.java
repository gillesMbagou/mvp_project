package be.caresync.demo.api;

import be.caresync.demo.model.db.PatientProfile;
import be.caresync.demo.model.db.patient.Allergy;
import be.caresync.demo.model.db.patient.Medication;
import be.caresync.demo.model.db.patient.MedicalHistory;
import be.caresync.demo.repository.PatientRepository;
import be.caresync.demo.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PatientController {

    private final PatientRepository       patientRepo;
    private final MedicalRecordService    medicalRecordService;

    // ── Patients ──────────────────────────────────────────────────────────────

    @GetMapping
    public Flux<PatientProfile> getAll(
            @RequestParam(required = false) String pathology) {
        return Mono.fromCallable(() ->
                pathology != null
                    ? patientRepo.findByPatient_Pathology(pathology)
                    : patientRepo.findAll())
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/{id}")
    public Mono<PatientProfile> getById(@PathVariable String id) {
        return Mono.fromCallable(() ->
                patientRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient introuvable : " + id)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    // ── Allergies ─────────────────────────────────────────────────────────────

    @GetMapping("/{patientId}/allergies")
    public Flux<Allergy> getAllergies(@PathVariable String patientId) {
        return Mono.fromCallable(() -> medicalRecordService.getAllergies(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/{patientId}/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Allergy> addAllergy(
            @PathVariable String patientId,
            @RequestBody @Valid Allergy allergy) {
        allergy.setPatientId(patientId);
        return Mono.fromCallable(() -> medicalRecordService.addAllergy(allergy))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{patientId}/allergies/{allergyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeAllergy(
            @PathVariable String patientId,
            @PathVariable Long allergyId) {
        return Mono.fromRunnable(() ->
                medicalRecordService.removeAllergy(allergyId, patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    // ── Antécédents médicaux ──────────────────────────────────────────────────

    @GetMapping("/{patientId}/history")
    public Flux<MedicalHistory> getHistory(@PathVariable String patientId) {
        return Mono.fromCallable(() -> medicalRecordService.getMedicalHistory(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/{patientId}/history")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MedicalHistory> addHistory(
            @PathVariable String patientId,
            @RequestBody @Valid MedicalHistory history) {
        history.setPatientId(patientId);
        return Mono.fromCallable(() -> medicalRecordService.addMedicalHistory(history))
            .subscribeOn(Schedulers.boundedElastic());
    }

    // ── Médicaments en cours ──────────────────────────────────────────────────

    @GetMapping("/{patientId}/medications")
    public Flux<Medication> getMedications(@PathVariable String patientId) {
        return Mono.fromCallable(() -> medicalRecordService.getMedications(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/{patientId}/medications")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Medication> addMedication(
            @PathVariable String patientId,
            @RequestBody @Valid Medication medication) {
        medication.setPatientId(patientId);
        return Mono.fromCallable(() -> medicalRecordService.addMedication(medication))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
