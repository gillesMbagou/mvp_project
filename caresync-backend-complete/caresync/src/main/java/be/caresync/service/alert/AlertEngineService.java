package be.caresync.service.alert;

import be.caresync.domain.alert.*;
import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.Observation;
import be.caresync.domain.iot.ObservationRepository;
import be.caresync.messaging.kafka.events.AlertCreatedEvent;
import be.caresync.messaging.kafka.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Moteur d'alertes cliniques.
 *
 * Évalue chaque observation IoT reçue contre les seuils configurés
 * du patient, puis crée et publie les alertes appropriées.
 *
 * Pipeline : Observation → Threshold eval → Alert creation → Kafka publish
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEngineService {

    private final AlertRepository          alertRepo;
    private final AlertThresholdRepository thresholdRepo;
    private final ObservationRepository    observationRepo;
    private final KafkaProducerService     kafkaProducer;

    // ── Point d'entrée principal ───────────────────────────────────────────────

    @Transactional
    public void evaluate(Observation obs) {

        UUID       patientId  = obs.getPatient().getId();
        DeviceType deviceType = obs.getDeviceType();

        if (deviceType == null) {
            log.warn("Observation sans deviceType — skip alerting, id={}", obs.getId());
            return;
        }

        // 1. Récupérer le seuil configuré pour ce patient + type de dispositif
        Optional<AlertThreshold> thresholdOpt =
                thresholdRepo.findByPatientIdAndDeviceTypeAndActiveTrue(patientId, deviceType);

        if (thresholdOpt.isEmpty()) {
            log.debug("Aucun seuil configuré — patient={} type={}", patientId, deviceType);
            return;
        }

        AlertThreshold threshold = thresholdOpt.get();

        // 2. Règle delta 48h (ICFe) — évaluée indépendamment de la valeur instantanée
        if (deviceType == DeviceType.SCALE && threshold.getDeltaThreshold48h() != null) {
            evaluateWeightDelta(obs, threshold);
        }

        // 3. Évaluation de la valeur courante
        Severity severity = threshold.evaluate(obs.getValue());
        if (severity == Severity.NONE) {
            log.debug("Valeur dans les normes — patient={} type={} value={}",
                    patientId, deviceType, obs.getValue());
            return;
        }

        // 4. Vérification dédoublonnage : alerte identique déjà active ?
        long activeCount = alertRepo.countByPatientIdAndStatusAndCreatedAtAfter(
                patientId, Alert.AlertStatus.ACTIVE,
                Instant.now().minus(30, ChronoUnit.MINUTES));

        if (activeCount > 0 && severity != Severity.CRITIQUE) {
            log.debug("Alerte déjà active — skip dédoublonnage pour patient={}", patientId);
            return;
        }

        // 5. Évaluation spécifique par type (règles métier enrichies)
        AlertType alertType = determineAlertType(deviceType, obs, threshold);

        // 6. Création de l'alerte
        Alert alert = Alert.builder()
                .patient(obs.getPatient())
                .triggeringObservation(obs)
                .type(alertType)
                .severity(severity)
                .message(buildMessage(deviceType, obs.getValue(), obs.getUnit(), threshold, severity))
                .triggeredValue(obs.getValue().toPlainString() + " " + obs.getUnit())
                .thresholdValue(formatThreshold(threshold, severity))
                .firstNotifiedAt(Instant.now())
                .build();

        alert = alertRepo.save(alert);
        log.warn("[{}] Alerte créée — patient={} type={} value={}{} — alertId={}",
                severity, patientId, deviceType, obs.getValue(), obs.getUnit(), alert.getId());

        // 7. Publication Kafka pour notifications
        kafkaProducer.publishAlert(AlertCreatedEvent.builder()
                .alertId(alert.getId())
                .patientId(patientId)
                .patientName(obs.getPatient().getFirstName() + " " + obs.getPatient().getLastName())
                .type(alertType)
                .severity(severity)
                .message(alert.getMessage())
                .triggeredValue(alert.getTriggeredValue())
                .thresholdValue(alert.getThresholdValue())
                .createdAt(alert.getCreatedAt())
                .build());
    }

    // ── Règle spéciale : variation poids 48h (ICFe) ───────────────────────────

    private void evaluateWeightDelta(Observation current, AlertThreshold threshold) {
        Instant since48h = Instant.now().minus(48, ChronoUnit.HOURS);
        List<Observation> recent = observationRepo
                .findByPatientIdAndObservedAtAfterOrderByObservedAtDesc(
                        current.getPatient().getId(), since48h);

        if (recent.size() < 2) return;

        BigDecimal oldest = recent.get(recent.size() - 1).getValue();
        BigDecimal delta  = current.getValue().subtract(oldest);

        if (delta.compareTo(threshold.getDeltaThreshold48h()) > 0) {
            Alert alert = Alert.builder()
                    .patient(current.getPatient())
                    .triggeringObservation(current)
                    .type(AlertType.IOT)
                    .severity(Severity.URGENTE)
                    .message("Prise de poids +%.1f kg en 48h — rétention hydrique possible (ICFe)"
                            .formatted(delta.doubleValue()))
                    .triggeredValue("Δ+" + delta.toPlainString() + " kg/48h")
                    .thresholdValue("Seuil: +" + threshold.getDeltaThreshold48h() + " kg")
                    .firstNotifiedAt(Instant.now())
                    .build();

            alertRepo.save(alert);
            log.warn("[URGENTE] Alerte rétention hydrique — patient={} Δpoids=+{}kg",
                    current.getPatient().getId(), delta);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AlertType determineAlertType(DeviceType dt, Observation obs, AlertThreshold t) {
        return switch (dt) {
            case GLUCOMETER, BLOOD_PRESSURE,
                 HEART_RATE, TEMPERATURE       -> AlertType.BIOLOGIQUE;
            case PULSE_OXIMETER, SPO2, SCALE,
                 ECG                           -> AlertType.IOT;
        };
    }

    private String buildMessage(DeviceType dt, BigDecimal value,
                                String unit, AlertThreshold t, Severity sev) {
        return switch (sev) {
            case CRITIQUE -> "🔴 %s critique : %s %s".formatted(dt.getLabel(), value.toPlainString(), unit);
            case URGENTE  -> "🟠 %s hors seuil : %s %s".formatted(dt.getLabel(), value.toPlainString(), unit);
            default       -> "%s : %s %s".formatted(dt.getLabel(), value.toPlainString(), unit);
        };
    }

    private String formatThreshold(AlertThreshold t, Severity sev) {
        if (sev == Severity.CRITIQUE) {
            if (t.getCriticalLow()  != null) return "< " + t.getCriticalLow();
            if (t.getCriticalHigh() != null) return "> " + t.getCriticalHigh();
        }
        if (t.getWarningLow()  != null) return "< " + t.getWarningLow();
        if (t.getWarningHigh() != null) return "> " + t.getWarningHigh();
        return "—";
    }
}
