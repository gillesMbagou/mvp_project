package be.caresync.demo.api;

import be.caresync.demo.model.db.prescription.Prescription;
import be.caresync.demo.model.db.prescription.PrescriptionLine;
import be.caresync.demo.service.PrescriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // ── Ordonnances ───────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}")
    public Flux<Prescription> getByPatient(@PathVariable String patientId) {
        return Mono.fromCallable(() -> prescriptionService.getByPatient(patientId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/{id}")
    public Mono<Prescription> getById(@PathVariable Long id) {
        return Mono.fromCallable(() ->
                prescriptionService.getById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ordonnance introuvable : " + id)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}/lines")
    public Flux<PrescriptionLine> getLines(@PathVariable Long id) {
        return Mono.fromCallable(() -> prescriptionService.getLines(id))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Prescription> create(@RequestBody @Valid PrescriptionRequest request) {
        return Mono.fromCallable(() ->
                prescriptionService.create(request.prescription(), request.lines()))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/renew")
    public Mono<Prescription> renew(@PathVariable Long id) {
        return Mono.fromCallable(() -> prescriptionService.renew(id))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/cancel")
    public Mono<Prescription> cancel(@PathVariable Long id) {
        return Mono.fromCallable(() -> prescriptionService.cancel(id))
            .subscribeOn(Schedulers.boundedElastic());
    }

    // ── DTO de création ───────────────────────────────────────────────────────

    record PrescriptionRequest(
        @NotNull Prescription prescription,
        @NotNull List<PrescriptionLine> lines
    ) {}
}
