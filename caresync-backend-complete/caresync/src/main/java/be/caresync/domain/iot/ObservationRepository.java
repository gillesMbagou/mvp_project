package be.caresync.domain.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    // Dernières observations par patient et type de dispositif
    List<Observation> findByPatientIdAndDeviceTypeOrderByObservedAtDesc(
            UUID patientId, DeviceType deviceType, Pageable pageable);

    // Pour l'alert engine : observations récentes d'un patient
    List<Observation> findByPatientIdAndObservedAtAfterOrderByObservedAtDesc(
            UUID patientId, Instant since);

    // Dernier relevé par patient + type (utile pour l'écran de monitoring)
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patient.id = :patientId
          AND o.deviceType = :deviceType
          AND o.observedAt = (
              SELECT MAX(o2.observedAt) FROM Observation o2
              WHERE o2.patient.id = :patientId AND o2.deviceType = :deviceType
          )
        """)
    Optional<Observation> findLatestByPatientAndType(UUID patientId, DeviceType deviceType);

    // Pour analytics : série temporelle
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patient.id = :patientId
          AND o.deviceType = :deviceType
          AND o.observedAt BETWEEN :from AND :to
        ORDER BY o.observedAt ASC
        """)
    List<Observation> findTimeSeries(UUID patientId, DeviceType deviceType,
                                     Instant from, Instant to);

    // Statistiques : moyenne sur une période
    @Query("""
        SELECT AVG(o.value) FROM Observation o
        WHERE o.patient.id = :patientId
          AND o.deviceType = :deviceType
          AND o.observedAt BETWEEN :from AND :to
        """)
    Double averageValueByPatientAndPeriod(UUID patientId, DeviceType deviceType,
                                          Instant from, Instant to);

    Page<Observation> findByPatientIdOrderByObservedAtDesc(UUID patientId, Pageable pageable);

    long countByPatientIdAndObservedAtAfter(UUID patientId, Instant since);
}
