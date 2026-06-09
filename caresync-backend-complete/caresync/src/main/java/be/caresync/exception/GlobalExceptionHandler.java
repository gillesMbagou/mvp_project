package be.caresync.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Ressources introuvables (404) ─────────────────────────────────────────

    @ExceptionHandler(PatientNotFoundException.class)
    public ProblemDetail handlePatientNotFound(PatientNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "patient-not-found", ex.getMessage());
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    public ProblemDetail handleDeviceNotFound(DeviceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "device-not-found", ex.getMessage());
    }

    @ExceptionHandler(AlertNotFoundException.class)
    public ProblemDetail handleAlertNotFound(AlertNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "alert-not-found", ex.getMessage());
    }

    @ExceptionHandler(CarePlanNotFoundException.class)
    public ProblemDetail handleCarePlanNotFound(CarePlanNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "care-plan-not-found", ex.getMessage());
    }

    @ExceptionHandler(MessageThreadNotFoundException.class)
    public ProblemDetail handleThreadNotFound(MessageThreadNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "message-thread-not-found", ex.getMessage());
    }

    @ExceptionHandler(PrescriptionNotFoundException.class)
    public ProblemDetail handlePrescriptionNotFound(PrescriptionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "prescription-not-found", ex.getMessage());
    }

    @ExceptionHandler(ProtocolNotFoundException.class)
    public ProblemDetail handleProtocolNotFound(ProtocolNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "protocol-not-found", ex.getMessage());
    }

    // ── Erreurs métier (400 / 409) ────────────────────────────────────────────

    @ExceptionHandler(InvalidObservationException.class)
    public ProblemDetail handleInvalidObservation(InvalidObservationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-observation", ex.getMessage());
    }

    @ExceptionHandler(DrugInteractionException.class)
    public ProblemDetail handleDrugInteraction(DrugInteractionException ex) {
        return problem(HttpStatus.CONFLICT, "drug-interaction", ex.getMessage());
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ProblemDetail handleConsentRequired(ConsentRequiredException ex) {
        return problem(HttpStatus.FORBIDDEN, "consent-required", ex.getMessage());
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ProblemDetail handleDocumentProcessing(DocumentProcessingException ex) {
        log.error("Erreur traitement document : {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "document-processing-error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-argument", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "invalid-state", ex.getMessage());
    }

    // ── Validation (400) ──────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalide",
                        (existing, replacement) -> existing));

        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation-error", "Erreur(s) de validation");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Paramètre '%s' invalide — valeur '%s' attendue de type %s"
                .formatted(ex.getName(), ex.getValue(),
                           ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "inconnu");
        return problem(HttpStatus.BAD_REQUEST, "type-mismatch", msg);
    }

    // ── Sécurité (401 / 403) ──────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "access-denied",
                "Accès non autorisé — droits insuffisants pour cette ressource");
    }

    @ExceptionHandler(InvalidBearerTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidBearerTokenException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-token",
                "Token d'authentification invalide ou expiré");
    }

    // ── Erreur générique (500) ────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erreur inattendue : {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Une erreur interne est survenue — veuillez contacter le support");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://caresync.be/errors/" + type));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
