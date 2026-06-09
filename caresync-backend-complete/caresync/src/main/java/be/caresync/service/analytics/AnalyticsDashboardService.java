package be.caresync.service.analytics;

import be.caresync.domain.alert.Alert;
import be.caresync.domain.alert.AlertRepository;
import be.caresync.domain.alert.Severity;
import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.ObservationRepository;
import be.caresync.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service d'analytique clinique.
 * Produit les KPIs pour les 3 niveaux de tableaux de bord :
 *  - Opérationnel (temps réel)
 *  - Tactique     (7 jours)
 *  - Stratégique  (30 jours)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDashboardService {

    private final AlertRepository       alertRepo;
    private final ObservationRepository observationRepo;

    // ── Dashboard opérationnel ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OperationalDashboardResponse getOperationalDashboard() {

        Instant now      = Instant.now();
        Instant since24h = now.minus(24, ChronoUnit.HOURS);

        long activeAlerts    = alertRepo.countByStatusAndSeverity(Alert.AlertStatus.ACTIVE, Severity.URGENTE)
                             + alertRepo.countByStatusAndSeverity(Alert.AlertStatus.ACTIVE, Severity.CRITIQUE);
        long criticalAlerts  = alertRepo.countByStatusAndSeverity(Alert.AlertStatus.ACTIVE, Severity.CRITIQUE);

        List<Alert> recentAlerts = alertRepo.findByStatusOrderBySeverityDescCreatedAtDesc(
                Alert.AlertStatus.ACTIVE);

        return OperationalDashboardResponse.builder()
                .activeAlertsCount(activeAlerts)
                .criticalAlertsCount(criticalAlerts)
                .recentAlerts(recentAlerts.stream().limit(10).map(this::toAlertSummary).toList())
                .generatedAt(now)
                .build();
    }

    // ── Dashboard tactique (7 jours) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public TacticalDashboardResponse getTacticalDashboard(UUID patientId) {

        Instant now     = Instant.now();
        Instant since7d = now.minus(7, ChronoUnit.DAYS);

        // Moyennes des vitaux sur 7 jours
        Map<DeviceType, Double> averages = new EnumMap<>(DeviceType.class);
        for (DeviceType dt : DeviceType.values()) {
            Double avg = observationRepo.averageValueByPatientAndPeriod(patientId, dt, since7d, now);
            if (avg != null) averages.put(dt, avg);
        }

        // Alertes par sévérité sur 7 jours
        List<Object[]> alertCountsBySeverity = alertRepo.countBySeverityBetween(since7d, now);

        Map<String, Long> alertsBySeverity = new LinkedHashMap<>();
        alertCountsBySeverity.forEach(row ->
                alertsBySeverity.put(((Severity) row[0]).name(), (Long) row[1]));

        return TacticalDashboardResponse.builder()
                .patientId(patientId)
                .periodDays(7)
                .vitalAverages(averages)
                .alertsBySeverity(alertsBySeverity)
                .generatedAt(now)
                .build();
    }

    // ── Dashboard stratégique (30 jours) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public StrategicDashboardResponse getStrategicDashboard() {

        Instant now      = Instant.now();
        Instant since30d = now.minus(30, ChronoUnit.DAYS);

        List<Object[]> alertCounts = alertRepo.countBySeverityBetween(since30d, now);

        long totalAlerts    = alertCounts.stream().mapToLong(r -> (Long) r[1]).sum();
        long criticalCount  = alertCounts.stream()
                .filter(r -> r[0] == Severity.CRITIQUE)
                .mapToLong(r -> (Long) r[1]).sum();

        double escalationRate = totalAlerts > 0
                ? (double) criticalCount / totalAlerts * 100 : 0;

        return StrategicDashboardResponse.builder()
                .periodDays(30)
                .totalAlertsGenerated(totalAlerts)
                .criticalAlertsCount(criticalCount)
                .escalationRatePercent(Math.round(escalationRate * 10.0) / 10.0)
                .generatedAt(now)
                .build();
    }

    // ── Série temporelle d'une mesure (pour les graphiques) ───────────────────

    @Transactional(readOnly = true)
    public TimeSeriesResponse getTimeSeries(UUID patientId, DeviceType deviceType,
                                             int lastHours) {
        Instant to   = Instant.now();
        Instant from = to.minus(lastHours, ChronoUnit.HOURS);

        var observations = observationRepo.findTimeSeries(patientId, deviceType, from, to);

        List<TimeSeriesResponse.DataPoint> points = observations.stream()
                .map(obs -> new TimeSeriesResponse.DataPoint(obs.getObservedAt(), obs.getValue()))
                .toList();

        return TimeSeriesResponse.builder()
                .patientId(patientId)
                .deviceType(deviceType)
                .loincCode(deviceType.getLoincCode())
                .unit(deviceType.getUnit())
                .from(from)
                .to(to)
                .points(points)
                .build();
    }

    // ── Mapper local ──────────────────────────────────────────────────────────

    private AlertSummaryResponse toAlertSummary(Alert a) {
        return AlertSummaryResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getFirstName() + " " + a.getPatient().getLastName())
                .type(a.getType())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .status(a.getStatus())
                .escalationLevel(a.getEscalationLevel())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
