# CareSync Glucose — Stream Réactif

Microservice de monitoring IoT en temps réel pour la plateforme **CareSync** : simulation de dispositifs médicaux, streaming SSE, bridge MQTT→Kafka et observabilité OpenTelemetry complète.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-3.9-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-OTLP-425CC7?logo=opentelemetry&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Manifests-326CE5?logo=kubernetes&logoColor=white)

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    CARESYNC GLUCOSE — FLUX TEMPS RÉEL              │
│                                                                    │
│  MultiPatientSimulator (5s)                                        │
│      │  Glucose · SpO2 · Tension · Poids · FEV1                   │
│      ▼                                                             │
│  MQTT (Mosquitto)  ──►  MqttToKafkaBridge  ──►  Kafka             │
│                                   │          topic: glucose.raw    │
│                          ┌────────┴──────────────────┐            │
│                          ▼                           ▼             │
│              GlucoseEventStream           GlucoseKafkaConsumer     │
│              (Sinks.Many multicast)       (évaluation clinique)    │
│                          │                           │             │
│                     JPA persist              Redis dedup           │
│                   (boundedElastic)          → glucose.alerts       │
│                          │                           │             │
│                     SSE /stream/*          NotificationService     │
│                    (Angular front)         → Redis Pub/Sub         │
└────────────────────────────────────────────────────────────────────┘
```

## Stack technique

| Couche | Technologie |
|---|---|
| **Runtime** | Java 21 · Spring Boot 4.0.6 · Netty (WebFlux) |
| **Streaming** | Project Reactor · Server-Sent Events (SSE) |
| **Messaging** | Apache Kafka 3.9 (KRaft) · Eclipse Mosquitto 2 (MQTT QoS 1) |
| **Persistance** | PostgreSQL 16 · Spring Data JPA · HikariCP |
| **Cache & Pub/Sub** | Redis 7 (Lettuce réactif) |
| **Sécurité** | Keycloak 26 · OAuth2 Resource Server · JWT |
| **Observabilité** | OpenTelemetry · Micrometer · Prometheus · Grafana · Jaeger · Loki |
| **Documentation** | SpringDoc OpenAPI 3 · Swagger UI |

## Domaines fonctionnels

| Domaine | Entités | Description |
|---|---|---|
| **Simulation IoT** | `SimulatedDevice`, `MultiPatientSimulator` | 5 types de capteurs avec modèles physiologiques réalistes |
| **Patient** | `Patient`, `ClinicalContext`, `PatientProfile` | Dossier médical complet (allergies, antécédents, ordonnances) |
| **Observations** | `ObservationRecord` | Mesures LOINC : glycémie, SpO2, PA, poids, FEV1 |
| **Plans de soins** | `CarePlan`, `CarePlanTask`, `TherapeuticObjective` | Protocoles et tâches cliniques |
| **Prescriptions** | `Prescription`, `PrescriptionLine`, `DrugInteraction` | Ordonnances avec détection d'interactions |
| **Messagerie** | `SecureMessage`, `MessageThread` | Messagerie chiffrée inter-professionnels |
| **Audit RGPD** | `AuditLog`, `PatientConsent`, `DataRetentionPolicy` | Traçabilité complète des accès |

## Seuils d'alerte cliniques

| Paramètre | CRITIQUE | URGENTE | INFORMATIVE | NORMALE |
|---|---|---|---|---|
| **Glycémie (g/L)** | < 0,60 ou > 4,00 | < 0,80 ou > 2,50 | < 1,00 ou > 1,80 | 1,00 – 1,80 |
| **SpO2 (%)** | < 85 | < 90 | < 94 | ≥ 94 |
| **Systolique (mmHg)** | < 90 ou > 180 | > 160 | > 140 | ≤ 140 |

## Démarrage rapide — Docker Compose

**Prérequis :** Docker Desktop ou Docker Engine ≥ 24, 8 Go RAM disponibles.

```bash
git clone https://github.com/gillesMbagou/mvp_project.git
cd mvp_project/glucose-java-demo-stream/glucose-java-demo-stream

# Lancer toute la stack (13 services)
docker compose up -d

# Suivre le démarrage de l'application
docker compose logs -f caresync-app
```

### Accès aux interfaces

| Service | URL | Identifiants |
|---|---|---|
| **Application API** | http://localhost:8080/swagger-ui.html | JWT Keycloak requis |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Jaeger (traces)** | http://localhost:16686 | — |
| **Prometheus** | http://localhost:9090 | — |
| **Keycloak** | http://localhost:7080 | admin / admin |
| **Actuator** | http://localhost:8081/actuator/health | — |

```bash
# Stopper la stack
docker compose down

# Stopper et supprimer les volumes
docker compose down -v
```

## Démarrage local (sans Docker)

**Prérequis :** Java 21, Maven 3.9+, PostgreSQL 16, Redis 7, Kafka, Mosquitto, Keycloak actifs.

```bash
# Configurer les variables d'environnement
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5438/caresync_db
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092
export DEMO_MQTT_BROKER_URL=tcp://localhost:1883

# Lancer l'application
./mvnw spring-boot:run
```

## Endpoints SSE (streaming temps réel)

```
GET /api/health-professional/stream/observations?serial={deviceSerial}
    → Flux<ServerSentEvent<GlucoseObservation>>

GET /api/health-professional/stream/alerts
    → Filtre severity ≥ URGENTE

GET /api/health-professional/stream/stats
    → Statistiques pushées toutes les 30s

GET /api/health-professional/stream/heartbeat
    → Keep-alive pour les proxies
```

## Observabilité OpenTelemetry

La stack d'observabilité s'auto-configure via Docker Compose ou les variables d'environnement Kubernetes.

```
Application (Micrometer OTel Bridge)
    │
    ▼ OTLP HTTP :4318
OTel Collector
    ├── Traces  ──► Jaeger  (UI: :16686)
    ├── Métriques ► Prometheus (scrape :8889) ──► Grafana
    └── Logs ──────► Loki ──────────────────────► Grafana
```

**Corrélation logs ↔ traces :** chaque log JSON contient `traceId` et `spanId`, permettant de naviguer directement d'un log Loki vers la trace Jaeger correspondante dans Grafana.

### Variables d'environnement OTel

| Variable | Défaut | Description |
|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | Endpoint OTel Collector |
| `OTEL_SAMPLING_PROBABILITY` | `1.0` | Taux d'échantillonnage des traces |
| `ENVIRONMENT` | `local` | Label ajouté à toutes les métriques |

## Déploiement Kubernetes

Les manifests dans `k8s/` couvrent un déploiement production complet.

```bash
# Appliquer le namespace
kubectl apply -f k8s/namespace.yaml

# Déployer l'application
kubectl apply -f k8s/app/

# Configurer le ServiceMonitor (Prometheus Operator)
kubectl apply -f k8s/observability/
```

### Ce qui est inclus

| Manifest | Contenu |
|---|---|
| `namespace.yaml` | Namespace `caresync` |
| `configmap.yaml` | Configuration non sensible (URLs, feature flags) |
| `secret.yaml` | Credentials base64 — utiliser External Secrets en prod |
| `deployment.yaml` | 2 réplicas · rolling update zero-downtime · securityContext non-root · FS read-only · probes startup/liveness/readiness · requests/limits CPU+mémoire |
| `service.yaml` | ClusterIP sur ports http (80) et actuator (8081) |
| `ingress.yaml` | NGINX · TLS cert-manager · annotations SSE (proxy-buffering off, timeout 3600s) |
| `hpa.yaml` | Autoscaling 2→8 pods (CPU 70%, mémoire 80%) avec anti-flapping |
| `servicemonitor.yaml` | Prometheus Operator CRD pour scrape automatique |

## Structure du projet

```
├── src/main/java/be/caresync/demo/
│   ├── api/                   # Controllers REST (WebFlux)
│   ├── bridge/                # MqttToKafkaBridge (Spring Integration)
│   ├── config/                # Kafka, Redis, Sécurité OAuth2
│   ├── consumer/              # GlucoseKafkaConsumer (évaluation clinique)
│   ├── model/                 # GlucoseObservation, AlertEvent, entités JPA (32)
│   ├── notification/          # NotificationService (Kafka → Redis Pub/Sub)
│   ├── repository/jpa/        # Spring Data JPA (30+ repositories)
│   ├── seeder/                # DataSeeder (7 jours d'historique au démarrage)
│   ├── service/               # Logique métier (12 services)
│   ├── simulator/             # MultiPatientSimulator + DashboardController
│   └── stream/                # GlucoseEventStream (Sinks.Many) + SseController
│
├── src/main/resources/
│   ├── application.yml        # Configuration (variables d'env + valeurs par défaut)
│   └── logback-spring.xml     # JSON structuré (prod) / coloré (local)
│
├── docker-compose.yml         # Stack complète : app + infra + OTel (13 services)
├── Dockerfile                 # Multi-stage Maven → JRE Alpine
├── otel/                      # Configuration OTel Collector
├── grafana/                   # Datasources + dashboard provisionné
├── prometheus/                # prometheus.yml
├── promtail/                  # Collecte des logs Docker → Loki
├── mosquitto/                 # Configuration MQTT broker
└── k8s/                       # Manifests Kubernetes (Deployment, HPA, Ingress…)
```

## Codes LOINC utilisés

| Code | Paramètre |
|---|---|
| `14743-9` | Glucose interstitiel (CGM) |
| `59408-5` | SpO2 (oxymétrie) |
| `55284-4` | Pression artérielle (systolique/diastolique) |
| `29463-7` | Poids corporel |
| `19896-4` | VEMS / FEV1 (spirométrie) |

## Licence

MIT
