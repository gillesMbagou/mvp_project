package be.caresync.service.alert;

import be.caresync.domain.alert.AlertThreshold;
import be.caresync.domain.alert.AlertThresholdRepository;
import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.patient.Patient;
import be.caresync.domain.patient.PatientRepository;
import be.caresync.dto.request.CreateThresholdRequest;
import be.caresync.dto.response.AlertThresholdResponse;
import be.caresync.exception.PatientNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertThresholdService {

    private final AlertThresholdRepository thresholdRepo;
    private final PatientRepository        patientRepo;

    // ── Créer ou remplacer un seuil ────────────────────────────────────────────

    @Transactional
    public AlertThresholdResponse upsert(UUID patientId, CreateThresholdRequest req) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        String prescribedBy = extractCurrentUserEmail();

        // Désactiver l'ancien seuil s'il existe
        thresholdRepo.findByPatientIdAndDeviceTypeAndActiveTrue(patientId, req.deviceType())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    thresholdRepo.save(existing);
                    log.info("Ancien seuil désactivé — patient={} type={}", patientId, req.deviceType());
                });

        AlertThreshold threshold = AlertThreshold.builder()
                .patient(patient)
                .deviceType(req.deviceType())
                .criticalLow(req.criticalLow())
                .warningLow(req.warningLow())
                .warningHigh(req.warningHigh())
                .criticalHigh(req.criticalHigh())
                .deltaThreshold48h(req.deltaThreshold48h())
                .prescribedByEmail(prescribedBy)
                .active(true)
                .build();

        threshold = thresholdRepo.save(threshold);

        log.info("Seuil créé — patient={} type={} critH={} warnH={}",
                patientId, req.deviceType(), req.criticalHigh(), req.warningHigh());

        return toResponse(threshold);
    }

    // ── Lister les seuils d'un patient ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AlertThresholdResponse> getForPatient(UUID patientId) {
        return thresholdRepo.findByPatientIdAndActiveTrue(patientId)
                .stream().map(this::toResponse).toList();
    }

    // ── Désactiver un seuil ────────────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID thresholdId) {
        thresholdRepo.findById(thresholdId).ifPresent(t -> {
            t.setActive(false);
            thresholdRepo.save(t);
        });
    }

    // ── Seuils par défaut pour un patient nouvellement inscrit ────────────────
    // Pré-peuple les seuils selon la pathologie principale

    @Transactional
    public void applyDefaultThresholds(Patient patient) {
        switch (patient.getPrimaryPathology()) {
            case DIABETE_T2 -> {
                upsertDefault(patient, DeviceType.GLUCOMETER,
                        0.6, 0.8, 2.5, 4.0, null);
            }
            case ICFe -> {
                upsertDefault(patient, DeviceType.PULSE_OXIMETER,
                        null, null, null, null, null);
                upsertDefault(patient, DeviceType.SCALE,
                        null, null, null, null, 2.0);
                upsertDefault(patient, DeviceType.HEART_RATE,
                        40.0, 50.0, 110.0, 130.0, null);
            }
            case BPCO -> {
                upsertDefault(patient, DeviceType.PULSE_OXIMETER,
                        88.0, 91.0, null, null, null);
            }
            default -> log.debug("Pas de seuils par défaut pour {}", patient.getPrimaryPathology());
        }
    }

    private void upsertDefault(Patient patient, DeviceType dt,
            Double critLo, Double warnLo, Double warnHi, Double critHi, Double delta) {

        if (thresholdRepo.existsByPatientIdAndDeviceType(patient.getId(), dt)) return;

        thresholdRepo.save(AlertThreshold.builder()
                .patient(patient)
                .deviceType(dt)
                .criticalLow(critLo  != null ? java.math.BigDecimal.valueOf(critLo)  : null)
                .warningLow (warnLo  != null ? java.math.BigDecimal.valueOf(warnLo)  : null)
                .warningHigh(warnHi  != null ? java.math.BigDecimal.valueOf(warnHi)  : null)
                .criticalHigh(critHi != null ? java.math.BigDecimal.valueOf(critHi)  : null)
                .deltaThreshold48h(delta != null ? java.math.BigDecimal.valueOf(delta) : null)
                .prescribedByEmail("system@caresync.be")
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractCurrentUserEmail() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt)
                return jwt.getClaimAsString("email");
        } catch (Exception ignored) {}
        return "system@caresync.be";
    }

    private AlertThresholdResponse toResponse(AlertThreshold t) {
        return new AlertThresholdResponse(
                t.getId(), t.getPatient().getId(), t.getDeviceType(),
                t.getCriticalLow(), t.getWarningLow(),
                t.getWarningHigh(), t.getCriticalHigh(),
                t.getDeltaThreshold48h(),
                t.getPrescribedByEmail(), t.isActive()
        );
    }
}
