package be.caresync.exception;

import be.caresync.domain.iot.DeviceType;
import java.math.BigDecimal;
import java.util.UUID;

public class DeviceNotFoundException extends CaresynException {
    public DeviceNotFoundException(UUID id)       { super("Dispositif IoT introuvable : id=" + id); }
    public DeviceNotFoundException(String serial) { super("Dispositif IoT introuvable : serial=" + serial); }
}
