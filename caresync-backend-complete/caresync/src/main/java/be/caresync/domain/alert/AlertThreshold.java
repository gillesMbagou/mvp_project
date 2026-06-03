package be.caresync.domain.alert;

import be.caresync.domain.common.BaseEntity;
import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.patient.Patient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Seuil d'alerte configurable par patient et par type de mesure.
 * Permet une médecine personnalisée : deux patients diabétiques
 * peuvent avoir des seuils de glycémie différents.
 */
@Entity
@Table(name = "alert_thresholds", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"patient_id", "device_type"})
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertThreshold extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    // Seuil bas critique (ex: glycémie < 0.6 g/L → CRITIQUE)
    @Column(name = "critical_low", precision = 10, scale = 3)
    private BigDecimal criticalLow;

    // Seuil bas d'alerte (ex: glycémie < 0.8 g/L → URGENTE)
    @Column(name = "warning_low", precision = 10, scale = 3)
    private BigDecimal warningLow;

    // Seuil haut d'alerte (ex: glycémie > 2.5 g/L → URGENTE)
    @Column(name = "warning_high", precision = 10, scale = 3)
    private BigDecimal warningHigh;

    // Seuil haut critique (ex: glycémie > 4.0 g/L → CRITIQUE)
    @Column(name = "critical_high", precision = 10, scale = 3)
    private BigDecimal criticalHigh;

    // Seuil de variation rapide (ex: poids +2 kg/48h → ICFe)
    @Column(name = "delta_threshold_48h", precision = 10, scale = 3)
    private BigDecimal deltaThreshold48h;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Médecin prescripteur du seuil
    @Column(name = "prescribed_by")
    private String prescribedByEmail;

    public Severity evaluate(BigDecimal value) {
        if (criticalLow  != null && value.compareTo(criticalLow)  < 0) return Severity.CRITIQUE;
        if (criticalHigh != null && value.compareTo(criticalHigh) > 0) return Severity.CRITIQUE;
        if (warningLow   != null && value.compareTo(warningLow)   < 0) return Severity.URGENTE;
        if (warningHigh  != null && value.compareTo(warningHigh)  > 0) return Severity.URGENTE;
        return Severity.NONE;
    }
}
