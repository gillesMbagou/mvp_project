package be.caresync.controller.iot;

import be.caresync.service.iot.MqttSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur de simulation — disponible uniquement en profil "demo" ou "dev".
 * Permet de déclencher des scénarios cliniques sans capteur physique.
 */
@RestController
@RequestMapping("/sim")
@RequiredArgsConstructor
@Profile({"demo", "dev"})
@Tag(name = "Simulateur IoT", description = "Contrôle du simulateur de capteurs (dev/demo uniquement)")
public class SimulatorController {

    private final MqttSimulatorService simulator;

    @PostMapping("/scenario/{name}")
    @Operation(summary = "Activer un scénario de simulation",
               description = "Valeurs : NORMAL, HYPOGLYCEMIE, CHUTE_SPO2, RETENTION_HYDRIQUE")
    public ResponseEntity<Map<String, String>> setScenario(@PathVariable String name) {
        MqttSimulatorService.Scenario sc =
                MqttSimulatorService.Scenario.valueOf(name.toUpperCase());
        simulator.setScenario(sc);
        return ResponseEntity.ok(Map.of(
                "scenario", sc.name(),
                "status",   "activé"
        ));
    }

    @GetMapping("/scenario")
    @Operation(summary = "Scénario actif")
    public ResponseEntity<Map<String, String>> getScenario() {
        return ResponseEntity.ok(Map.of("scenario", simulator.getScenario().name()));
    }
}
