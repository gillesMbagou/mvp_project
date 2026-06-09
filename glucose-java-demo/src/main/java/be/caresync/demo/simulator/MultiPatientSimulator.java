package be.caresync.demo.simulator;

import be.caresync.demo.model.GlucoseObservation;
import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.model.db.PatientProfile;
import be.caresync.demo.model.db.SimulatedDevice;
import be.caresync.demo.repository.DeviceRepository;
import be.caresync.demo.repository.ObservationRepository;
import be.caresync.demo.repository.PatientRepository;
import be.caresync.demo.streaming.ReactiveEventBus;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulateur multi-patients et multi-dispositifs.
 *
 * Lit la configuration des patients et dispositifs depuis la base H2
 * (initialisée par DataSeeder) et publie des mesures temps réel sur MQTT.
 *
 * Chaque dispositif maintient son propre état (glucose courant, etc.)
 * avec le rythme circadien et les scénarios pathologiques.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiPatientSimulator {

    private final PatientRepository     patientRepo;
    private final DeviceRepository      deviceRepo;
    private final ObservationRepository obsRepo;
    private final ObjectMapper          objectMapper;
    private final ReactiveEventBus      eventBus;

    @Value("${demo.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    private MqttClient mqttClient;
    private final Random rng = new Random();

    // État courant par dispositif (valeur de glucose, SpO2, etc.)
    private final Map<String, Double> deviceState = new ConcurrentHashMap<>();

    // ── Connexion MQTT ─────────────────────────────────────────────────────────
    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(brokerUrl,
                    "caresync-multi-sim-" + System.nanoTime());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            mqttClient.connect(opts);
            log.info("MultiPatientSimulator connecté à MQTT ({})", brokerUrl);
        } catch (MqttException ex) {
            log.error("Connexion MQTT simulateur échouée : {}", ex.getMessage());
        }
    }

    // ── Tick principal toutes les 5 secondes ──────────────────────────────────
    @Scheduled(fixedDelayString = "${demo.simulator.interval-ms:5000}")
    public void tick() {
        if (mqttClient == null || !mqttClient.isConnected()) return;

        List<SimulatedDevice> activeDevices = deviceRepo.findByActiveTrue();
        Instant now = Instant.now();
        LocalDateTime ldt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        for (SimulatedDevice dev : activeDevices) {
            PatientProfile patient = patientRepo.findById(dev.getPatientId())
                    .orElse(null);
            if (patient == null) continue;

            // Calculer la valeur selon le type de dispositif
            double value = computeValue(dev, patient, ldt);
            String severity = computeSeverity(dev.getDeviceType(), value);

            // Persister en base
            ObservationRecord rec = ObservationRecord.builder()
                    .patientId(dev.getPatientId())
                    .deviceSerial(dev.getSerial())
                    .deviceType(dev.getDeviceType())
                    .loincCode(dev.getLoincCode())
                    .value(Math.round(value * 100.0) / 100.0)
                    .unit(dev.getUnit())
                    .severity(severity)
                    .source("REALTIME_SIMULATOR")
                    .observedAt(now)
                    .build();
            obsRepo.save(rec);

            // Diffuser aux clients SSE
            eventBus.publish(rec);

            // Publier sur MQTT
            publishMqtt(dev, patient, rec, now);
        }
    }

    // ── Calcul de la valeur selon le type de dispositif ───────────────────────

    private double computeValue(SimulatedDevice dev, PatientProfile p,
                                 LocalDateTime ldt) {
        String key = dev.getSerial();
        double current = deviceState.getOrDefault(key, getBaseline(dev, p));

        double next = switch (dev.getDeviceType()) {
            case "GLUCOMETER"     -> evolveGlucose(current, p, ldt, dev.getCurrentScenario());
            case "PULSE_OXIMETER" -> evolveSpo2(current, p, ldt, dev.getCurrentScenario());
            case "BP_MONITOR"     -> evolveBP(current, p, ldt);
            case "SCALE"          -> evolveWeight(current, p);
            case "SPIROMETER"     -> evolveFev1(current, p);
            default               -> current;
        };

        deviceState.put(key, next);
        return next;
    }

    // ── Modèles d'évolution ───────────────────────────────────────────────────

    private double evolveGlucose(double current, PatientProfile p,
                                  LocalDateTime ldt, String scenario) {
        int hour = ldt.getHour();
        double drift = switch (scenario) {
            case "HYPOGLYCEMIE"  -> -0.04;
            case "HYPERGLYCEMIE" -> +0.06;
            default              -> circadianGlucoseDrift(hour, current, p);
        };
        double next = current + drift + rng.nextGaussian() * 0.05;
        return Math.max(0.3, Math.min(5.5, next));
    }

    private double circadianGlucoseDrift(int hour, double current,
                                           PatientProfile p) {
        double target = switch (hour) {
            case 7, 8   -> p.getBaseGlucose() + 1.0;  // pic petit-déjeuner
            case 12, 13 -> p.getBaseGlucose() + 1.1;  // pic déjeuner
            case 19, 20 -> p.getBaseGlucose() + 0.9;  // pic dîner
            case 2, 3   -> p.getBaseGlucose() - 0.15; // nadir nocturne
            default     -> p.getBaseGlucose();
        };
        // Retour progressif vers la cible
        return (target - current) * 0.1;
    }

    private double evolveSpo2(double current, PatientProfile p,
                               LocalDateTime ldt, String scenario) {
        int hour = ldt.getHour();
        if ("DECOMPENSATION".equals(scenario)) {
            double next = current - 0.3 + rng.nextGaussian() * 0.5;
            return Math.max(75.0, next);
        }
        double target = (hour >= 22 || hour < 6)
                ? p.getBaseSpO2() - 1.5
                : p.getBaseSpO2();
        double next = current + (target - current) * 0.15
                + rng.nextGaussian() * 0.4;
        return Math.max(75.0, Math.min(100.0, next));
    }

    private double evolveBP(double current, PatientProfile p,
                             LocalDateTime ldt) {
        int hour = ldt.getHour();
        double target = (hour < 10 || hour > 18)
                ? p.getBaseSystolic() + 8
                : p.getBaseSystolic();
        return current + (target - current) * 0.2
                + rng.nextGaussian() * 4.0;
    }

    private double evolveWeight(double current, PatientProfile p) {
        // Poids très stable (variation ±0.1 kg autour de la base)
        return p.getWeightKg() + rng.nextGaussian() * 0.1;
    }

    private double evolveFev1(double current, PatientProfile p) {
        // VEMS stable avec légère variabilité
        return Math.max(0.5, current + rng.nextGaussian() * 0.04);
    }

    private double getBaseline(SimulatedDevice dev, PatientProfile p) {
        return switch (dev.getDeviceType()) {
            case "GLUCOMETER"     -> p.getBaseGlucose();
            case "PULSE_OXIMETER" -> p.getBaseSpO2();
            case "BP_MONITOR"     -> p.getBaseSystolic();
            case "SCALE"          -> p.getWeightKg();
            case "SPIROMETER"     -> 1.0;
            default               -> 0.0;
        };
    }

    // ── Publication MQTT ──────────────────────────────────────────────────────

    private void publishMqtt(SimulatedDevice dev, PatientProfile patient,
                              ObservationRecord rec, Instant now) {
        try {
            String topic = "caresync/devices/%s/%s"
                    .formatted(dev.getSerial(), dev.getMqttTopicSuffix());

            GlucoseObservation payload = GlucoseObservation.builder()
                    .serial(dev.getSerial())
                    .deviceName(dev.getModel())
                    .manufacturer(dev.getManufacturer())
                    .model(dev.getModel())
                    .loincCode(dev.getLoincCode())
                    .value(rec.getValue())
                    .unit(dev.getUnit())
                    .severity(rec.getSeverity())
                    .scenario(dev.getCurrentScenario().toLowerCase())
                    .source("REALTIME_SIMULATOR")
                    .observedAt(now)
                    .ts(now.toEpochMilli())
                    .build();

            MqttMessage msg = new MqttMessage(
                    objectMapper.writeValueAsBytes(payload));
            msg.setQos(1);
            mqttClient.publish(topic, msg);

            // Log coloré
            String icon = switch (rec.getSeverity()) {
                case "CRITIQUE"    -> "🔴";
                case "URGENTE"     -> "🟠";
                case "INFORMATIVE" -> "🔵";
                default            -> "🟢";
            };

            log.info("{} {} [{}] {} {} {} → {}",
                    icon,
                    patient.fullName(),
                    dev.getDeviceType(),
                    rec.getValue(),
                    dev.getUnit(),
                    rec.getSeverity(),
                    topic);

        } catch (Exception ex) {
            log.error("Erreur publication MQTT {} : {}", dev.getSerial(),
                    ex.getMessage());
        }
    }

    // ── Évaluation clinique ───────────────────────────────────────────────────

    private String computeSeverity(String deviceType, double value) {
        return switch (deviceType) {
            case "GLUCOMETER" -> {
                if (value < 0.60 || value > 4.00) yield "CRITIQUE";
                if (value < 0.80 || value > 2.50) yield "URGENTE";
                if (value < 1.00 || value > 1.80) yield "INFORMATIVE";
                yield "NORMAL";
            }
            case "PULSE_OXIMETER" -> {
                if (value < 85.0) yield "CRITIQUE";
                if (value < 90.0) yield "URGENTE";
                if (value < 94.0) yield "INFORMATIVE";
                yield "NORMAL";
            }
            case "SCALE"      -> value > 84.0 ? "URGENTE" : "NORMAL";
            case "BP_MONITOR" -> {
                if (value > 180.0) yield "CRITIQUE";
                if (value > 160.0) yield "URGENTE";
                if (value > 140.0) yield "INFORMATIVE";
                yield "NORMAL";
            }
            case "SPIROMETER" -> value < 0.6 ? "CRITIQUE" :
                                  value < 0.8 ? "URGENTE" : "NORMAL";
            default           -> "NORMAL";
        };
    }

    // ── Contrôle via REST ─────────────────────────────────────────────────────

    public void setScenario(String deviceSerial, String scenario) {
        deviceRepo.findById(deviceSerial).ifPresent(dev -> {
            dev.setCurrentScenario(scenario);
            deviceRepo.save(dev);
            log.info("Dispositif {} → scénario {}", deviceSerial, scenario);
        });
    }

    public List<SimulatedDevice> getAllDevices() {
        return deviceRepo.findAll();
    }
}
