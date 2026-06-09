package be.caresync.demo.simulator;

import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.model.db.PatientProfile;
import be.caresync.demo.model.db.SimulatedDevice;
import be.caresync.demo.repository.ObservationRepository;
import be.caresync.demo.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * API REST CareSync — observations, patients, dispositifs.
 *
 * GET  /api/patients                          → liste des 3 patients
 * GET  /api/patients/{id}/observations        → historique paginé
 * GET  /api/patients/{id}/observations/latest → dernière mesure par device
 * GET  /api/observations/alerts               → alertes des 24 dernières heures
 * GET  /api/stats                             → statistiques globales
 * POST /api/devices/{serial}/scenario/{name}  → changer le scénario
 * GET  /api/devices                           → liste des dispositifs
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")   // Angular dev server
public class DashboardController {

    private final PatientRepository     patientRepo;
    private final ObservationRepository obsRepo;
    private final MultiPatientSimulator simulator;

    // ── Patients ──────────────────────────────────────────────────────────────

    @GetMapping("/patients")
    public List<PatientProfile> getPatients() {
        return patientRepo.findAll();
    }

    // ── Observations ──────────────────────────────────────────────────────────

    @GetMapping("/patients/{patientId}/observations")
    public Page<ObservationRecord> getObservations(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return obsRepo.findByPatientIdOrderByObservedAtDesc(
                patientId, PageRequest.of(page, size));
    }

    @GetMapping("/patients/{patientId}/observations/timeseries")
    public List<ObservationRecord> getTimeSeries(
            @PathVariable String patientId,
            @RequestParam String deviceType,
            @RequestParam(defaultValue = "24") int hours) {
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        return obsRepo
                .findByPatientIdAndDeviceTypeAndObservedAtBetweenOrderByObservedAt(
                        patientId, deviceType, from, Instant.now());
    }

    // ── Alertes ───────────────────────────────────────────────────────────────

    @GetMapping("/observations/alerts")
    public List<ObservationRecord> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return obsRepo.findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
                "NORMAL", since);
    }

    // ── Statistiques dashboard ────────────────────────────────────────────────

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        return Map.of(
            "totalObservations",  obsRepo.count(),
            "totalPatients",      patientRepo.count(),
            "totalDevices",       simulator.getAllDevices().size(),
            "alertsLast24h",      obsRepo
                .findBySeverityNotAndObservedAtAfterOrderByObservedAtDesc(
                        "NORMAL", since24h).size(),
            "criticalAlerts24h",  obsRepo
                .countBySeverityAndObservedAtAfter("CRITIQUE", since24h)
        );
    }

    // ── Dispositifs ───────────────────────────────────────────────────────────

    @GetMapping("/devices")
    public List<SimulatedDevice> getDevices() {
        return simulator.getAllDevices();
    }

    @PostMapping("/devices/{serial}/scenario/{scenario}")
    public ResponseEntity<Map<String, String>> setScenario(
            @PathVariable String serial,
            @PathVariable String scenario) {
        simulator.setScenario(serial, scenario.toUpperCase());
        return ResponseEntity.ok(Map.of(
            "device",   serial,
            "scenario", scenario.toUpperCase(),
            "status",   "activé"
        ));
    }
}
