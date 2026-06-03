package be.caresync.exception;

import be.caresync.domain.iot.DeviceType;
import java.math.BigDecimal;

public class InvalidObservationException extends CaresynException {
    public InvalidObservationException(DeviceType type, BigDecimal value) {
        super("Observation physiologiquement invalide — type=%s value=%s (plage: [%.1f, %.1f])"
                .formatted(type.name(), value.toPlainString(), type.getPhysioMin(), type.getPhysioMax()));
    }
    public InvalidObservationException(String reason) {
        super("Observation invalide : " + reason);
    }
}
