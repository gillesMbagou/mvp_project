package be.caresync.controller.iot;

import be.caresync.domain.iot.*;
import be.caresync.domain.patient.Patient;
import be.caresync.domain.patient.PatientRepository;
import be.caresync.dto.request.CreateObservationRequest;
import be.caresync.exception.*;
import be.caresync.service.alert.AlertEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/iot")
@RequiredArgsConstructor
@Tag(name = "Télésurveillance IoT", description = "Observations et dispositifs connectés")
public class ObservationController {

    private final ObservationRepository observationRepo;
    private final IoTDeviceRepository   deviceRepo;
    private final PatientRepository     patientRepo;
    private final AlertEngineService    alertEngine;

    // ── Observations ──────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/observations")
    @Operation(summary = "Historique des observations d'un patient")
    public ResponseEntity<Page<Observation>> getObservations(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("observedAt").descending());
        return ResponseEntity.ok(observationRepo.findByPatientIdOrderByObservedAtDesc(patientId, pageable));
    }

    @GetMapping("/patients/{patientId}/observations/latest/{deviceType}")
    @Operation(summary = "Dernière mesure par type de dispositif")
    public ResponseEntity<Observation> getLatest(
            @PathVariable UUID patientId,
            @PathVariable DeviceType deviceType) {

        return observationRepo
                .findLatestByPatientAndType(patientId, deviceType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/observations")
    @Operation(summary = "Saisie manuelle d'une observation (médecin/infirmier)")
    @PreAuthorize("hasAnyRole('MEDECIN','INFIRMIER')")
    public ResponseEntity<Observation> createManualObservation(
            @RequestBody @Valid CreateObservationRequest req) {

        Patient patient = patientRepo.findById(req.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(req.getPatientId()));

        // Validation physiologique
        if (!req.getDeviceType().isPhysiologicallyValid(req.getValue().doubleValue())) {
            throw new InvalidObservationException(req.getDeviceType(), req.getValue());
        }

        Observation obs = Observation.builder()
                .patient(patient)
                .loincCode(req.getDeviceType().getLoincCode())
                .deviceType(req.getDeviceType())
                .value(req.getValue())
                .unit(req.getUnit())
                .observedAt(req.getObservedAt() != null ? req.getObservedAt() : Instant.now())
                .source(Observation.ObservationSource.MANUAL)
                .status(Observation.ObservationStatus.FINAL)
                .note(req.getNote())
                .build();

        obs = observationRepo.save(obs);

        // Évaluation des alertes même pour les saisies manuelles
        alertEngine.evaluate(obs);

        return ResponseEntity.status(201).body(obs);
    }

    // ── Dispositifs ───────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/devices")
    @Operation(summary = "Dispositifs actifs d'un patient")
    public ResponseEntity<?> getDevices(@PathVariable UUID patientId) {
        return ResponseEntity.ok(deviceRepo.findByPatientIdAndActiveTrue(patientId));
    }
}
