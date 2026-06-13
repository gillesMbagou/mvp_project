-- Création de la base Keycloak (caresync_db est créée par POSTGRES_DB)
CREATE DATABASE keycloak;
CREATE USER keycloak WITH ENCRYPTED PASSWORD 'keycloak_secret';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
\connect keycloak
GRANT ALL ON SCHEMA public TO keycloak;
