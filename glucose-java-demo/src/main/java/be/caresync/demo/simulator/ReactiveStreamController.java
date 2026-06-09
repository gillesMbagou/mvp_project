package be.caresync.demo.simulator;

import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.streaming.ReactiveEventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Endpoints SSE (Server-Sent Events) — flux réactif WebFlux.
 *
 * Les clients (Angular, navigateur) s'abonnent via EventSource :
 *   const es = new EventSource('/api/stream/observations/patient-001');
 *   es.addEventListener('observation', e => console.log(JSON.parse(e.data)));
 *   es.addEventListener('critique',    e => triggerAlert(JSON.parse(e.data)));
 *
 * Endpoints :
 *   GET /api/stream/observations              → toutes observations temps réel
 *   GET /api/stream/observations/{patientId}  → par patient
 *   GET /api/stream/alerts                    → uniquement hors-NORMAL
 *   GET /api/stream/devices/{serial}          → par dispositif
 */
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReactiveStreamController {

    private final ReactiveEventBus eventBus;

    // Flux.interval est cold : chaque souscripteur obtient son propre timer indépendant.
    private static final Flux<ServerSentEvent<ObservationRecord>> HEARTBEAT =
            Flux.interval(Duration.ofSeconds(25))
                .map(i -> ServerSentEvent.<ObservationRecord>builder()
                        .comment("heartbeat")
                        .build());

    // Toutes les observations

    @GetMapping(value = "/observations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ObservationRecord>> streamAll() {
        return withHeartbeat(
                eventBus.asFlux()
                        .map(obs -> toSSE(obs, "observation"))
        );
    }

    // ── Par patient ───────────────────────────────────────────────────────────

    @GetMapping(value = "/observations/{patientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ObservationRecord>> streamPatient(
            @PathVariable String patientId) {
        return withHeartbeat(
                eventBus.asFlux()
                        .filter(obs -> patientId.equals(obs.getPatientId()))
                        .map(obs -> toSSE(obs, "observation"))
        );
    }

    // ── Alertes uniquement (severity != NORMAL) ───────────────────────────────

    @GetMapping(value = "/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ObservationRecord>> streamAlerts() {
        return withHeartbeat(
                eventBus.asFlux()
                        .filter(obs -> !"NORMAL".equals(obs.getSeverity()))
                        .map(obs -> toSSE(obs, obs.getSeverity().toLowerCase()))
        );
    }

    // ── Par dispositif ────────────────────────────────────────────────────────

    @GetMapping(value = "/devices/{serial}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ObservationRecord>> streamDevice(
            @PathVariable String serial) {
        return withHeartbeat(
                eventBus.asFlux()
                        .filter(obs -> serial.equals(obs.getDeviceSerial()))
                        .map(obs -> toSSE(obs, "observation"))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerSentEvent<ObservationRecord> toSSE(ObservationRecord obs, String event) {
        return ServerSentEvent.<ObservationRecord>builder()
                .id(obs.getId() != null ? obs.getId().toString() : null)
                .event(event)
                .data(obs)
                .build();
    }

    private Flux<ServerSentEvent<ObservationRecord>> withHeartbeat(
            Flux<ServerSentEvent<ObservationRecord>> data) {
        return Flux.merge(data, HEARTBEAT);
    }
}
