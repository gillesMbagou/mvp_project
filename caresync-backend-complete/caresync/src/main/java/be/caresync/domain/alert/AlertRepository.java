package be.caresync.domain.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // Alertes actives d'un patient (pour la console infirmier)
    List<Alert> findByPatientIdAndStatusOrderByCreatedAtDesc(
            UUID patientId, Alert.AlertStatus status);

    // Toutes les alertes actives (pour le dashboard opérationnel)
    List<Alert> findByStatusOrderBySeverityDescCreatedAtDesc(Alert.AlertStatus status);

    // Alertes actives non escaladées depuis N minutes (pour le scheduler d'escalade)
    @Query("""
        SELECT a FROM Alert a
        WHERE a.status = 'ACTIVE'
          AND a.escalationLevel = :level
          AND a.firstNotifiedAt < :cutoff
          AND a.severity IN ('CRITIQUE', 'URGENTE')
        """)
    List<Alert> findAlertsRequiringEscalation(Alert.EscalationLevel level, Instant cutoff);

    // Pour les analytics : comptage par sévérité sur une période
    @Query("""
        SELECT a.severity, COUNT(a) FROM Alert a
        WHERE a.createdAt BETWEEN :from AND :to
        GROUP BY a.severity
        """)
    List<Object[]> countBySeverityBetween(Instant from, Instant to);

    // Alertes récentes d'un patient (feed patient)
    Page<Alert> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

    // Nombre d'alertes actives critiques (KPI dashboard)
    long countByStatusAndSeverity(Alert.AlertStatus status, Severity severity);

    long countByPatientIdAndStatusAndCreatedAtAfter(
            UUID patientId, Alert.AlertStatus status, Instant since);
}
