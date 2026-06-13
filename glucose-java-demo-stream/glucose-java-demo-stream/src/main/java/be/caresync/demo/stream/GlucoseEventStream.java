package be.caresync.demo.stream;

import be.caresync.demo.model.GlucoseObservation;
import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.model.db.SimulatedDevice;
import be.caresync.demo.repository.jpa.DeviceRepository;
import be.caresync.demo.repository.jpa.ObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline Kafka → SSE.
 *
 * Spring Kafka (@KafkaListener) reçoit les messages du topic glucose.raw
 * et les pousse dans un Sinks.Many multicast → tous les clients SSE connectés
 * reçoivent chaque observation en temps réel.
 *
 * Avantage par rapport à reactor-kafka : Spring Kafka gère la compatibilité
 * avec les versions de kafka-clients (ici 4.x), sans dépendance tierce fragile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GlucoseEventStream {

    private final ObservationRepository obsRepo;
    private final DeviceRepository deviceRepo;
    private final ConcurrentHashMap<String, String> serialToPatientCache = new ConcurrentHashMap<>();

    /**
     * Hub central : multicast avec buffer de 256 événements par abonné lent.
     * Buffer plutôt que drop pour ne pas perdre de mesures médicales.
     */
    private final Sinks.Many<GlucoseObservation> sink =
            Sinks.many().multicast().onBackpressureBuffer(256);

    public Flux<GlucoseObservation> observations() {
        return sink.asFlux();
    }

    @KafkaListener(topics = "glucose.raw")
    public void onKafkaMessage(GlucoseObservation obs) {
        // Persistance JPA (bloquante) isolée sur le pool boundedElastic
        Schedulers.boundedElastic().schedule(() -> persistObservation(obs));

        Sinks.EmitResult result = sink.tryEmitNext(obs);
        if (result.isFailure()) {
            log.warn("Sink emit failure ({}), probablement aucun client SSE connecté", result);
        }

        log.debug("→ SSE broadcast {} {} {} [{}]",
                obs.getSerial(), obs.getValue(), obs.getUnit(), obs.getSeverity());
    }

    private void persistObservation(GlucoseObservation obs) {
        try {
            String patientId = serialToPatientCache.computeIfAbsent(obs.getSerial(), s ->
                    deviceRepo.findById(s)
                            .map(SimulatedDevice::getPatientId)
                            .orElse(s)
            );

            obsRepo.save(ObservationRecord.builder()
                    .patientId(patientId)
                    .deviceSerial(obs.getSerial())
                    .deviceType("GLUCOMETER")
                    .loincCode(obs.getLoincCode())
                    .value(obs.getValue())
                    .unit(obs.getUnit())
                    .severity(obs.computeSeverity())
                    .source(obs.getSource())
                    .observedAt(obs.getObservedAt() != null
                            ? obs.getObservedAt() : Instant.now())
                    .build());
        } catch (Exception ex) {
            log.error("Erreur persistance observation : {}", ex.getMessage());
        }
    }
}
