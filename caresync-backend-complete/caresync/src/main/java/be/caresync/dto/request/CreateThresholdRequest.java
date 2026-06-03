package be.caresync.dto.request;

import be.caresync.domain.iot.DeviceType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateThresholdRequest(
    @NotNull DeviceType deviceType,
    BigDecimal criticalLow,
    BigDecimal warningLow,
    BigDecimal warningHigh,
    BigDecimal criticalHigh,
    BigDecimal deltaThreshold48h    // Pour ICFe : variation poids 48h
) {}
