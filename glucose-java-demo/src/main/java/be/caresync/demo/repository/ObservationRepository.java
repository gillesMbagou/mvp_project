package be.caresync.demo.repository;

import be.caresync.demo.model.db.ObservationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationRecord, Long> {

    Page<ObservationRecord> findByPatientIdOrderByObservedAtDesc(
            String patientId, Pageable pageable);

    List<ObservationRecord> findByPatientIdAndDeviceTypeAndObservedAtBetweenOrderByObservedAt(
            String patientId, String deviceType, Instant from, Instant to);

    List<ObservationRecord> findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
            String severity, Instant after);

    Optional<ObservationRecord> findTopByDeviceSerialOrderByObservedAtDesc(String deviceSerial);

    long countByPatientIdAndSeverity(String patientId, String severity);

    long countBySeverityAndObservedAtAfter(String severity, Instant after);

    @Query("SELECT AVG(o.value), MIN(o.value), MAX(o.value) " +
           "FROM ObservationRecord o " +
           "WHERE o.patientId = :patientId AND o.deviceType = :deviceType " +
           "AND o.observedAt > :since")
    Object[] getStats(@Param("patientId") String patientId,
                      @Param("deviceType") String deviceType,
                      @Param("since") Instant since);
}
