package be.caresync.demo.simulator;

import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.model.db.PatientProfile;
import be.caresync.demo.model.db.SimulatedDevice;
import be.caresync.demo.repository.ObservationRepository;
import be.caresync.demo.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST réactif — WebFlux.
 *
 * Pattern : Mono.fromCallable(() -> jpaCall()).subscribeOn(boundedElastic())
 * → JPA bloquant isolé sur un thread pool dédié, jamais sur le thread Netty.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final PatientRepository     patientRepo;
    private final ObservationRepository obsRepo;
    private final MultiPatientSimulator simulator;

    @GetMapping("/patients")
    public Flux<PatientProfile> getPatients() {
        return Mono.fromCallable(patientRepo::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/patients/{patientId}/observations")
    public Mono<Page<ObservationRecord>> getObservations(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return Mono.fromCallable(() ->
                obsRepo.findByPatientIdOrderByObservedAtDesc(
                        patientId, PageRequest.of(page, size)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/patients/{patientId}/observations/timeseries")
    public Flux<ObservationRecord> getTimeSeries(
            @PathVariable String patientId,
            @RequestParam String deviceType,
            @RequestParam(defaultValue = "24") int hours) {
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        return Mono.fromCallable(() ->
                obsRepo.findByPatientIdAndDeviceTypeAndObservedAtBetweenOrderByObservedAt(
                        patientId, deviceType, from, Instant.now()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/observations/alerts")
    public Flux<ObservationRecord> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return Mono.fromCallable(() ->
                obsRepo.findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
                        "NORMAL", since))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return Mono.fromCallable(() -> {
            Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
            List<ObservationRecord> alerts = obsRepo
                    .findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
                            "NORMAL", since24h);
            return Map.<String, Object>of(
                "totalObservations", obsRepo.count(),
                "totalPatients",     patientRepo.count(),
                "totalDevices",      simulator.getAllDevices().size(),
                "alertsLast24h",     alerts.size(),
                "criticalAlerts24h", alerts.stream()
                        .filter(a -> "CRITIQUE".equals(a.getSeverity())).count()
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/devices")
    public Flux<SimulatedDevice> getDevices() {
        return Mono.fromCallable(simulator::getAllDevices)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @PostMapping("/devices/{serial}/scenario/{scenario}")
    public Mono<Map<String, String>> setScenario(
            @PathVariable String serial,
            @PathVariable String scenario) {
        return Mono.fromCallable(() -> {
            simulator.setScenario(serial, scenario.toUpperCase());
            return Map.of(
                "device",   serial,
                "scenario", scenario.toUpperCase(),
                "status",   "activé"
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
