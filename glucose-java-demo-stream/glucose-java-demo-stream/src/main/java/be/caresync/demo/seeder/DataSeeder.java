package be.caresync.demo.seeder;

import be.caresync.demo.model.db.ObservationRecord;
import be.caresync.demo.model.db.PatientProfile;
import be.caresync.demo.model.db.SimulatedDevice;
import be.caresync.demo.model.db.patient.ClinicalContext;
import be.caresync.demo.model.db.patient.Patient;
import be.caresync.demo.model.db.staff.Infirmiere;
import be.caresync.demo.model.db.staff.Medecin;
import be.caresync.demo.model.db.staff.UserRole;
import be.caresync.demo.repository.jpa.*;
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
 * DataSeeder — génère 7 jours de données historiques réalistes au démarrage.
 *
 * Initialise :
 *   - 3 PatientProfile (agrégat Patient + ClinicalContext)
 *   - 2 médecins + 2 infirmier(e)s
 *   - 7 dispositifs médicaux
 *   - Historique d'observations sur 7 jours
 *
 * Ordre de persistance : ClinicalContext en premier (pas de cascade depuis
 * PatientProfile), puis PatientProfile (cascade → Patient).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PatientRepository          patientRepo;         // gère PatientProfile
    private final ClinicalContextRepository clinicalContextRepo;
    private final DeviceRepository deviceRepo;
    private final ObservationRepository obsRepo;
    private final MedecinRepository medecinRepo;
    private final InfirmiereRepository infirmiereRepo;

    private final Random rng = new Random(42);

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

        List<PatientProfile> profiles = createProfiles();
        List<SimulatedDevice> devices = createDevices(profiles);
        createStaff(profiles);
        generateHistory(profiles, devices);

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== Base initialisée en {}ms : {} patients | {} médecins | {} infirmier(e)s | {} dispositifs | {} observations ===",
                elapsed,
                patientRepo.count(), medecinRepo.count(), infirmiereRepo.count(),
                deviceRepo.count(), obsRepo.count());
    }

    // ── 1. PatientProfile = Patient + ClinicalContext ─────────────────────────

    private List<PatientProfile> createProfiles() {
        // ClinicalContext sauvegardé en premier : pas de cascade depuis PatientProfile
        // (jointure read-only pour éviter les conflits de clé partagée).
        List<ClinicalContext> contexts = List.of(
            ClinicalContext.builder()
                .patientId("patient-001")
                .baseGlucose(1.25).baseSpO2(97.5)
                .baseSystolic(138.0).baseDiastolic(85.0)
                .build(),
            ClinicalContext.builder()
                .patientId("patient-002")
                .baseGlucose(1.05).baseSpO2(94.0)
                .baseSystolic(145.0).baseDiastolic(90.0)
                .build(),
            ClinicalContext.builder()
                .patientId("patient-003")
                .baseGlucose(1.02).baseSpO2(91.0)
                .baseSystolic(132.0).baseDiastolic(80.0)
                .build()
        );
        clinicalContextRepo.saveAll(contexts);

        // PatientProfile cascade vers Patient via @MapsId.
        // L'id de PatientProfile est dérivé de Patient.id.
        List<PatientProfile> profiles = List.of(
            PatientProfile.builder()
                .patient(Patient.builder()
                    .id("patient-001")
                    .firstName("Marie-Christine").lastName("Decleves")
                    .age(58).gender("F").pathology("DIABETE_T2")
                    .weightKg(74.5).heightCm(163.0)
                    .build())
                .clinicalContext(contexts.get(0))
                .build(),

            PatientProfile.builder()
                .patient(Patient.builder()
                    .id("patient-002")
                    .firstName("Jean-Marie").lastName("Lécrivain")
                    .age(72).gender("M").pathology("ICFE")
                    .weightKg(82.0).heightCm(175.0)
                    .build())
                .clinicalContext(contexts.get(1))
                .build(),

            PatientProfile.builder()
                .patient(Patient.builder()
                    .id("patient-003")
                    .firstName("Gilles").lastName("Mbagou")
                    .age(46).gender("M").pathology("BPCO")
                    .weightKg(78.0).heightCm(170.0)
                    .build())
                .clinicalContext(contexts.get(2))
                .build()
        );

        patientRepo.saveAll(profiles);
        log.info("  {} patients créés", profiles.size());
        return profiles;
    }

    // ── 2. Personnel soignant ─────────────────────────────────────────────────

    private void createStaff(List<PatientProfile> profiles) {
        List<Medecin> medecins = List.of(
            Medecin.builder()
                .id("med-001")
                .firstName("Isabelle").lastName("Moreau")
                .email("i.moreau@caresync.be").phone("+32 2 555 01 01")
                .role(UserRole.MEDECIN)
                .specialty("DIABETOLOGIE").rppsNumber("10012345678")
                .serviceUnit("Diabétologie — CHU Saint-Luc")
                .assignedPatientIds(new ArrayList<>(List.of(profiles.get(0).getId())))
                .build(),

            Medecin.builder()
                .id("med-002")
                .firstName("Thomas").lastName("Renard")
                .email("t.renard@caresync.be").phone("+32 2 555 02 02")
                .role(UserRole.MEDECIN)
                .specialty("CARDIOLOGIE").rppsNumber("10098765432")
                .serviceUnit("Cardiologie / Pneumologie — CHU Saint-Luc")
                .assignedPatientIds(new ArrayList<>(List.of(
                    profiles.get(1).getId(), profiles.get(2).getId())))
                .build()
        );
        medecinRepo.saveAll(medecins);
        log.info("  {} médecins créés", medecins.size());

        List<Infirmiere> infirmieres = List.of(
            Infirmiere.builder()
                .id("inf-001")
                .firstName("Claire").lastName("Bernard")
                .email("c.bernard@caresync.be").phone("+32 2 555 03 01")
                .role(UserRole.INFIRMIERE)
                .diplomeCode("IDE").nursingUnit("Diabétologie").shift("MATIN")
                .assignedPatientIds(new ArrayList<>(List.of(profiles.get(0).getId())))
                .build(),

            Infirmiere.builder()
                .id("inf-002")
                .firstName("Marc").lastName("Dumont")
                .email("m.dumont@caresync.be").phone("+32 2 555 03 02")
                .role(UserRole.INFIRMIERE)
                .diplomeCode("IDE").nursingUnit("USIC / Pneumologie").shift("APRES_MIDI")
                .assignedPatientIds(new ArrayList<>(List.of(
                    profiles.get(1).getId(), profiles.get(2).getId())))
                .build()
        );
        infirmiereRepo.saveAll(infirmieres);
        log.info("  {} infirmier(e)s créé(e)s", infirmieres.size());
    }

    // ── 3. Dispositifs ────────────────────────────────────────────────────────

    private List<SimulatedDevice> createDevices(List<PatientProfile> profiles) {
        List<SimulatedDevice> devices = new ArrayList<>();

        devices.add(device("LIB3-P001-001", profiles.get(0).getId(),
                "GLUCOMETER", "Abbott", "FreeStyle Libre 3",
                "14743-9", "g/L", "glucose", 300));
        devices.add(device("BP-P001-001", profiles.get(0).getId(),
                "BP_MONITOR", "Withings", "BPM Connect",
                "55284-4", "mmHg", "bloodpressure", 43200));

        devices.add(device("SPO2-P002-001", profiles.get(1).getId(),
                "PULSE_OXIMETER", "Nonin", "GO2",
                "59408-5", "%", "spo2", 600));
        devices.add(device("SCALE-P002-001", profiles.get(1).getId(),
                "SCALE", "Withings", "Body+",
                "29463-7", "kg", "weight", 86400));
        devices.add(device("BP-P002-001", profiles.get(1).getId(),
                "BP_MONITOR", "Omron", "HeartGuide",
                "55284-4", "mmHg", "bloodpressure", 43200));

        devices.add(device("SPO2-P003-001", profiles.get(2).getId(),
                "PULSE_OXIMETER", "Masimo", "MightySat",
                "59408-5", "%", "spo2", 300));
        devices.add(device("SPIRO-P003-001", profiles.get(2).getId(),
                "SPIROMETER", "NuvoAir", "Air Next",
                "19896-4", "L/s", "spirometry", 86400));

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
                .loincCode(loinc).unit(unit).mqttTopicSuffix(topicSuffix)
                .intervalSeconds(intervalSec).currentScenario("NORMAL").active(true)
                .build();
    }

    // ── 4. Génération de l'historique ─────────────────────────────────────────

    private void generateHistory(List<PatientProfile> profiles,
                                  List<SimulatedDevice> devices) {
        Instant now   = Instant.now();
        Instant start = now.minus(HISTORY_DAYS, ChronoUnit.DAYS);

        List<ObservationRecord> batch = new ArrayList<>();

        for (SimulatedDevice dev : devices) {
            PatientProfile profile = profiles.stream()
                    .filter(p -> p.getId().equals(dev.getPatientId()))
                    .findFirst().orElseThrow();

            Instant cursor = start;
            while (cursor.isBefore(now)) {
                LocalDateTime ldt = LocalDateTime.ofInstant(cursor, ZoneOffset.UTC);
                int dayOfHistory  = (int) ChronoUnit.DAYS.between(
                        start.atOffset(ZoneOffset.UTC).toLocalDate(),
                        ldt.toLocalDate());

                ObservationRecord obs =
                        generateObservation(dev, profile, cursor, ldt, dayOfHistory);
                if (obs != null) batch.add(obs);

                cursor = cursor.plusSeconds(dev.getIntervalSeconds());
                if (batch.size() >= 500) { obsRepo.saveAll(batch); batch.clear(); }
            }

            log.info("  {} : {} observations générées", dev.getSerial(),
                    obsRepo.countByPatientIdAndSeverity(dev.getPatientId(), "NORMAL")
                    + obsRepo.countByPatientIdAndSeverity(dev.getPatientId(), "URGENTE")
                    + obsRepo.countByPatientIdAndSeverity(dev.getPatientId(), "CRITIQUE")
                    + obsRepo.countByPatientIdAndSeverity(dev.getPatientId(), "INFORMATIVE"));
        }
        if (!batch.isEmpty()) obsRepo.saveAll(batch);
    }

    private ObservationRecord generateObservation(SimulatedDevice dev,
                                                   PatientProfile profile,
                                                   Instant ts,
                                                   LocalDateTime ldt,
                                                   int dayIndex) {
        double value = switch (dev.getDeviceType()) {
            case "GLUCOMETER"     -> glucoseValue(profile, ldt, dayIndex);
            case "PULSE_OXIMETER" -> spo2Value(profile, ldt, dayIndex);
            case "BP_MONITOR"     -> bpSystolic(profile, ldt);
            case "SCALE"          -> weightValue(profile, dayIndex);
            case "SPIROMETER"     -> fev1Value();
            default               -> 0.0;
        };

        return ObservationRecord.builder()
                .patientId(dev.getPatientId())
                .deviceSerial(dev.getSerial())
                .deviceType(dev.getDeviceType())
                .loincCode(dev.getLoincCode())
                .value(Math.round(value * 100.0) / 100.0)
                .unit(dev.getUnit())
                .severity(computeSeverity(dev.getDeviceType(), value))
                .source("SEEDER_HISTORICAL")
                .observedAt(ts)
                .build();
    }

    // ── Modèles physiologiques (délèguent aux sous-entités via PatientProfile) ─

    private double glucoseValue(PatientProfile p, LocalDateTime ldt, int day) {
        int hour = ldt.getHour();
        int min  = ldt.getMinute();
        double mf = hour + min / 60.0;

        double base;
        if      (mf < 6.0)  base = p.getBaseGlucose() - 0.10;
        else if (mf < 7.5)  base = p.getBaseGlucose();
        else if (mf < 9.5)  base = p.getBaseGlucose() + 0.85;
        else if (mf < 12.0) base = p.getBaseGlucose() + 0.25;
        else if (mf < 14.0) base = p.getBaseGlucose() + 0.95;
        else if (mf < 17.0) base = p.getBaseGlucose() + 0.20;
        else if (mf < 20.0) base = p.getBaseGlucose() + 0.80;
        else if (mf < 22.0) base = p.getBaseGlucose() + 0.30;
        else                base = p.getBaseGlucose() - 0.05;

        if (day == 3 && hour >= 2 && hour < 4) base = 0.55 + rng.nextGaussian() * 0.05;

        double dayVariability = (day % 7 >= 5) ? 0.15 : 0.0;
        return Math.max(0.3, base + dayVariability + rng.nextGaussian() * 0.06);
    }

    private double spo2Value(PatientProfile p, LocalDateTime ldt, int day) {
        int hour = ldt.getHour();
        double base = (hour >= 22 || hour < 6) ? p.getBaseSpO2() - 1.5 : p.getBaseSpO2();

        if (day == 5 && hour >= 14 && hour < 20 && "ICFE".equals(p.getPathology()))
            base = 88.0 + rng.nextGaussian() * 0.8;

        if (hour >= 9 && hour < 10) base += 0.5;
        return Math.max(75.0, Math.min(100.0, base + rng.nextGaussian() * 0.8));
    }

    private double bpSystolic(PatientProfile p, LocalDateTime ldt) {
        int hour = ldt.getHour();
        double modifier = (hour < 10 || hour > 18) ? 8.0 : 0.0;
        return p.getBaseSystolic() + modifier + rng.nextGaussian() * 6.0;
    }

    private double weightValue(PatientProfile p, int day) {
        double gain = (day < 5) ? day * 0.3 : 1.5;
        return p.getWeightKg() + gain + rng.nextGaussian() * 0.1;
    }

    private double fev1Value() {
        return Math.max(0.5, 2.4 * 0.42 + rng.nextGaussian() * 0.08);
    }

    //  Évaluation clinique
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
            case "SCALE"      -> (value > 84.0) ? "URGENTE" : "NORMAL";
            case "SPIROMETER" -> {
                if (value < 0.6) yield "CRITIQUE";
                if (value < 0.8) yield "URGENTE";
                yield "NORMAL";
            }
            default -> "NORMAL";
        };
    }
}
