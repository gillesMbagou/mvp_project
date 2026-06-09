package be.caresync.service.iot;

import be.caresync.domain.iot.IoTDevice;
import be.caresync.domain.iot.IoTDeviceRepository;
import be.caresync.domain.iot.Observation;
import be.caresync.domain.iot.ObservationRepository;
import be.caresync.domain.patient.Patient;
import be.caresync.domain.patient.PatientRepository;
import be.caresync.exception.DeviceNotFoundException;
import be.caresync.exception.PatientNotFoundException;
import be.caresync.messaging.kafka.events.IoTObservationEvent;
import be.caresync.service.alert.AlertEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consomme les événements IoT depuis Kafka,
 * persiste les observations et déclenche le moteur d'alertes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IoTIngestionService {

    private final ObservationRepository observationRepo;
    private final IoTDeviceRepository   deviceRepo;
    private final PatientRepository     patientRepo;
    private final AlertEngineService    alertEngine;

    @KafkaListener(
        topics           = "caresync.iot.observations",
        groupId          = "caresync-ingestion",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, IoTObservationEvent> record) {
        IoTObservationEvent event = record.value();
        Instant start = Instant.now();

        try {
            // Le record contient des métadonnées Kafka utiles pour le débogage
            log.debug("Observation reçue — partition={} offset={} lag={}",
                    record.partition(),
                    record.offset(),
                    // Le lag = messages en attente = indicateur de backpressure
                    start.toEpochMilli() - record.timestamp()
            );
            log.debug("Ingestion observation — patient={} type={} value={}",
                    event.getPatientId(), event.getDeviceType(), event.getValue());

            Patient   patient = patientRepo.findById(event.getPatientId())
                    .orElseThrow(() -> new PatientNotFoundException(event.getPatientId()));

            IoTDevice device  = event.getDeviceId() != null
                    ? deviceRepo.findById(event.getDeviceId())
                              .orElse(null)
                    : null;

            // Mise à jour lastSeenAt du dispositif
            if (device != null) {
                device.setLastSeenAt(Instant.now());
                deviceRepo.save(device);
            }

            Observation obs = Observation.builder()
                    .patient(patient)
                    .device(device)
                    .loincCode(event.getLoincCode())
                    .deviceType(event.getDeviceType())
                    .value(event.getValue())
                    .unit(event.getUnit())
                    .observedAt(event.getObservedAt() != null ? event.getObservedAt() : Instant.now())
                    .source(Observation.ObservationSource.IOT)
                    .status(Observation.ObservationStatus.FINAL)
                    .mqttTopic(event.getMqttTopic())
                    .latencyMs(event.getLatencyMs())
                    .build();

            obs = observationRepo.save(obs);

            long ingestionMs = Instant.now().toEpochMilli() - start.toEpochMilli();
            log.info("Observation persistée — id={} type={} value={}{} [{}ms ingestion]",
                    obs.getId(), obs.getDeviceType(), obs.getValue(), obs.getUnit(), ingestionMs);

            // Évaluation des seuils d'alerte en asynchrone
            alertEngine.evaluate(obs);

        } catch (PatientNotFoundException ex) {
            log.error("Patient introuvable pour l'observation — patientId={}",
                    event.getPatientId());
        } catch (Exception ex) {
            log.error("Erreur ingestion observation Kafka : {}", ex.getMessage(), ex);
            throw ex; // Re-throw pour que Kafka retente
        }
    }
}
