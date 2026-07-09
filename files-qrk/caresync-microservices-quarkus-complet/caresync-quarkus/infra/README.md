# Vault selon l'environnement

Le mécanisme de peuplement et de consommation de Vault change selon
l'environnement — l'application ne parle **jamais** directement à Vault en
dehors du profil `%dev`.

| Environnement | Peuplement de Vault | Consommation côté appli |
|---|---|---|
| Dev local (option A, par défaut) | Quarkus Vault Dev Services + `DevVaultSecretSeeder` (bean CDI, écrit un secret de démo au boot) | `VaultKVSecretEngine` (API Java, `app.vault.demo-secret-path`) |
| Dev local (option B) | `infra/vault-agent/` — Vault persistant + service `vault-seed` | Fichiers `.env` rendus par un Vault Agent, lus nativement par SmallRye Config |
| Staging / CI | `infra/terraform/vault/` (Terraform/OpenTofu, IaC déclaratif) | Identique à la prod (ESO) — Terraform ne fait que peupler Vault |
| Production K8s | `infra/k8s/external-secrets/` (External Secrets Operator) | `Secret` K8s natif → variables d'env classiques, **aucun accès direct à Vault** |

## Pourquoi 4 mécanismes différents

- **Dev** : on veut zéro friction pour démarrer un microservice localement.
  L'option A (Quarkus Dev Services) est automatique et ne demande rien.
  L'option B (Vault Agent) existe pour ceux qui veulent un Vault qui survit
  entre deux runs, ou tester du code qui lit des `.env` plutôt que l'API Vault.
- **Staging/CI** : les secrets doivent être provisionnés de façon reproductible
  et versionnée (Terraform), pas tapés à la main dans l'UI Vault.
- **Prod K8s** : on veut le moins de surface d'attaque possible — aucun pod
  applicatif n'a de réseau direct vers Vault ni de token Vault en mémoire.
  External Secrets Operator centralise cette responsabilité dans un seul
  contrôleur, avec rotation automatique via `refreshInterval`.

## Détails par mécanisme

- [`vault-agent/`](vault-agent/README.md)
- [`terraform/vault/`](terraform/vault/README.md)
- [`k8s/external-secrets/`](k8s/external-secrets/README.md)

Le bean `be.caresync.common.vault.DevVaultSecretSeeder` (module
`caresync-q-common`) documente l'option A directement dans son Javadoc, et
chaque `application.yml` de service a un bloc `%dev.quarkus.vault` +
`%dev.app.vault.demo-secret-path` correspondant.

# CI/CD Kubernetes (dev + prod)

Pipeline GitHub Actions complet — voir `.github/workflows/` à la racine du
dépôt (`backend-ci.yml`, `backend-cd-dev.yml`, `backend-cd-prod.yml`,
`frontend-ci.yml`, `frontend-cd-dev.yml`, `frontend-cd-prod.yml`,
`infra-dev-dependencies.yml`, `observability-dev.yml`) et
[`k8s/README.md`](k8s/README.md) pour le détail des manifestes.

## En un coup d'œil

| | Dev | Prod |
|---|---|---|
| Déclencheur | push `develop` | tag `vX.Y.Z` (+ approbation manuelle, environment GitHub `production`) |
| Manifestes | Kustomize `k8s/overlays/dev` | Kustomize `k8s/overlays/prod` |
| Kafka/Postgres/Keycloak/Vault/Redis/Mosquitto | in-cluster, léger (`helm/dev-dependencies/`, `infra-dev-dependencies.yml`, manuel) | externes/managés — jamais déployés par ce pipeline |
| Secrets applicatifs | `kustomize secretGenerator` depuis GitHub Secrets `DEV_*` | ESO + Vault (`k8s/external-secrets/`, déjà documenté ci-dessus), ce pipeline ne fait qu'appliquer les manifestes ESO |
| Registry images | `ghcr.io/<owner du repo>/caresync-q-<service>` et `caresync-frontend` | idem, tag = nom du tag git |
| Build Docker | JVM mode, `Dockerfile` générique (`ARG SERVICE_MODULE`) à la racine de `caresync-quarkus/` | idem |
| Quality gate | Sonar bloquant (`backend-ci.yml`, réutilisé par `backend-cd-dev.yml`/`backend-cd-prod.yml` via `workflow_call`) | idem |
| Observabilité | Prometheus/Grafana + Jaeger + Loki + OTel Collector, `observability-dev.yml` (manuel) | même topologie à reproduire côté cluster prod (hors périmètre de ce pipeline) |

## Prérequis à configurer une fois (non automatisés par ce pipeline)

- Branche `develop` (créée), environment GitHub `production` avec règle
  d'approbation (reviewers) sur le repo.
- Secrets GitHub Actions : `KUBE_CONFIG_DEV`, `KUBE_CONFIG_PROD` (kubeconfig
  en base64), `SONAR_HOST_URL`, `SONAR_TOKEN` (+ `SONAR_ORGANIZATION` si
  SonarCloud), `DEV_DB_USER`, `DEV_DB_PASSWORD`, `PROD_DB_HOST`,
  `PROD_KAFKA_BROKERS`, `PROD_KEYCLOAK_URL`, `PROD_REDIS_HOST`,
  `PROD_MQTT_HOST`, `PROD_OTEL_ENDPOINT`. `GITHUB_TOKEN` natif suffit pour GHCR.
- Cluster prod : External Secrets Operator + CRD Prometheus Operator déjà
  installés (ce pipeline ne les installe pas), et les dépendances externes
  (Postgres/Kafka/Keycloak/Vault/Redis/Mosquitto) déjà provisionnées et
  joignables sous les noms/URLs fournis via les secrets `PROD_*`.

## Limite connue — Redis et Keycloak en prod

`caresync-q-gateway` a `redis://redis:6379` et
`http://keycloak:8180/realms/caresync` **en dur** dans son `application.yml`
(bloc `%prod`), pas via variable d'env — contrairement à `DB_HOST`,
`KAFKA_BROKERS` et `MQTT_HOST` qui sont bien paramétrables. En prod, il faut
donc soit nommer les Service Kubernetes exposant Redis/Keycloak exactement
`redis`/`keycloak` dans le namespace `caresync` (ex. via un `Service` de type
`ExternalName` pointant vers l'instance managée réelle), soit modifier
`caresync-q-gateway/src/main/resources/application.yml` pour lire
`REDIS_URL`/`KEYCLOAK_URL` comme le fait déjà `caresync-q-patient` — non fait
dans ce chantier (changement de code applicatif, pas d'infra).
