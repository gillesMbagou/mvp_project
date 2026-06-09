package be.caresync.demo.libreview;

import be.caresync.demo.model.GlucoseObservation;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client LibreView API (LibreLinkUp Abbott) — Java pur.
 * Remplace intégralement libreview_client.py.
 *
 * API Abbott (EU) utilisée par LibreLinkUp :
 *   POST /llu/auth/login      → token JWT
 *   GET  /llu/connections     → liste patients connectés
 *   GET  /llu/connections/{id}/graph → mesures glucose
 *
 * Prérequis :
 *   1. Compte LibreView sur libreview.io
 *   2. Patient active le partage LibreLinkUp dans l'app FreeStyle LibreLink
 *   3. Renseigner email/password dans application.yml
 *
 * Activation : demo.libreview.enabled=true dans application.yml
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LibreViewClient {

    private final ObjectMapper objectMapper;

    @Value("${demo.libreview.enabled:false}")
    private boolean enabled;

    @Value("${demo.libreview.email:}")
    private String  email;

    @Value("${demo.libreview.password:}")
    private String  password;

    @Value("${demo.mqtt.broker-url:tcp://localhost:1883}")
    private String  brokerUrl;

    private static final String LIBRE_BASE  = "https://api.libreview.io";
    private static final String MQTT_TOPIC  = "caresync/devices/{serial}/glucose";
    private static final String LOINC_CODE  = "14743-9";

    private final AtomicReference<String> token     = new AtomicReference<>();
    private final AtomicReference<String> patientId = new AtomicReference<>();
    private MqttClient mqttClient;
    private WebClient  webClient;

    // ── Initialisation ────────────────────────────────────────────────────────

    @jakarta.annotation.PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(LIBRE_BASE)
                .defaultHeader("Content-Type",    "application/json")
                .defaultHeader("Product",         "llu.ios")
                .defaultHeader("Version",         "4.12.0")
                .defaultHeader("User-Agent",
                        "LibreLinkUp/4.12.0 CFNetwork/1494.0.7 Darwin/23.4.0")
                .build();

        if (!enabled) {
            log.info("LibreView client désactivé (demo.libreview.enabled=false)");
            return;
        }
        try {
            mqttClient = new MqttClient(brokerUrl,
                    "caresync-libreview-" + System.nanoTime());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            mqttClient.connect(opts);

            if (login()) {
                loadFirstConnection();
                log.info("LibreView client initialisé — patient={}", patientId.get());
            }
        } catch (Exception ex) {
            log.error("Initialisation LibreView échouée : {}", ex.getMessage());
        }
    }

    // ── Polling toutes les 60s ────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${demo.libreview.poll-interval-ms:60000}")
    public void pollLatestReading() {
        if (!enabled || patientId.get() == null) return;

        try {
            GlucoseObservation obs = fetchLatestReading();
            if (obs != null) {
                publishToMqtt(obs);
                log.info("LibreView → MQTT {} g/L (trend={})",
                        obs.getValue(), "→");
            }
        } catch (Exception ex) {
            // Token peut expirer — retenter le login
            log.warn("Erreur polling LibreView, tentative de re-login : {}", ex.getMessage());
            login();
        }
    }

    // ── Authentification ──────────────────────────────────────────────────────

    private boolean login() {
        try {
            log.info("Authentification LibreView avec {}...", email);

            JsonNode response = webClient.post()
                    .uri("/llu/auth/login")
                    .bodyValue("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return false;

            // Gestion du redirect régional Abbott (EU vs US)
            if (response.path("status").asInt() == 2) {
                String redirect = response.path("data").path("redirect").asString();
                log.info("Redirect Abbott → {}", redirect);
                // En prod : recréer le WebClient avec la nouvelle URL
            }

            String jwt = response
                    .path("data")
                    .path("authTicket")
                    .path("token")
                    .asString();

            token.set(jwt);
            log.info("Authentification LibreView réussie ✓");
            return true;

        } catch (Exception ex) {
            log.error("Authentification LibreView échouée : {}", ex.getMessage());
            return false;
        }
    }

    // ── Récupération du premier patient connecté ──────────────────────────────

    private void loadFirstConnection() {
        JsonNode connections = webClient.get()
                .uri("/llu/connections")
                .header("Authorization", "Bearer " + token.get())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (connections != null && connections.path("data").isArray()
                && connections.path("data").size() > 0) {
            JsonNode first = connections.path("data").get(0);
            patientId.set(first.path("patientId").asString());
            log.info("Patient connecté : {} {}",
                    first.path("firstName").asString(),
                    first.path("lastName").asString());
        } else {
            log.warn("Aucun patient connecté dans LibreView. " +
                     "Activez le partage LibreLinkUp dans l'app FreeStyle LibreLink.");
        }
    }

    // ── Lecture de la glycémie courante ───────────────────────────────────────

    private GlucoseObservation fetchLatestReading() {
        JsonNode response = webClient.get()
                .uri("/llu/connections/{id}/graph", patientId.get())
                .header("Authorization", "Bearer " + token.get())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) return null;

        JsonNode current = response
                .path("data")
                .path("connection")
                .path("glucoseMeasurement");

        if (current.isMissingNode()) return null;

        // Abbott renvoie en mg/dL → conversion en g/L
        double valueMgDl = current.path("Value").asDouble();
        double valueGperL = Math.round((valueMgDl / 10.0) * 100.0) / 100.0;

        String serial = response
                .path("data")
                .path("activeSensors")
                .path(0)
                .path("sensor")
                .path("deviceId")
                .asString("LIB3-REAL-001");

        Instant now = Instant.now();
        return GlucoseObservation.builder()
                .serial(serial)
                .deviceName("FreeStyle Libre 3")
                .manufacturer("Abbott")
                .model("FreeStyle Libre 3")
                .loincCode(LOINC_CODE)
                .value(valueGperL)
                .unit("g/L")
                .severity(computeSeverity(valueGperL))
                .source("LIBREVIEW_API")
                .observedAt(now)
                .ts(now.toEpochMilli())
                .build();
    }

    // ── Publication MQTT (utilise l'ObjectMapper injecté avec JavaTimeModule) ─

    private void publishToMqtt(GlucoseObservation obs) throws Exception {
        String topic = MQTT_TOPIC.replace("{serial}", obs.getSerial());
        byte[] payload = objectMapper.writeValueAsBytes(obs);
        MqttMessage msg = new MqttMessage(payload);
        msg.setQos(1);
        mqttClient.publish(topic, msg);
    }

    private String computeSeverity(double value) {
        if (value < 0.60 || value > 4.00) return "CRITIQUE";
        if (value < 0.80 || value > 2.50) return "URGENTE";
        return "NORMAL";
    }
}
