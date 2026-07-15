package be.caresync.analytics.processor;

import be.caresync.analytics.entity.Observation;
import be.caresync.common.events.IoTObservationEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Persiste chaque observation IoT dans la base "caresync_analytics" (table
 * "observations"). Sans ce consumer la table restait vide : AnalyticsResource
 * exécutait des requêtes time_bucket() sur un jeu de données inexistant.
 */
@ApplicationScoped
@Slf4j
public class ObservationIngestConsumer {

    @Incoming("iot-observations")
    @Blocking
    @Transactional
    public void consume(IoTObservationEvent event) {
        Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(event.getPatientId())
                .deviceType(event.getDeviceType())
                .loincCode(event.getLoincCode())
                .value(event.getValue())
                .unit(event.getUnit())
                .severity(event.getSeverity())
                .observedAt(Instant.ofEpochMilli(event.getTs()))
                .build()
                .persist();
    }
}
