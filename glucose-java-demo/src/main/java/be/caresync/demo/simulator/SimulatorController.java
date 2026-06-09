package be.caresync.demo.simulator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API REST de contrôle du simulateur FreeStyle Libre 3.
 *
 * Endpoints :
 *   POST /api/sim/scenario/{nom}  → changer le scénario
 *   POST /api/sim/start           → démarrer
 *   POST /api/sim/stop            → arrêter
 *   GET  /api/sim/status          → état courant
 */
@RestController
@RequestMapping("/api/sim")
@RequiredArgsConstructor
@Tag(name = "Simulateur", description = "Contrôle du simulateur FreeStyle Libre 3")
public class SimulatorController {

    private final FreeStyleLibreSimulator simulator;

    @PostMapping("/scenario/{name}")
    @Operation(summary = "Activer un scénario",
               description = "NORMAL | HYPOGLYCEMIE | HYPERGLYCEMIE | VARIATION_NUIT")
    public ResponseEntity<Map<String, String>> setScenario(@PathVariable String name) {
        try {
            var scenario = FreeStyleLibreSimulator.Scenario.valueOf(name.toUpperCase());
            simulator.setScenario(scenario);
            return ResponseEntity.ok(Map.of(
                "scenario", scenario.name(),
                "status",   "activé"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "error",    "Scénario inconnu : " + name,
                "valides",  "NORMAL, HYPOGLYCEMIE, HYPERGLYCEMIE, VARIATION_NUIT"
            ));
        }
    }

    @PostMapping("/start")
    @Operation(summary = "Démarrer le simulateur")
    public ResponseEntity<Map<String, String>> start() {
        simulator.setRunning(true);
        return ResponseEntity.ok(Map.of("status", "démarré"));
    }

    @PostMapping("/stop")
    @Operation(summary = "Arrêter le simulateur")
    public ResponseEntity<Map<String, String>> stop() {
        simulator.setRunning(false);
        return ResponseEntity.ok(Map.of("status", "arrêté"));
    }

    @GetMapping("/status")
    @Operation(summary = "État courant du simulateur")
    public ResponseEntity<FreeStyleLibreSimulator.SimulatorStatus> status() {
        return ResponseEntity.ok(simulator.getStatus());
    }
}
