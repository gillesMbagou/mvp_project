# CareSync — Infrastructure Docker Compose

## Prérequis
- Docker >= 24.x
- Docker Compose plugin v2 (`docker compose` sans tiret)
- 6 Go de RAM disponible recommandés

---

## Structure des fichiers

```
.
├── docker-compose.yml
└── infra/
    ├── keycloak/
    │   └── realms/
    │       └── caresync-realm.json      ← Realm pré-configuré (users, roles, clients)
    ├── pgadmin/
    │   └── servers.json                 ← Connexion PostgreSQL pré-configurée
    ├── postgres/
    │   └── init/
    │       ├── 01-init-databases.sh     ← Crée les bases caresync + keycloak
    │       └── 02-init-extensions.sql   ← Extensions uuid-ossp, pgcrypto, etc.
    └── vault/
        ├── config/
        │   └── vault.hcl                ← Configuration du serveur Vault
        └── init/
            └── init-vault.sh            ← Initialise engines + secrets (run once)
```

---

## Démarrage rapide

### 1 — Services principaux

```bash
# Démarrer tous les services de base
docker compose up -d

# Suivre les logs
docker compose logs -f

# Attendre que Keycloak soit prêt (~60s)
docker compose ps
```

### 2 — Initialiser Vault (une seule fois)

```bash
# Après que Vault soit démarré et healthy
docker exec caresync-vault sh /vault/init/init-vault.sh
```

### 3 — Démarrer les outils d'administration (optionnel)

```bash
# Lance pgAdmin + Kafka-UI + Swagger-UI + Kibana
docker compose --profile tools up -d
```

---

## URLs d'accès

| Service          | URL                              | Credentials              |
|------------------|----------------------------------|--------------------------|
| **Keycloak**     | http://localhost:8080            | admin / admin            |
| **Vault UI**     | http://localhost:8200/ui         | Token: caresync-root-token |
| **MailHog**      | http://localhost:8025            | —                        |
| **pgAdmin**      | http://localhost:5050  *(tools)* | admin@caresync.local / admin |
| **Kafka UI**     | http://localhost:8090  *(tools)* | —                        |
| **Swagger UI**   | http://localhost:8082  *(tools)* | —                        |
| **Kibana**       | http://localhost:5601  *(tools)* | —                        |
| **PostgreSQL**   | localhost:5432                   | caresync / caresync_secret |
| **Redis**        | localhost:6379                   | password: redis_secret   |
| **Kafka**        | localhost:9092                   | —                        |
| **Elasticsearch**| localhost:9200                   | —                        |

---

## Utilisateurs Keycloak pré-créés (realm: caresync)

| Username         | Mot de passe  | Rôle        |
|------------------|---------------|-------------|
| dr.dupont        | Password1!    | MEDECIN     |
| inf.martin       | Password1!    | INFIRMIER   |
| pharm.durand     | Password1!    | PHARMACIEN  |
| admin.caresync   | Password1!    | ADMIN       |

---

## Commandes utiles

```bash
# Arrêter sans supprimer les volumes
docker compose down

# Arrêter ET supprimer tous les volumes (reset complet)
docker compose down -v

# Rebuilder un service spécifique
docker compose up -d --force-recreate keycloak

# Logs d'un service
docker compose logs -f keycloak
docker compose logs -f vault

# Shell dans un container
docker exec -it caresync-postgres psql -U caresync -d caresync
docker exec -it caresync-redis redis-cli -a redis_secret
docker exec -it caresync-vault vault status

# Lister les topics Kafka
docker exec -it caresync-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## Configuration Spring Boot (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/caresync
    username: caresync
    password: caresync_secret
  data:
    redis:
      host: localhost
      port: 6379
      password: redis_secret
  kafka:
    bootstrap-servers: localhost:9092
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/caresync

elasticsearch:
  uris: http://localhost:9200

vault:
  uri: http://localhost:8200
  token: caresync-root-token
```

---

## Topics Kafka CareSync

| Topic                       | Partitions | Description                         |
|-----------------------------|------------|-------------------------------------|
| caresync.patient.events     | 3          | Événements dossier patient (CRUD)   |
| caresync.alerts             | 3          | Alertes cliniques générées          |
| caresync.iot.observations   | 6          | Données IoT temps réel (haut volume)|
| caresync.prescriptions      | 3          | Événements prescriptions            |
| caresync.notifications      | 3          | Notifications push/email/SMS        |
| caresync.audit              | 3          | Audit trail immuable                |

---

## Notes de sécurité

> ⚠️ Cette configuration est **UNIQUEMENT pour le développement local**.
>
> Pour staging/production :
> - Changer **tous** les mots de passe et secrets
> - Activer TLS sur Vault (`tls_disable = false`)
> - Utiliser Vault Auto-Unseal (AWS KMS, GCP KMS, Azure Key Vault)
> - Activer `xpack.security.enabled=true` sur Elasticsearch
> - Ne jamais exposer les ports sur `0.0.0.0` (garder `127.0.0.1`)
> - Configurer des `POSTGRES_MULTIPLE_DATABASES` avec des users dédiés
> - Restreindre le realm Keycloak (désactiver direct access grants)
