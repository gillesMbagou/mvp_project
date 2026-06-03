package be.caresync.exception;

import be.caresync.domain.iot.DeviceType;
import java.math.BigDecimal;
import java.util.UUID;

public class AlertNotFoundException extends CaresynException {
    public AlertNotFoundException(UUID id) { super("Alerte introuvable : id=" + id); }
}
