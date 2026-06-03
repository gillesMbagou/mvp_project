package be.caresync.domain.alert;

import be.caresync.domain.iot.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, UUID> {

    Optional<AlertThreshold> findByPatientIdAndDeviceTypeAndActiveTrue(
            UUID patientId, DeviceType deviceType);

    List<AlertThreshold> findByPatientIdAndActiveTrue(UUID patientId);

    boolean existsByPatientIdAndDeviceType(UUID patientId, DeviceType deviceType);
}
