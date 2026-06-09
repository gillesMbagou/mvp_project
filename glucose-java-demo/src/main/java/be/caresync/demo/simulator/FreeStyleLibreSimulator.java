package be.caresync.demo.simulator;

import be.caresync.demo.model.GlucoseObservation;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simulateur FreeStyle Libre 3 en Java pur.
 *
 * Remplace intégralement simulator.py.
 * Publie des mesures réalistes sur MQTT toutes les N secondes.
 *
 * Contrôle via REST :
 *   POST /api/sim/scenario/HYPOGLYCEMIE
 *   POST /api/sim/scenario/NORMAL
 *   GET  /api/sim/status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FreeStyleLibreSimulator {

    private final ObjectMapper objectMapper;

    @Value("${demo.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    private static final String SERIAL      = "LIB3-SIM-001";
    private static final String DEVICE_NAME = "FreeStyle-Libre-3-SIM";
    private static final String MQTT_TOPIC  = "caresync/devices/LIB3-SIM-001/glucose";
    private static final String LOINC_CODE  = "14743-9";   // Glucose interstitiel CGM

    private final Random        random      = new Random();
    private final AtomicInteger sequence    = new AtomicInteger(0);
    private final AtomicInteger totalSent   = new AtomicInteger(0);

    // État courant du simulateur
    private volatile double     glucose     = 1.1;
    private volatile Scenario   scenario    = Scenario.NORMAL;
    private volatile boolean    running     = true;

    private MqttClient mqttClient;

    // Scénarios
    public enum Scenario {
        NORMAL          (1.1,  0.08,  0.0),
        HYPOGLYCEMIE    (1.1,  0.05, -0.04),   // descend 0.04 g/L par tick
        HYPERGLYCEMIE   (1.2,  0.10, +0.08),   // monte 0.08 g/L par tick
        VARIATION_NUIT  (1.0,  0.15,  0.0);

        final double base;
        final double noise;
        final double drift;

        Scenario(double base, double noise, double drift) {
            this.base  = base;
            this.noise = noise;
            this.drift = drift;
        }
    }

    // Connexion MQTT à l'init

    @jakarta.annotation.PostConstruct
    public void initMqtt() {
        try {
            mqttClient = new MqttClient(brokerUrl,
                    "caresync-simulator-" + System.nanoTime());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);

            mqttClient.connect(opts);
            log.info("Simulateur FreeStyle Libre 3 connecté à MQTT ({})", brokerUrl);

        } catch (MqttException ex) {
            log.error("Connexion MQTT simulateur échouée : {}", ex.getMessage());
        }
    }

    // ── Tick toutes les 5 secondes (configurable via application.yml) ─────────

    @Scheduled(fixedDelayString = "${demo.simulator.interval-ms:5000}")
    public void tick() {
        if (!running || mqttClient == null || !mqttClient.isConnected()) return;

        evolveGlucose();
        publishMqtt();
    }

    // ── Évolution de la glycémie selon le scénario ────────────────────────────

    private void evolveGlucose() {
        double noise = random.nextGaussian() * scenario.noise;
        glucose += scenario.drift + noise;
        // Bornes physiologiques
        glucose = Math.max(0.25, Math.min(5.5, glucose));
    }

    // ── Publication MQTT ──────────────────────────────────────────────────────

    private void publishMqtt() {
        try {
            GlucoseObservation obs = buildObservation();
            byte[] payload = objectMapper.writeValueAsBytes(obs);

            MqttMessage msg = new MqttMessage(payload);
            msg.setQos(1);
            mqttClient.publish(MQTT_TOPIC, msg);

            totalSent.incrementAndGet();

            String icon = obs.severityIcon();
            log.info("{} [#{}] Glucose = {} g/L | {} | Scénario: {}",
                    icon,
                    String.format("%04d", obs.getSequence()),
                    String.format("%.2f", obs.getValue()),
                    obs.computeSeverity(),
                    scenario.name());

            if (!obs.computeSeverity().equals("NORMAL")) {
                log.warn("   ⚠ Alerte attendue dans GlucoseMonitor — {} g/L",
                        String.format("%.2f", obs.getValue()));
            }

        } catch (Exception ex) {
            log.error("Erreur publication MQTT simulateur : {}", ex.getMessage());
        }
    }

    private GlucoseObservation buildObservation() {
        Instant now = Instant.now();
        double  val = Math.round(glucose * 100.0) / 100.0;

        return GlucoseObservation.builder()
                .serial(SERIAL)
                .deviceName(DEVICE_NAME)
                .manufacturer("Abbott")
                .model("FreeStyle Libre 3")
                .loincCode(LOINC_CODE)
                .value(val)
                .unit("g/L")
                .sequence(sequence.incrementAndGet())
                .severity(computeSeverity(val))
                .scenario(scenario.name().toLowerCase())
                .source("CGM_SIMULATOR")
                .observedAt(now)
                .ts(now.toEpochMilli())
                .firmware("3.1.0-sim")
                .batteryPct(Math.max(20, 100 - totalSent.get() / 10))
                .build();
    }

    // ── Seuils cliniques ──────────────────────────────────────────────────────

    private String computeSeverity(double value) {
        if (value < 0.60 || value > 4.00) return "CRITIQUE";
        if (value < 0.80 || value > 2.50) return "URGENTE";
        if (value < 1.00 || value > 1.80) return "INFORMATIVE";
        return "NORMAL";
    }

    // ── API de contrôle (appelée par SimulatorController) ────────────────────

    public void setScenario(Scenario sc) {
        this.scenario = sc;
        if (sc == Scenario.NORMAL) {
            this.glucose = sc.base;
        }
        log.info("Simulateur — scénario activé : {}", sc);
    }

    public void setRunning(boolean running) { this.running = running; }

    public SimulatorStatus getStatus() {
        return new SimulatorStatus(
                running, scenario.name(),
                Math.round(glucose * 100.0) / 100.0,
                computeSeverity(glucose),
                sequence.get(), totalSent.get()
        );
    }

    public record SimulatorStatus(
        boolean running,
        String  scenario,
        double  lastGlucose,
        String  lastSeverity,
        int     sequence,
        int     totalSent
    ) {}
}
