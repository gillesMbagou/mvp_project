-- infra/postgres/init/02-init-extensions.sql
-- Extensions et schémas initiaux pour CareSync

\connect caresync

-- Extensions utiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- génération UUID v4
CREATE EXTENSION IF NOT EXISTS "pgcrypto";        -- chiffrement at-rest
CREATE EXTENSION IF NOT EXISTS "pg_trgm";         -- recherche fuzzy (LIKE rapide)
CREATE EXTENSION IF NOT EXISTS "unaccent";        -- recherche sans accents

-- Schéma public = référentiels partagés (médicaments, codes CIM-10, LOINC)
-- Les tenants auront chacun leur schéma : CREATE SCHEMA tenant_<id>

-- Table de mapping tenant → schéma (dans le schéma public)
CREATE TABLE IF NOT EXISTS public.tenants (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nom           VARCHAR(200) NOT NULL,
  schema_name   VARCHAR(50) NOT NULL UNIQUE,
  actif         BOOLEAN NOT NULL DEFAULT true,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tenant de développement
INSERT INTO public.tenants (nom, schema_name) VALUES ('CareSync Dev', 'tenant_dev')
ON CONFLICT (schema_name) DO NOTHING;

CREATE SCHEMA IF NOT EXISTS tenant_dev;

-- Droits
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO caresync;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO caresync;
GRANT ALL PRIVILEGES ON SCHEMA tenant_dev TO caresync;
