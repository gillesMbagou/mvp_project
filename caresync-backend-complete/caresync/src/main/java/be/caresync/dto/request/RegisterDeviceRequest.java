package be.caresync.dto.request;

import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.IoTDevice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterDeviceRequest(
    @NotNull  UUID        patientId,
    @NotNull  DeviceType  deviceType,
    @NotBlank String      manufacturer,
    @NotBlank String      model,
    @NotBlank String      serialNumber,
    @NotNull  IoTDevice.Protocol protocol,
    String                firmwareVersion
) {}
