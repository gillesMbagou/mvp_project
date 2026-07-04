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
