package be.caresync.domain.alert;

import be.caresync.domain.common.BaseEntity;
import be.caresync.domain.iot.Observation;
import be.caresync.domain.patient.Patient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_patient_status", columnList = "patient_id, status"),
    @Index(name = "idx_alert_severity",        columnList = "severity"),
    @Index(name = "idx_alert_created",         columnList = "created_at DESC"),
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Observation déclencheuse (nullable si alerte créée manuellement)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id")
    private Observation triggeringObservation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    // ── Escalade ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private EscalationLevel escalationLevel = EscalationLevel.NONE;

    @Column(name = "first_notified_at")
    private Instant firstNotifiedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "escalated_to_email")
    private String escalatedToEmail;

    // ── Acquittement ──────────────────────────────────────────────────────────

    @Column(name = "acknowledged_by")
    private String acknowledgedByEmail;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledgment_comment", columnDefinition = "TEXT")
    private String acknowledgmentComment;

    // ── Résolution ────────────────────────────────────────────────────────────

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    // ── Métadonnées ───────────────────────────────────────────────────────────

    // Valeur qui a déclenché l'alerte (snapshottée pour l'historique)
    @Column(name = "triggered_value")
    private String triggeredValue;

    @Column(name = "threshold_value")
    private String thresholdValue;

    // ── Méthodes métier ───────────────────────────────────────────────────────

    public boolean isActive()    { return status == AlertStatus.ACTIVE; }
    public boolean isResolved()  { return status == AlertStatus.RESOLVED; }

    public void acknowledge(String byEmail, String comment) {
        this.status                  = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedByEmail     = byEmail;
        this.acknowledgedAt          = Instant.now();
        this.acknowledgmentComment   = comment;
    }

    public void resolve(String note) {
        this.status      = AlertStatus.RESOLVED;
        this.resolvedAt  = Instant.now();
        this.resolutionNote = note;
    }

    public void escalate(EscalationLevel level, String toEmail) {
        this.escalationLevel  = level;
        this.escalatedAt      = Instant.now();
        this.escalatedToEmail = toEmail;
    }

    public enum AlertStatus {
        ACTIVE,         // Alerte créée, non traitée
        ACKNOWLEDGED,   // Acquittée par un professionnel
        RESOLVED,       // Résolue (valeurs revenues à la normale)
        SUPPRESSED      // Supprimée manuellement (faux positif)
    }

    public enum EscalationLevel {
        NONE,       // Pas encore escaladée
        NURSE,      // Notifié infirmier (T+3s)
        DOCTOR,     // Escaladée au médecin (T+15min)
        ON_CALL     // Médecin de garde (T+30min)
    }
}
