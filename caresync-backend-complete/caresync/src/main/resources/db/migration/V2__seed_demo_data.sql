-- V2__seed_demo_data.sql
-- Données de démonstration — 3 patients, dispositifs, seuils

-- ── Patients ──────────────────────────────────────────────────────────────────
INSERT INTO patients (id, first_name, last_name, email, gender, date_of_birth,
                      medical_record_number, primary_pathology)
VALUES
    ('a0000001-0000-0000-0000-000000000001', 'Jean',   'Martin', 'jean.martin@demo.be',
     'M', '1958-04-12', 'MRN-0001', 'DIABETE_T2'),
    ('a0000002-0000-0000-0000-000000000002', 'Sophie', 'Durand', 'sophie.durand@demo.be',
     'F', '1965-09-23', 'MRN-0002', 'ICFe'),
    ('a0000003-0000-0000-0000-000000000003', 'Marc',   'Leroy',  'marc.leroy@demo.be',
     'M', '1950-01-07', 'MRN-0003', 'BPCO')
ON CONFLICT DO NOTHING;

-- ── Dispositifs IoT ───────────────────────────────────────────────────────────
INSERT INTO iot_devices (patient_id, device_type, manufacturer, model,
                         serial_number, mqtt_topic, protocol)
VALUES
    -- Jean Martin (Diabète T2)
    ('a0000001-0000-0000-0000-000000000001',
     'GLUCOMETER', 'Accu-Check', 'Guide Plus', 'GLU-DEMO-001',
     'caresync/devices/GLU-DEMO-001/glucose', 'MQTT'),

    -- Sophie Durand (ICFe)
    ('a0000002-0000-0000-0000-000000000002',
     'SCALE', 'Withings', 'Body+', 'BAL-DEMO-001',
     'caresync/devices/BAL-DEMO-001/weight', 'WIFI'),
    ('a0000002-0000-0000-0000-000000000002',
     'PULSE_OXIMETER', 'Nonin', 'GO2', 'OXY-DEMO-001',
     'caresync/devices/OXY-DEMO-001/spo2', 'BLE'),

    -- Marc Leroy (BPCO)
    ('a0000003-0000-0000-0000-000000000003',
     'PULSE_OXIMETER', 'Nonin', 'GO2', 'OXY-DEMO-002',
     'caresync/devices/OXY-DEMO-002/spo2', 'BLE')
ON CONFLICT DO NOTHING;

-- ── Seuils d'alerte ───────────────────────────────────────────────────────────
INSERT INTO alert_thresholds
    (patient_id, device_type, critical_low, warning_low, warning_high, critical_high,
     delta_threshold_48h, prescribed_by)
VALUES
    -- Jean Martin : Diabète T2 — glycémie
    ('a0000001-0000-0000-0000-000000000001',
     'GLUCOMETER', 0.6, 0.8, 2.5, 4.0, NULL, 'dr.dupont@caresync.be'),

    -- Sophie Durand : ICFe — poids + SpO2
    ('a0000002-0000-0000-0000-000000000002',
     'SCALE', NULL, NULL, NULL, NULL, 2.0, 'dr.martin@caresync.be'),
    ('a0000002-0000-0000-0000-000000000002',
     'PULSE_OXIMETER', 88.0, 91.0, NULL, NULL, NULL, 'dr.martin@caresync.be'),

    -- Marc Leroy : BPCO — SpO2
    ('a0000003-0000-0000-0000-000000000003',
     'PULSE_OXIMETER', 88.0, 90.0, NULL, NULL, NULL, 'dr.leroy@caresync.be')
ON CONFLICT DO NOTHING;
