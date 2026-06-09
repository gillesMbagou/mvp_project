package be.caresync.demo.repository;

import be.caresync.demo.model.db.ObservationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationRecord, Long> {

    // Historique paginé par patient
    Page<ObservationRecord> findByPatientIdOrderByObservedAtDesc(
            String patientId, Pageable pageable);

    // Série temporelle pour graphiques
    List<ObservationRecord> findByPatientIdAndDeviceTypeAndObservedAtBetweenOrderByObservedAt(
            String patientId, String deviceType, Instant from, Instant to);

    // Alertes actives (severity != NORMAL)
    List<ObservationRecord> findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
            String severity, Instant after);

    // Dernière mesure par dispositif
    @Query("SELECT o FROM ObservationRecord o WHERE o.deviceSerial = :serial " +
           "ORDER BY o.observedAt DESC LIMIT 1")
    ObservationRecord findLatestByDevice(String serial);

    // Comptage par patient et sévérité
    long countByPatientIdAndSeverity(String patientId, String severity);

    // Statistiques 24h
    @Query("SELECT AVG(o.value), MIN(o.value), MAX(o.value) " +
           "FROM ObservationRecord o " +
           "WHERE o.patientId = :patientId AND o.deviceType = :deviceType " +
           "AND o.observedAt > :since")
    Object[] getStats(String patientId, String deviceType, Instant since);
}
