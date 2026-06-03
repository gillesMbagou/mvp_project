package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import be.caresync.domain.iot.Observation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ObservationResponse(
    UUID                         id,
    UUID                         patientId,
    UUID                         deviceId,
    DeviceType                   deviceType,
    String                       loincCode,
    BigDecimal                   value,
    String                       unit,
    Instant                      observedAt,
    Observation.ObservationSource source,
    Observation.ObservationStatus status,
    String                       note,
    Long                         latencyMs
) {}
