# CareSync — MVP

CareSync is a healthcare coordination platform designed for multi-tenant, multi-professional environments (hospitals, clinics, CPTS). This repository contains the data model, architecture diagrams, specifications, and local development infrastructure for the MVP.

---

## Repository contents

| File / Folder | Description |
|---|---|
| `CareSync_ERD.jsx` | Interactive Entity-Relationship Diagram (React) |
| `CareSync_EA.mermaid` | Enterprise architecture diagram |
| `CareSync_Specifications.docx` | Functional specifications |
| `CareSync_UseCases_BDD.pptx` | Use cases and BDD scenarios |
| `generateSQL.js` | DDL generator — produces PostgreSQL `CREATE TABLE` statements from the ERD entity definitions |
| `docker-compose.yml` | Full local dev stack |
| `files/` | Infrastructure init scripts (PostgreSQL, Vault, Keycloak) |
| `boutique_en_ligne_mcd.html` | Reference MCD (e-commerce domain, used for modelling comparison) |

---

## Data model

The ERD covers 16 entities across 8 domains:

| Domain | Entities |
|---|---|
| **Acteurs** | `ETABLISSEMENT`, `PROFESSIONNEL` |
| **Patient** | `PATIENT`, `PATIENT_CONDITION`, `EQUIPE_SOINS` |
| **Plans de soins** | `CARE_PLAN`, `CARE_PLAN_TASK` |
| **Observations & IoT** | `OBSERVATION`, `IOT_DEVICE` |
| **Prescriptions** | `MEDICAMENT`, `PRESCRIPTION`, `PRESCRIPTION_LINE` |
| **Alertes** | `ALERTE` |
| **Messagerie** | `MESSAGE_SECURISE`, `MESSAGE_DESTINATAIRE` |
| **Audit** | `AUDIT_LOG` |

Open `CareSync_ERD.jsx` in a React sandbox (e.g. [StackBlitz](https://stackblitz.com)) to browse entities and relations interactively.

### Generate SQL DDL

```bash
node generateSQL.js
```

---

## Tech stack (local dev)

| Layer | Technology |
|---|---|
| Database | PostgreSQL 16 (multi-schema/tenant) |
| Auth | Keycloak 26 (OAuth2 / OIDC) |
| Cache / Sessions | Redis 7 |
| Event streaming | Kafka 7.6 + Zookeeper |
| Secrets / Encryption | HashiCorp Vault 1.17 |
| Full-text search | Elasticsearch 8.13 |
| Email (dev) | MailHog |
| Admin tools *(optional)* | pgAdmin, Kafka UI, Swagger UI, Kibana |

---

## Getting started

**Prerequisites:** Docker >= 24, Docker Compose v2, ~6 GB RAM

```bash
# Start core services
docker compose up -d

# Initialise Vault (once, after Vault is healthy)
docker exec caresync-vault sh /vault/init/init-vault.sh

# Start optional admin tools
docker compose --profile tools up -d
```

### Service URLs

| Service | URL | Credentials |
|---|---|---|
| Keycloak | http://localhost:8080 | admin / admin |
| Vault UI | http://localhost:8200/ui | Token: `caresync-root-token` |
| MailHog | http://localhost:8025 | — |
| pgAdmin *(tools)* | http://localhost:5050 | admin@caresync.local / admin |
| Kafka UI *(tools)* | http://localhost:8090 | — |
| Swagger UI *(tools)* | http://localhost:8082 | — |
| Kibana *(tools)* | http://localhost:5601 | — |

See [`files/README.md`](files/README.md) for full infrastructure documentation, Spring Boot configuration snippets, Kafka topic details, and security notes.

---

> **Warning:** All credentials in this repository are for local development only. Never use them in staging or production environments.
