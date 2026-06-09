-- V1__init_schema.sql
-- Schéma initial CareSync — Alertes & Télésurveillance IoT

-- ── Extension ─────────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Patients ──────────────────────────────────────────────────────────────────
CREATE TABLE patients (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name           VARCHAR(100)  NOT NULL,
    last_name            VARCHAR(100)  NOT NULL,
    email                VARCHAR(200)  NOT NULL UNIQUE,
    ins_nir              VARCHAR(50),
    gender               VARCHAR(10)   NOT NULL,
    date_of_birth        DATE,
    medical_record_number VARCHAR(50)  UNIQUE,
    primary_pathology    VARCHAR(30),
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_mrn   ON patients(medical_record_number);

-- ── Dispositifs IoT ───────────────────────────────────────────────────────────
CREATE TABLE iot_devices (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id       UUID          NOT NULL REFERENCES patients(id),
    device_type      VARCHAR(30)   NOT NULL,
    manufacturer     VARCHAR(100)  NOT NULL,
    model            VARCHAR(100)  NOT NULL,
    serial_number    VARCHAR(100)  NOT NULL UNIQUE,
    mqtt_topic       VARCHAR(200)  NOT NULL,
    protocol         VARCHAR(20)   NOT NULL DEFAULT 'MQTT',
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    last_seen_at     TIMESTAMPTZ,
    firmware_version VARCHAR(50),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_patient   ON iot_devices(patient_id);
CREATE INDEX idx_devices_serial    ON iot_devices(serial_number);
CREATE INDEX idx_devices_mqtt      ON iot_devices(mqtt_topic);
CREATE INDEX idx_devices_active    ON iot_devices(active, device_type);

-- ── Observations FHIR ─────────────────────────────────────────────────────────
CREATE TABLE observations (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id   UUID            NOT NULL REFERENCES patients(id),
    device_id    UUID            REFERENCES iot_devices(id),
    loinc_code   VARCHAR(20)     NOT NULL,
    device_type  VARCHAR(30),
    value        NUMERIC(12, 4)  NOT NULL,
    unit         VARCHAR(50)     NOT NULL,
    observed_at  TIMESTAMPTZ     NOT NULL,
    source       VARCHAR(20)     NOT NULL DEFAULT 'IOT',
    status       VARCHAR(30)     NOT NULL DEFAULT 'FINAL',
    note         TEXT,
    mqtt_topic   VARCHAR(200),
    latency_ms   BIGINT,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_obs_patient_date ON observations(patient_id, observed_at DESC);
CREATE INDEX idx_obs_loinc        ON observations(loinc_code);
CREATE INDEX idx_obs_device_type  ON observations(patient_id, device_type);
CREATE INDEX idx_obs_device_id    ON observations(device_id);

-- ── Seuils d'alerte ───────────────────────────────────────────────────────────
CREATE TABLE alert_thresholds (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id           UUID           NOT NULL REFERENCES patients(id),
    device_type          VARCHAR(30)    NOT NULL,
    critical_low         NUMERIC(10, 3),
    warning_low          NUMERIC(10, 3),
    warning_high         NUMERIC(10, 3),
    critical_high        NUMERIC(10, 3),
    delta_threshold_48h  NUMERIC(10, 3),
    prescribed_by        VARCHAR(200),
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE (patient_id, device_type)
);

CREATE INDEX idx_thresh_patient ON alert_thresholds(patient_id, active);

-- ── Alertes ───────────────────────────────────────────────────────────────────
CREATE TABLE alerts (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id               UUID         NOT NULL REFERENCES patients(id),
    observation_id           UUID         REFERENCES observations(id),
    type                     VARCHAR(30)  NOT NULL,
    severity                 VARCHAR(20)  NOT NULL,
    message                  TEXT         NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    escalation_level         VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    first_notified_at        TIMESTAMPTZ,
    escalated_at             TIMESTAMPTZ,
    escalated_to_email       VARCHAR(200),
    acknowledged_by          VARCHAR(200),
    acknowledged_at          TIMESTAMPTZ,
    acknowledgment_comment   TEXT,
    resolved_at              TIMESTAMPTZ,
    resolution_note          TEXT,
    triggered_value          VARCHAR(100),
    threshold_value          VARCHAR(100),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_patient_status ON alerts(patient_id, status);
CREATE INDEX idx_alert_severity       ON alerts(severity);
CREATE INDEX idx_alert_status_esc     ON alerts(status, escalation_level, first_notified_at);
CREATE INDEX idx_alert_created        ON alerts(created_at DESC);
