package be.caresync.exception;

import java.util.UUID;

// ── Base ──────────────────────────────────────────────────────────────────────
public class CaresynException extends RuntimeException {
    public CaresynException(String message) { super(message); }
    public CaresynException(String message, Throwable cause) { super(message, cause); }
}
