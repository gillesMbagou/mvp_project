package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.IoTDevice;
import java.time.Instant;
import java.util.UUID;

public record IoTDeviceResponse(
    UUID              id,
    UUID              patientId,
    DeviceType        deviceType,
    String            manufacturer,
    String            model,
    String            serialNumber,
    String            mqttTopic,
    IoTDevice.Protocol protocol,
    boolean           active,
    Instant           lastSeenAt,
    String            firmwareVersion
) {}
