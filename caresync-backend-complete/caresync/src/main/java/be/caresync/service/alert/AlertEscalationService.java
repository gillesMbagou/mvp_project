package be.caresync.service.alert;

import be.caresync.domain.alert.Alert;
import be.caresync.domain.alert.AlertRepository;
import be.caresync.domain.alert.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Gestion de la chaîne d'escalade des alertes non acquittées.
 *
 * Règles d'escalade :
 *  T+3min  → Notification infirmier  (EscalationLevel.NONE → NURSE)
 *  T+15min → Escalade médecin        (NURSE → DOCTOR)
 *  T+30min → Médecin de garde        (DOCTOR → ON_CALL) pour les CRITIQUES
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEscalationService {

    private static final long NURSE_AFTER_MIN   = 3;
    private static final long DOCTOR_AFTER_MIN  = 15;
    private static final long ON_CALL_AFTER_MIN = 30;

    private final AlertRepository alertRepo;

    // Exécution toutes les 60 secondes
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processEscalations() {
        Instant now = Instant.now();

        escalateToNurse(now);
        escalateToDoctor(now);
        escalateToOnCall(now);
    }

    // ── Étape 1 : NONE → NURSE ────────────────────────────────────────────────

    private void escalateToNurse(Instant now) {
        Instant cutoff = now.minus(NURSE_AFTER_MIN, ChronoUnit.MINUTES);

        List<Alert> alerts = alertRepo.findAlertsRequiringEscalation(
                Alert.EscalationLevel.NONE, cutoff);

        alerts.forEach(alert -> {
            alert.escalate(Alert.EscalationLevel.NURSE, resolveNurseEmail(alert));
            alertRepo.save(alert);
            log.warn("Escalade INFIRMIER — alertId={} patient={} severity={}",
                    alert.getId(), alert.getPatient().getId(), alert.getSeverity());
            // TODO: notif.sendPushToNurse(alert);
        });
    }

    // ── Étape 2 : NURSE → DOCTOR ──────────────────────────────────────────────

    private void escalateToDoctor(Instant now) {
        Instant cutoff = now.minus(DOCTOR_AFTER_MIN, ChronoUnit.MINUTES);

        List<Alert> alerts = alertRepo.findAlertsRequiringEscalation(
                Alert.EscalationLevel.NURSE, cutoff);

        alerts.forEach(alert -> {
            alert.escalate(Alert.EscalationLevel.DOCTOR, resolveDoctorEmail(alert));
            alertRepo.save(alert);
            log.warn("Escalade MÉDECIN — alertId={} patient={}",
                    alert.getId(), alert.getPatient().getId());
            // TODO: notif.sendSmsToDoctor(alert);
        });
    }

    // ── Étape 3 : DOCTOR → ON_CALL (critiques uniquement) ────────────────────

    private void escalateToOnCall(Instant now) {
        Instant cutoff = now.minus(ON_CALL_AFTER_MIN, ChronoUnit.MINUTES);

        List<Alert> alerts = alertRepo.findAlertsRequiringEscalation(
                Alert.EscalationLevel.DOCTOR, cutoff);

        alerts.stream()
              .filter(a -> a.getSeverity() == Severity.CRITIQUE)
              .forEach(alert -> {
                  alert.escalate(Alert.EscalationLevel.ON_CALL, "guard@caresync.be");
                  alertRepo.save(alert);
                  log.error("Escalade MÉDECIN DE GARDE — alertId={} patient={}",
                          alert.getId(), alert.getPatient().getId());
                  // TODO: notif.callOnDutyDoctor(alert);
              });
    }

    // ── Résolution des destinataires (à remplacer par lookup DB) ─────────────

    private String resolveNurseEmail(Alert alert) {
        // TODO: chercher l'infirmier responsable du patient dans l'équipe de soins
        return "infirmier@caresync.be";
    }

    private String resolveDoctorEmail(Alert alert) {
        // TODO: chercher le médecin traitant depuis le dossier patient
        return "medecin@caresync.be";
    }
}
