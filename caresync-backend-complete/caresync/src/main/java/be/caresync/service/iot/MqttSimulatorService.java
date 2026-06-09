package be.caresync.service.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simulateur MQTT — remplace les capteurs physiques en profil "demo" ou "dev".
 * Publie des mesures réalistes sur les topics MQTT toutes les 2 secondes.
 *
 * Activation : spring.profiles.active=demo
 * Contrôle   : POST /sim/scenario/{nom}
 */
@Service
@Profile({"demo", "dev"})
@RequiredArgsConstructor
@Slf4j
public class MqttSimulatorService {

    @Value("${caresync.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${caresync.mqtt.username:caresync}")
    private String username;

    @Value("${caresync.mqtt.password:mqtt_secret}")
    private String password;

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    // État courant du patient simulé
    private double  glucose  = 1.1;
    private int     spo2     = 97;
    private int     hr       = 72;
    private double  weight   = 82.0;

    public enum Scenario { NORMAL, HYPOGLYCEMIE, CHUTE_SPO2, RETENTION_HYDRIQUE }

    private final AtomicReference<Scenario> scenario = new AtomicReference<>(Scenario.NORMAL);

    // Serial numbers des dispositifs enregistrés en base (à adapter)
    private static final String GLUCOMETER_SERIAL  = "GLU-DEMO-001";
    private static final String OXIMETER_SERIAL    = "OXY-DEMO-001";
    private static final String HEART_RATE_SERIAL  = "HR-DEMO-001";
    private static final String SCALE_SERIAL       = "BAL-DEMO-001";

    // ── Publication toutes les 2s ─────────────────────────────────────────────

    @Scheduled(fixedDelay = 2000)
    public void simulateTick() {
        evolveVitals();
        try (MqttClient client = createClient()) {
            publishGlucose(client);
            publishSpo2(client);
            publishHeartRate(client);
            publishWeight(client);
        } catch (MqttException ex) {
            log.error("Erreur publication MQTT simulateur : {}", ex.getMessage());
        }
    }

    // ── Évolution des vitaux selon le scénario ────────────────────────────────

    private void evolveVitals() {
        switch (scenario.get()) {
            case HYPOGLYCEMIE -> {
                glucose = Math.max(0.3, glucose - (0.08 + random.nextDouble() * 0.1));
                hr      = Math.min(140, hr + 1 + random.nextInt(3));
            }
            case CHUTE_SPO2 -> {
                spo2 = Math.max(78, spo2 - (int)(1 + random.nextDouble() * 2));
                hr   = Math.min(140, hr + 1);
            }
            case RETENTION_HYDRIQUE -> {
                weight += 0.15 + random.nextDouble() * 0.2;
                spo2    = Math.max(88, spo2 - (random.nextDouble() > 0.7 ? 1 : 0));
            }
            default -> {
                glucose = clamp(1.1 + (random.nextDouble() - 0.5) * 0.3, 0.7, 2.2);
                spo2    = clampInt(97 + (int)((random.nextDouble() - 0.5) * 4), 94, 100);
                hr      = clampInt(72  + (int)((random.nextDouble() - 0.5) * 8), 58, 95);
                weight  = 82.0 + (random.nextDouble() - 0.5) * 0.2;
            }
        }
    }

    // ── Publications MQTT ─────────────────────────────────────────────────────

    private void publishGlucose(MqttClient client) throws MqttException {
        publish(client, "caresync/devices/" + GLUCOMETER_SERIAL + "/glucose",
                Map.of("serial", GLUCOMETER_SERIAL,
                        "value", Math.round(glucose * 10.0) / 10.0,
                        "unit",  "g/L",
                        "ts",    Instant.now().toEpochMilli()));
    }

    private void publishSpo2(MqttClient client) throws MqttException {
        publish(client, "caresync/devices/" + OXIMETER_SERIAL + "/spo2",
                Map.of("serial", OXIMETER_SERIAL,
                        "value", spo2,
                        "unit",  "%",
                        "ts",    Instant.now().toEpochMilli()));
    }

    private void publishHeartRate(MqttClient client) throws MqttException {
        publish(client, "caresync/devices/" + HEART_RATE_SERIAL + "/heartrate",
                Map.of("serial", HEART_RATE_SERIAL,
                        "value", hr,
                        "unit",  "bpm",
                        "ts",    Instant.now().toEpochMilli()));
    }

    private void publishWeight(MqttClient client) throws MqttException {
        publish(client, "caresync/devices/" + SCALE_SERIAL + "/weight",
                Map.of("serial", SCALE_SERIAL,
                        "value", Math.round(weight * 10.0) / 10.0,
                        "unit",  "kg",
                        "ts",    Instant.now().toEpochMilli()));
    }

    private void publish(MqttClient client, String topic, Map<String, Object> payload)
            throws MqttException {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            client.publish(topic, new MqttMessage(json) {{ setQos(1); }});
            log.debug("MQTT pub → {} : {}", topic, payload.get("value"));
        } catch (Exception ex) {
            log.error("Sérialisation payload MQTT : {}", ex.getMessage());
        }
    }

    private MqttClient createClient() throws MqttException {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(username);
        opts.setPassword(password.toCharArray());
        opts.setCleanSession(true);
        MqttClient client = new MqttClient(brokerUrl,
                "caresync-simulator-" + System.nanoTime());
        client.connect(opts);
        return client;
    }

    // ── API de contrôle ───────────────────────────────────────────────────────

    public void setScenario(Scenario sc) {
        this.scenario.set(sc);
        log.info("Simulateur — scénario activé : {}", sc);
        if (sc == Scenario.NORMAL) {
            glucose = 1.1; spo2 = 97; hr = 72; weight = 82.0;
        }
    }

    public Scenario getScenario() { return scenario.get(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double clamp(double v, double lo, double hi)    { return Math.max(lo, Math.min(hi, v)); }
    private int    clampInt(int v, int lo, int hi)          { return Math.max(lo, Math.min(hi, v)); }
}
