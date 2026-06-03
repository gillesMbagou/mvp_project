package be.caresync.controller.alert;

import be.caresync.domain.alert.Alert;
import be.caresync.domain.alert.AlertRepository;
import be.caresync.dto.request.AcknowledgeAlertRequest;
import be.caresync.dto.response.AlertSummaryResponse;
import be.caresync.exception.AlertNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertes Cliniques", description = "Gestion des alertes IoT et cliniques")
public class AlertController {

    private final AlertRepository alertRepo;

    @GetMapping
    @Operation(summary = "Lister les alertes actives")
    public ResponseEntity<List<AlertSummaryResponse>> getActiveAlerts() {
        List<Alert> alerts = alertRepo.findByStatusOrderBySeverityDescCreatedAtDesc(
                Alert.AlertStatus.ACTIVE);
        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Alertes d'un patient")
    public ResponseEntity<Page<AlertSummaryResponse>> getPatientAlerts(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Alert> alerts = alertRepo.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
        return ResponseEntity.ok(alerts.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une alerte")
    public ResponseEntity<AlertSummaryResponse> getAlert(@PathVariable UUID id) {
        Alert alert = alertRepo.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acquitter une alerte")
    @PreAuthorize("hasAnyRole('MEDECIN','INFIRMIER','ADMIN')")
    public ResponseEntity<AlertSummaryResponse> acknowledge(
            @PathVariable UUID id,
            @RequestBody @Valid AcknowledgeAlertRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        Alert alert = alertRepo.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        String userEmail = jwt.getClaimAsString("email");
        alert.acknowledge(userEmail, req.getComment());
        alertRepo.save(alert);

        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Résoudre une alerte")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<AlertSummaryResponse> resolve(
            @PathVariable UUID id,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal Jwt jwt) {

        Alert alert = alertRepo.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        alert.resolve(note != null ? note : "Résolu par " + jwt.getClaimAsString("email"));
        alertRepo.save(alert);

        return ResponseEntity.ok(toResponse(alert));
    }

    private AlertSummaryResponse toResponse(Alert a) {
        return AlertSummaryResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getFirstName() + " " + a.getPatient().getLastName())
                .type(a.getType())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .status(a.getStatus())
                .escalationLevel(a.getEscalationLevel())
                .triggeredValue(a.getTriggeredValue())
                .thresholdValue(a.getThresholdValue())
                .createdAt(a.getCreatedAt())
                .acknowledgedAt(a.getAcknowledgedAt())
                .acknowledgedBy(a.getAcknowledgedByEmail())
                .build();
    }
}
