package be.caresync.domain.alert;

public enum Severity {
    NONE,
    INFORMATIVE,    // Information — aucune action immédiate requise
    URGENTE,        // Action requise sous 15 minutes
    CRITIQUE        // Action immédiate requise
}
