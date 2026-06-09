package be.caresync.demo.stream;

import be.caresync.demo.model.GlucoseObservation;
import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.repository.ObservationRepository;
import be.caresync.demo.repository.PatientRepository;
import be.caresync.demo.simulator.MultiPatientSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Endpoints SSE exposés aux clients Angular.
 *
 *   GET /api/health-professional/stream/observations  → mesures IoT temps réel
 *   GET /api/health-professional/stream/alerts        → alertes URGENTE + CRITIQUE
 *   GET /api/health-professional/stream/stats         → compteurs dashboard (push toutes les 30s)
 *   GET /api/health-professional/stream/heartbeat     → keepalive proxy
 */
@RestController
@RequestMapping("/api/health-professional/stream")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final GlucoseEventStream    eventStream;
    private final ObservationRepository obsRepo;
    private final PatientRepository     patientRepo;
    private final MultiPatientSimulator simulator;

    // ── Flux observations ─────────────────────────────────────────────────────

    @GetMapping(value = "/observations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GlucoseObservation>> streamObservations(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String patientId) {

        Flux<GlucoseObservation> source = eventStream.observations();

        if (serial != null) {
            source = source.filter(o -> serial.equals(o.getSerial()));
        }

        return source
            .mergeWith(heartbeatFlux())
            .map(obs -> ServerSentEvent.<GlucoseObservation>builder()
                    .id(String.valueOf(obs.getTs()))
                    .event("observation")
                    .data(obs)
                    .retry(Duration.ofSeconds(3))
                    .build())
            .doOnSubscribe(s -> log.info("Client SSE connecté (serial={}, patientId={})", serial, patientId))
            .doOnCancel(() -> log.info("Client SSE déconnecté (serial={})", serial));
    }

    // ── Flux alertes ──────────────────────────────────────────────────────────

    @GetMapping(value = "/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GlucoseObservation>> streamAlerts() {
        return eventStream.observations()
            .filter(obs -> "CRITIQUE".equals(obs.computeSeverity())
                        || "URGENTE".equals(obs.computeSeverity()))
            .map(obs -> ServerSentEvent.<GlucoseObservation>builder()
                    .id(String.valueOf(obs.getTs()))
                    .event("alert")
                    .data(obs)
                    .retry(Duration.ofSeconds(2))
                    .build())
            .doOnSubscribe(s -> log.info("Client SSE alertes connecté"));
    }

    // ── Flux stats (push toutes les 30s) ──────────────────────────────────────

    @GetMapping(value = "/stats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> streamStats() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(30))
            .flatMap(i -> Mono.fromCallable(this::computeStats)
                    .subscribeOn(Schedulers.boundedElastic()))
            .map(stats -> ServerSentEvent.<Map<String, Object>>builder()
                    .event("stats")
                    .data(stats)
                    .retry(Duration.ofSeconds(5))
                    .build())
            .doOnSubscribe(s -> log.info("Client SSE stats connecté"))
            .doOnCancel(() -> log.info("Client SSE stats déconnecté"));
    }

    private Map<String, Object> computeStats() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        List<ObservationRecord> alerts = obsRepo
                .findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc("NORMAL", since24h);
        return Map.of(
            "totalObservations", obsRepo.count(),
            "totalPatients",     patientRepo.count(),
            "totalDevices",      simulator.getAllDevices().size(),
            "alertsLast24h",     alerts.size(),
            "criticalAlerts24h", alerts.stream()
                    .filter(a -> "CRITIQUE".equals(a.getSeverity())).count(),
            "timestamp",         Instant.now().toEpochMilli()
        );
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    @GetMapping(value = "/heartbeat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(25))
                .map(i -> ServerSentEvent.<String>builder()
                        .comment("heartbeat " + i)
                        .build());
    }

    private Flux<GlucoseObservation> heartbeatFlux() {
        return Flux.interval(Duration.ofSeconds(25))
                .flatMap(i -> Flux.empty());
    }
}
