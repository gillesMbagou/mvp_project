package be.caresync.alert.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ALERT — persistance des alertes cliniques produites par AlertProcessor
 * (jusqu'ici un pur transform Kafka → Kafka, sans trace exploitable en
 * lecture par le frontend : /api/v1/alertes n'avait aucune Resource REST).
 */
@Entity
@Table(name = "alerts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Alert extends PanacheEntityBase {

    @Id
    private String alertId;

    @Column(nullable = false) private String patientId;
    private String deviceSerial;
    private String deviceType;
    private String loincCode;

    @Column(nullable = false) private String severity;
    private String message;
    private String triggeredValue;
    private String thresholdType;

    @Column(nullable = false) private Instant createdAt;
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.OUVERTE;

    public enum Statut { OUVERTE, ACQUITTEE, ESCALADEE, RESOLUE }

    public static List<Alert> findRecent(String patientId, int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        if (patientId != null && !patientId.isBlank()) {
            return find("patientId = ?1 AND createdAt >= ?2 ORDER BY createdAt DESC", patientId, since).list();
        }
        return find("createdAt >= ?1 ORDER BY createdAt DESC", since).list();
    }
}
