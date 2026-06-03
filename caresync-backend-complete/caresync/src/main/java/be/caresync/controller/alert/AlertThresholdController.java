package be.caresync.controller.alert;

import be.caresync.dto.request.CreateThresholdRequest;
import be.caresync.dto.response.AlertThresholdResponse;
import be.caresync.service.alert.AlertThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/thresholds")
@RequiredArgsConstructor
@Tag(name = "Seuils d'Alerte", description = "Configuration des seuils cliniques par patient")
public class AlertThresholdController {

    private final AlertThresholdService thresholdService;

    @GetMapping
    @Operation(summary = "Seuils configurés pour un patient")
    @PreAuthorize("hasAnyRole('MEDECIN','INFIRMIER','ADMIN')")
    public ResponseEntity<List<AlertThresholdResponse>> getThresholds(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(thresholdService.getForPatient(patientId));
    }

    @PutMapping
    @Operation(summary = "Créer ou remplacer un seuil — prescrit par le médecin")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<AlertThresholdResponse> upsertThreshold(
            @PathVariable UUID patientId,
            @RequestBody @Valid CreateThresholdRequest req) {
        return ResponseEntity.ok(thresholdService.upsert(patientId, req));
    }

    @DeleteMapping("/{thresholdId}")
    @Operation(summary = "Désactiver un seuil")
    @PreAuthorize("hasAnyRole('MEDECIN','ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID thresholdId) {
        thresholdService.deactivate(thresholdId);
        return ResponseEntity.noContent().build();
    }
}
