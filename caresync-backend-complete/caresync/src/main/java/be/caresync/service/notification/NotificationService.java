package be.caresync.service.notification;

import be.caresync.domain.alert.Alert;
import be.caresync.domain.alert.Severity;
import be.caresync.messaging.kafka.events.AlertCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consomme les événements Kafka caresync.alerts
 * et dispatche les notifications aux professionnels de santé.
 *
 * Remplacer les log.info() par les vrais appels :
 *   - Push : Firebase FCM / Apple APNs
 *   - SMS  : Twilio / OVH SMS
 *   - Email: Spring Mail (MailHog en dev)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @KafkaListener(
        topics           = "caresync.alerts",
        groupId          = "caresync-notifications",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAlertCreated(ConsumerRecord<String, AlertCreatedEvent> record) {
        AlertCreatedEvent event = record.value();

        log.info("Notification reçue pour alerte {} — sévérité={} patient={}",
                event.getAlertId(), event.getSeverity(), event.getPatientId());

        switch (event.getSeverity()) {
            case CRITIQUE -> {
                sendPushNotification(event.getNurseEmail(),
                        "⚠️ Alerte critique — " + event.getPatientName(),
                        event.getMessage());
                if (event.getDoctorEmail() != null) {
                    sendSms(event.getDoctorEmail(),
                            "[CareSync CRITIQUE] " + event.getPatientName() + " : " + event.getMessage());
                }
            }
            case URGENTE -> {
                sendPushNotification(event.getNurseEmail(),
                        "🔔 Alerte urgente — " + event.getPatientName(),
                        event.getMessage());
            }
            case INFORMATIVE -> {
                log.info("Notification informative — {}", event.getMessage());
            }
            default -> {}
        }
    }

    // ── Stubs à brancher sur les vrais services ────────────────────────────────

    private void sendPushNotification(String recipientEmail, String title, String body) {
        // TODO: FirebaseMessaging.getInstance().send(Message.builder()...)
        log.info("[PUSH] → {} | {} | {}", recipientEmail, title, body);
    }

    private void sendSms(String recipientEmail, String message) {
        // TODO: Twilio ou OVH SMS API
        log.info("[SMS] → {} | {}", recipientEmail, message);
    }

    private void sendEmail(String to, String subject, String body) {
        // TODO: JavaMailSender.send(...)
        log.info("[EMAIL] → {} | {}", to, subject);
    }

    // ── API directe (appelée par AlertEscalationService) ─────────────────────

    public void notifyEscalation(Alert alert, String toEmail, String level) {
        String msg = "[CareSync %s] Escalade %s — Patient %s %s : %s".formatted(
                alert.getSeverity(), level,
                alert.getPatient().getFirstName(), alert.getPatient().getLastName(),
                alert.getMessage());

        sendPushNotification(toEmail, "Escalade " + level, msg);
        if (alert.getSeverity() == Severity.CRITIQUE) sendSms(toEmail, msg);

        log.warn("Escalade notifiée — alertId={} level={} to={}", alert.getId(), level, toEmail);
    }
}
