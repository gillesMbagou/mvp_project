package be.caresync.dto.response;

import be.caresync.domain.iot.DeviceType;
import java.math.BigDecimal;
import java.util.UUID;

public record AlertThresholdResponse(
    UUID       id,
    UUID       patientId,
    DeviceType deviceType,
    BigDecimal criticalLow,
    BigDecimal warningLow,
    BigDecimal warningHigh,
    BigDecimal criticalHigh,
    BigDecimal deltaThreshold48h,
    String     prescribedByEmail,
    boolean    active
) {}
