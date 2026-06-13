package be.caresync.demo.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consomme les alertes glycémiques depuis glucose.alerts (JSON brut String)
 * et les publie sur le canal Redis Pub/Sub "caresync:notifications".
 *
 * Les abonnés (push mobile, SMS, email) n'ont qu'à SUBSCRIBE sur ce canal
 * sans dépendance directe à Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String NOTIFICATION_CHANNEL = "caresync:notifications";

    private final ReactiveStringRedisTemplate redisTemplate;

    @KafkaListener(
        topics           = "glucose.alerts",
        groupId          = "notification-service",
        containerFactory = "notificationListenerContainerFactory"
    )
    public void onAlert(String alertJson) {
        redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, alertJson)
            .doOnNext(subscribers ->
                log.info("Notification publiée → {} abonné(s) [canal: {}]",
                    subscribers, NOTIFICATION_CHANNEL))
            .doOnError(e ->
                log.error("Erreur publication Redis Pub/Sub sur {}: {}",
                    NOTIFICATION_CHANNEL, e.getMessage()))
            .subscribe();
    }
}
