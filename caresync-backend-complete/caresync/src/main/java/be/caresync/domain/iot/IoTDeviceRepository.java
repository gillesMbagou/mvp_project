package be.caresync.domain.iot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IoTDeviceRepository extends JpaRepository<IoTDevice, UUID> {

    Optional<IoTDevice> findBySerialNumber(String serialNumber);
    Optional<IoTDevice> findByMqttTopic(String topic);
    List<IoTDevice>     findByPatientIdAndActiveTrue(UUID patientId);
    List<IoTDevice>     findByDeviceTypeAndActiveTrue(DeviceType deviceType);

    @Query("SELECT d FROM IoTDevice d WHERE d.active = true AND d.lastSeenAt < :cutoff")
    List<IoTDevice> findOfflineDevices(java.time.Instant cutoff);
}
