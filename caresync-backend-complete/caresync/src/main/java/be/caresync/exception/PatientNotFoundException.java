package be.caresync.exception;

import java.util.UUID;

public class PatientNotFoundException extends CaresynException {
    public PatientNotFoundException(UUID id) {
        super("Patient introuvable : id=" + id);
    }
    public PatientNotFoundException(String identifier) {
        super("Patient introuvable : " + identifier);
    }
}
