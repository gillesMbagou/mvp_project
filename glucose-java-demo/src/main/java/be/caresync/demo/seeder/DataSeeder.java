package be.caresync.demo.seeder;

import be.caresync.demo.model.db.*;
import be.caresync.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * DataSeeder — génère 7 jours de données historiques réalistes
 * pour 3 patients (Diabète T2 / ICFe / BPCO) au démarrage.
 *
 * Caractéristiques :
 *   - Rythme circadien (glycémie monte après repas, SpO2 chute la nuit)
 *   - Événements pathologiques (hypoglycémie nocturne, décompensation ICFe)
 *   - Variabilité inter-journalière (weekends vs jours ouvrés)
 *   - Publications en batch vers Kafka (optionnel)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PatientRepository     patientRepo;
    private final DeviceRepository      deviceRepo;
    private final ObservationRepository obsRepo;

    private final Random rng = new Random(42); // Seed fixe = reproductible

    // ── Durée de l'historique ─────────────────────────────────────────────────
    private static final int HISTORY_DAYS = 7;

    @Override
    public void run(String... args) {
        if (patientRepo.count() > 0) {
            log.info("Base déjà initialisée ({} patients, {} observations)",
                    patientRepo.count(), obsRepo.count());
            return;
        }

        log.info("=== Initialisation base de données CareSync ===");
        long start = System.currentTimeMillis();

        // 1. Créer les patients
        List<PatientProfile> patients = createPatients();

        // 2. Créer les dispositifs
        List<SimulatedDevice> devices = createDevices(patients);

        // 3. Générer l'historique
        generateHistory(patients, devices);

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== Base initialisée en {}ms : {} patients | {} dispositifs | {} observations ===",
                elapsed, patientRepo.count(), deviceRepo.count(), obsRepo.count());
    }

    // ── 1. Patients ───────────────────────────────────────────────────────────

    private List<PatientProfile> createPatients() {
        List<PatientProfile> patients = List.of(
                PatientProfile.builder()
                        .id("patient-001")
                        .firstName("Marie-Christine").lastName("Decleves")
                        .age(58).gender("F").pathology("ICFE")
                        .weightKg(74.5).heightCm(163.0)
                        .baseGlucose(1.25)       // glycémie à jeun légèrement élevée
                        .baseSpO2(97.5)
                        .baseSystolic(138.0).baseDiastolic(85.0)
                        .build(),

                PatientProfile.builder()
                        .id("patient-002")
                        .firstName("Jean-Marie").lastName("Lécrivain")
                        .age(72).gender("M").pathology("DIABETE_T2")
                        .weightKg(82.0).heightCm(175.0)
                        .baseGlucose(1.05)
                        .baseSpO2(94.0)          // SpO2 plus basse (ICFe)
                        .baseSystolic(145.0).baseDiastolic(90.0)
                        .build(),

                PatientProfile.builder()
                        .id("patient-003")
                        .firstName("Gilles").lastName("Mbagou")
                        .age(46).gender("M").pathology("BPCO")
                        .weightKg(78.0).heightCm(170.0)
                        .baseGlucose(1.02)
                        .baseSpO2(91.0)          // SpO2 basse (BPCO stade 3)
                        .baseSystolic(132.0).baseDiastolic(80.0)
                        .build(),
                PatientProfile.builder()
                        .id("patient-004")
                        .firstName("Thomas").lastName("Moukanga")
                        .age(70).gender("M").pathology("BPCO")
                        .weightKg(95.0).heightCm(180.0)
                        .baseGlucose(1.02)
                        .baseSpO2(91.0)          // SpO2 basse (BPCO stade 3)
                        .baseSystolic(132.0).baseDiastolic(80.0)
                        .build()
        );

        patientRepo.saveAll(patients);
        log.info("  {} patients créés", patients.size());
        return patients;
    }

    // ── 2. Dispositifs ────────────────────────────────────────────────────────

    private List<SimulatedDevice> createDevices(List<PatientProfile> patients) {
        List<SimulatedDevice> devices = new ArrayList<>();

        // ── Patient 1 : Diabète T2 — FreeStyle Libre 3 + tensiomètre
        devices.add(device("LIB3-P001-001", patients.get(0).getId(),
                "GLUCOMETER", "Abbott", "FreeStyle Libre 3",
                "14743-9", "g/L", "glucose", 300));  // toutes les 5 min

        devices.add(device("BP-P001-001", patients.get(0).getId(),
                "BP_MONITOR", "Withings", "BPM Connect",
                "55284-4", "mmHg", "bloodpressure", 43200)); // 2x/jour

        // ── Patient 2 : ICFe — SpO2 + balance + tensiomètre
        devices.add(device("SPO2-P002-001", patients.get(1).getId(),
                "PULSE_OXIMETER", "Nonin", "GO2",
                "59408-5", "%", "spo2", 600));        // toutes les 10 min

        devices.add(device("SCALE-P002-001", patients.get(1).getId(),
                "SCALE", "Withings", "Body+",
                "29463-7", "kg", "weight", 86400));   // 1x/jour

        devices.add(device("BP-P002-001", patients.get(1).getId(),
                "BP_MONITOR", "Omron", "HeartGuide",
                "55284-4", "mmHg", "bloodpressure", 43200));

        // ── Patient 3 : BPCO — SpO2 + spiromètre
        devices.add(device("SPO2-P003-001", patients.get(2).getId(),
                "PULSE_OXIMETER", "Masimo", "MightySat",
                "59408-5", "%", "spo2", 300));

        devices.add(device("SPIRO-P003-001", patients.get(2).getId(),
                "SPIROMETER", "NuvoAir", "Air Next",
                "19896-4", "L/s", "spirometry", 86400));

        // ── Patient 4 : BPCO — SpO2 + tensiomètre
        devices.add(device("SPO2-P004-001", patients.get(3).getId(),
                "PULSE_OXIMETER", "Nonin", "GO2",
                "59408-5", "%", "spo2", 600));

        devices.add(device("BP-P004-001", patients.get(3).getId(),
                "BP_MONITOR", "Omron", "M7",
                "55284-4", "mmHg", "bloodpressure", 43200));

        deviceRepo.saveAll(devices);
        log.info("  {} dispositifs créés", devices.size());
        return devices;
    }

    private SimulatedDevice device(String serial, String patientId,
                                   String type, String maker, String model,
                                   String loinc, String unit,
                                   String topicSuffix, int intervalSec) {
        return SimulatedDevice.builder()
                .serial(serial).patientId(patientId)
                .deviceType(type).manufacturer(maker).model(model)
                .loincCode(loinc).unit(unit)
                .mqttTopicSuffix(topicSuffix)
                .intervalSeconds(intervalSec)
                .currentScenario("NORMAL")
                .active(true)
                .build();
    }

    // ── 3. Génération de l'historique ─────────────────────────────────────────

    private void generateHistory(List<PatientProfile> patients,
                                 List<SimulatedDevice> devices) {
        Instant now   = Instant.now();
        Instant start = now.minus(HISTORY_DAYS, ChronoUnit.DAYS);

        List<ObservationRecord> batch = new ArrayList<>();

        for (SimulatedDevice dev : devices) {
            PatientProfile patient = patients.stream()
                    .filter(p -> p.getId().equals(dev.getPatientId()))
                    .findFirst().orElseThrow();

            Instant cursor = start;
            long obsCount  = 0;

            while (cursor.isBefore(now)) {
                LocalDateTime ldt = LocalDateTime.ofInstant(cursor, ZoneOffset.UTC);
                int dayOfHistory = (int) ChronoUnit.DAYS.between(
                        start.atOffset(ZoneOffset.UTC).toLocalDate(),
                        ldt.toLocalDate());

                // Générer une observation selon le type de dispositif
                ObservationRecord obs = generateObservation(
                        dev, patient, cursor, ldt, dayOfHistory);

                if (obs != null) {
                    batch.add(obs);
                    obsCount++;
                }

                cursor = cursor.plusSeconds(dev.getIntervalSeconds());

                if (batch.size() >= 500) {
                    obsRepo.saveAll(batch);
                    batch.clear();
                }
            }

            log.info("  {} : {} observations générées", dev.getSerial(), obsCount);
        }

        if (!batch.isEmpty()) obsRepo.saveAll(batch);
    }

    // ── Génération d'une observation selon le type ────────────────────────────

    private ObservationRecord generateObservation(SimulatedDevice dev,
                                                  PatientProfile patient,
                                                  Instant ts,
                                                  LocalDateTime ldt,
                                                  int dayIndex) {
        double value = switch (dev.getDeviceType()) {
            case "GLUCOMETER"    -> glucoseValue(patient, ldt, dayIndex);
            case "PULSE_OXIMETER"-> spo2Value(patient, ldt, dayIndex);
            case "BP_MONITOR"    -> bpSystolic(patient, ldt);
            case "SCALE"         -> weightValue(patient, dayIndex);
            case "SPIROMETER"    -> fev1Value(patient);
            default              -> 0.0;
        };

        String severity = computeSeverity(dev.getDeviceType(), value);

        return ObservationRecord.builder()
                .patientId(dev.getPatientId())
                .deviceSerial(dev.getSerial())
                .deviceType(dev.getDeviceType())
                .loincCode(dev.getLoincCode())
                .value(Math.round(value * 100.0) / 100.0)
                .unit(dev.getUnit())
                .severity(severity)
                .source("SEEDER_HISTORICAL")
                .observedAt(ts)
                .build();
    }

    // ── Modèles physiologiques ────────────────────────────────────────────────

    /**
     * Rythme circadien glucose avec pics post-prandiaux.
     * Intègre un événement hypoglycémique nocturne le jour 3.
     */
    private double glucoseValue(PatientProfile p, LocalDateTime ldt, int day) {
        int hour = ldt.getHour();
        int min  = ldt.getMinute();
        double minuteFraction = hour + min / 60.0;

        // Courbe circadienne de base
        double base;
        if      (minuteFraction < 6.0)  base = p.getBaseGlucose() - 0.10; // nuit/jeûne
        else if (minuteFraction < 7.5)  base = p.getBaseGlucose();        // réveil
        else if (minuteFraction < 9.5)  base = p.getBaseGlucose() + 0.85; // pic petit-déj
        else if (minuteFraction < 12.0) base = p.getBaseGlucose() + 0.25; // descente matinée
        else if (minuteFraction < 14.0) base = p.getBaseGlucose() + 0.95; // pic déjeuner
        else if (minuteFraction < 17.0) base = p.getBaseGlucose() + 0.20; // après-midi
        else if (minuteFraction < 20.0) base = p.getBaseGlucose() + 0.80; // pic dîner
        else if (minuteFraction < 22.0) base = p.getBaseGlucose() + 0.30; // soirée
        else                            base = p.getBaseGlucose() - 0.05; // nuit

        // Événement spécial : hypoglycémie nocturne au jour 3 entre 2h et 4h
        if (day == 3 && hour >= 2 && hour < 4) {
            base = 0.55 + rng.nextGaussian() * 0.05; // hypoglycémie critique
        }

        // Variabilité journalière (DiabèteT2 moins bien contrôlé le weekend)
        double dayVariability = (day % 7 >= 5) ? 0.15 : 0.0;

        return Math.max(0.3, base + dayVariability + rng.nextGaussian() * 0.06);
    }

    /**
     * SpO2 avec désaturation nocturne (BPCO/ICFe)
     * et épisode de décompensation au jour 5.
     */
    private double spo2Value(PatientProfile p, LocalDateTime ldt, int day) {
        int hour = ldt.getHour();

        // Désaturation nocturne physiologique
        double base = (hour >= 22 || hour < 6)
                ? p.getBaseSpO2() - 1.5   // nuit : SpO2 plus basse
                : p.getBaseSpO2();

        // Episode de décompensation ICFe au jour 5 (après-midi)
        if (day == 5 && hour >= 14 && hour < 20
                && p.getPathology().equals("ICFE")) {
            base = 88.0 + rng.nextGaussian() * 0.8; // décompensation
        }

        // Effort physique simulé (marche matinale)
        if (hour >= 9 && hour < 10) {
            base += 0.5;
        }

        return Math.max(75.0, Math.min(100.0,
                base + rng.nextGaussian() * 0.8));
    }

    /**
     * Pression artérielle systolique — variation matin/soir
     * et pic hypertensif au jour 6.
     */
    private double bpSystolic(PatientProfile p, LocalDateTime ldt) {
        int hour = ldt.getHour();
        // PA plus haute le matin et le soir (effet dipping inversé)
        double modifier = (hour < 10 || hour > 18) ? 8.0 : 0.0;
        return p.getBaseSystolic() + modifier + rng.nextGaussian() * 6.0;
    }

    /**
     * Poids corporel — dérive progressive ICFe (rétention hydrique).
     * +0.3 kg/jour sur 5 jours puis plateau = alerte clinique classique.
     */
    private double weightValue(PatientProfile p, int day) {
        double gain = (day < 5) ? day * 0.3 : 1.5; // plateau après J5
        return p.getWeightKg() + gain + rng.nextGaussian() * 0.1;
    }

    /**
     * VEMS (FEV1) — spirométrie BPCO.
     * Légère amélioration après bronchodilatateur simulée.
     */
    private double fev1Value(PatientProfile p) {
        // BPCO stade 3 : VEMS 30-50% de la valeur prédite
        // Pour Sophie (F, 65 ans, 160cm) : valeur prédite ~2.4 L/s
        double predicted = 2.4;
        double actual    = predicted * 0.42; // 42% = stade 3
        return Math.max(0.5, actual + rng.nextGaussian() * 0.08);
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
            case "BP_MONITOR" -> {
                if (value > 180.0 || value < 90.0) yield "CRITIQUE";
                if (value > 160.0)                 yield "URGENTE";
                if (value > 140.0)                 yield "INFORMATIVE";
                yield "NORMAL";
            }
            case "SCALE" -> {
                // Alerte si gain > 2 kg (rétention hydrique ICFe)
                yield (value > 84.0) ? "URGENTE" : "NORMAL";
            }
            case "SPIROMETER" -> {
                if (value < 0.6) yield "CRITIQUE";
                if (value < 0.8) yield "URGENTE";
                yield "NORMAL";
            }
            default -> "NORMAL";
        };
    }
}