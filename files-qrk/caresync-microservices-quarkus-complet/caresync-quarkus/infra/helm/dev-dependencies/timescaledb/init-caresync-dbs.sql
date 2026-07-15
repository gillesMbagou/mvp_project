-- Exécuté une seule fois par l'image officielle timescale/timescaledb au
-- premier démarrage (docker-entrypoint-initdb.d), sur la base par défaut
-- (POSTGRES_DB=caresync_patient). Remplace l'équivalent bitnami
-- (primary.initdb.scripts) de l'ancien postgres-values.yaml.
CREATE DATABASE caresync_etablissement OWNER caresync;
CREATE DATABASE caresync_iot OWNER caresync;
CREATE DATABASE caresync_alert OWNER caresync;
CREATE DATABASE caresync_careplan OWNER caresync;
CREATE DATABASE caresync_prescription OWNER caresync;
CREATE DATABASE caresync_dossier OWNER caresync;
CREATE DATABASE caresync_messaging OWNER caresync;
CREATE DATABASE caresync_analytics OWNER caresync;
CREATE DATABASE caresync_audit OWNER caresync;
-- caresync_patient déjà créée via POSTGRES_DB ci-dessus
-- caresync-q-gateway n'a pas de base propre (proxy stateless)

-- L'extension timescaledb elle-même est créée par l'application
-- (TimescaleHypertableInitializer, caresync-q-analytics) au démarrage, pas
-- ici : à ce stade la base caresync_analytics vient d'être créée et
-- POSTGRES_USER (caresync) est superuser sur l'image officielle
-- timescale/timescaledb, donc CREATE EXTENSION depuis l'appli fonctionne.
