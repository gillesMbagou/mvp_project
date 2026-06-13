package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.SimulatedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<SimulatedDevice, String> {
    List<SimulatedDevice> findByPatientIdAndActiveTrue(String patientId);
    List<SimulatedDevice> findByActiveTrue();
}
