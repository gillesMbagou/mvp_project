package be.caresync.service.iot;

import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.IoTDevice;
import be.caresync.domain.iot.IoTDeviceRepository;
import be.caresync.domain.patient.Patient;
import be.caresync.domain.patient.PatientRepository;
import be.caresync.dto.request.RegisterDeviceRequest;
import be.caresync.dto.response.IoTDeviceResponse;
import be.caresync.exception.DeviceNotFoundException;
import be.caresync.exception.PatientNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IoTDeviceService {

    private final IoTDeviceRepository deviceRepo;
    private final PatientRepository   patientRepo;

    // ── Enregistrement d'un dispositif ────────────────────────────────────────

    @Transactional
    public IoTDeviceResponse register(RegisterDeviceRequest req) {
        Patient patient = patientRepo.findById(req.patientId())
                .orElseThrow(() -> new PatientNotFoundException(req.patientId()));

        // Topic MQTT : caresync/devices/{serial}/{type}
        String mqttTopic = "caresync/devices/%s/%s"
                .formatted(req.serialNumber(), req.deviceType().name().toLowerCase());

        IoTDevice device = IoTDevice.builder()
                .patient(patient)
                .deviceType(req.deviceType())
                .manufacturer(req.manufacturer())
                .model(req.model())
                .serialNumber(req.serialNumber())
                .mqttTopic(mqttTopic)
                .protocol(req.protocol())
                .firmwareVersion(req.firmwareVersion())
                .active(true)
                .build();

        device = deviceRepo.save(device);
        log.info("Dispositif enregistré — serial={} type={} patient={}",
                device.getSerialNumber(), device.getDeviceType(), patient.getId());

        return toResponse(device);
    }

    // ── Désactivation (soft delete) ────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID deviceId) {
        IoTDevice device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        device.setActive(false);
        deviceRepo.save(device);
        log.info("Dispositif désactivé — id={} serial={}", deviceId, device.getSerialNumber());
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IoTDeviceResponse> getDevicesForPatient(UUID patientId) {
        return deviceRepo.findByPatientIdAndActiveTrue(patientId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IoTDeviceResponse getDevice(UUID deviceId) {
        return toResponse(deviceRepo.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId)));
    }

    // ── Détection des dispositifs hors-ligne ──────────────────────────────────
    // Tournée toutes les 5 minutes — si un dispositif n'a pas envoyé depuis 3h,
    // une alerte "perte de signal" est générée.

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void detectOfflineDevices() {
        Instant cutoff = Instant.now().minus(3, ChronoUnit.HOURS);
        List<IoTDevice> offline = deviceRepo.findOfflineDevices(cutoff);

        offline.forEach(d -> {
            log.warn("Dispositif hors-ligne détecté — serial={} lastSeen={}",
                    d.getSerialNumber(), d.getLastSeenAt());
            // TODO: publier alerte AlertType.DISPOSITIF via AlertEngineService
        });
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private IoTDeviceResponse toResponse(IoTDevice d) {
        return new IoTDeviceResponse(
                d.getId(),
                d.getPatient().getId(),
                d.getDeviceType(),
                d.getManufacturer(),
                d.getModel(),
                d.getSerialNumber(),
                d.getMqttTopic(),
                d.getProtocol(),
                d.isActive(),
                d.getLastSeenAt(),
                d.getFirmwareVersion()
        );
    }
}
