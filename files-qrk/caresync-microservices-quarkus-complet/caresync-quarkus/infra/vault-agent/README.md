# Dev local — option B : Vault Agent sidecar

Alternative à l'option A (Quarkus Dev Services, voir `DevVaultSecretSeeder` dans
`caresync-q-common`). À utiliser si tu veux un Vault **persistant** entre deux
runs (les conteneurs Dev Services sont éphémères et recréés à chaque
`mvn quarkus:dev`), ou pour tester du code qui lit des variables d'environnement
plutôt que de parler à Vault directement.

## Démarrage

```bash
docker compose -f infra/vault-agent/docker-compose.vault-dev.yml up
```

Cela démarre :
1. `vault` : un `vault server -dev` avec le root token fixe `root` (uniquement valable contre ce Vault de dev local, jamais un vrai Vault).
2. `vault-seed` : écrit une fois un secret de démo (`demo.vault.secret`) sous `secret/caresync/<service>` pour les 10 microservices.
3. `vault-agent` : rend un fichier `.env` par microservice (`caresync-q-<service>/.env`) à partir de ces secrets.

Quarkus (SmallRye Config) charge automatiquement un `.env` présent dans le
répertoire de travail du module — aucune config applicative supplémentaire.

## Important

- `.env` est dans le `.gitignore` racine : ne jamais le committer.
- Le token `root` dans `dev-token` est le token de dev **par défaut de Vault**
  (`VAULT_DEV_ROOT_TOKEN_ID=root` dans le même `docker-compose.yml`), pas un
  secret réel — il n'ouvre l'accès qu'à ce conteneur Vault de dev jetable.
- Cette option coexiste avec l'option A (Dev Services) mais n'est pas destinée
  à tourner en même temps sur le même port (8200 par défaut pour les deux).
