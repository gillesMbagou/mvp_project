#!/bin/sh
# infra/vault/init/init-vault.sh
# Initialise les secrets engines et policy CareSync
# Exécuter UNE FOIS après le 1er démarrage : docker exec caresync-vault sh /vault/init/init-vault.sh

set -e
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="caresync-root-token"

echo "==> Activation des Secrets Engines"

# KV v2 — credentials applicatifs
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "  kv-v2 déjà activé"

# Transit — chiffrement applicatif (NISS, données sensibles)
vault secrets enable transit 2>/dev/null || echo "  transit déjà activé"

# PKI — certificats TLS internes
vault secrets enable pki 2>/dev/null || echo "  pki déjà activé"
vault secrets tune -max-lease-ttl=87600h pki

# Database — rotation automatique des mots de passe PostgreSQL
vault secrets enable database 2>/dev/null || echo "  database déjà activé"

echo "==> Écriture des secrets applicatifs"

# Credentials PostgreSQL
vault kv put secret/caresync/postgres \
  host="postgres" \
  port="5432" \
  database="caresync" \
  username="caresync" \
  password="caresync_secret"

# Credentials Redis
vault kv put secret/caresync/redis \
  host="redis" \
  port="6379" \
  password="redis_secret"

# JWT signing key
vault kv put secret/caresync/jwt \
  secret="$(head -c 64 /dev/urandom | base64)" \
  expiration="3600"

# SMTP (MailHog en dev)
vault kv put secret/caresync/smtp \
  host="mailhog" \
  port="1025" \
  username="" \
  password=""

# Keycloak client secret
vault kv put secret/caresync/keycloak \
  realm="caresync" \
  client-id="caresync-backend" \
  client-secret="caresync-client-secret-dev" \
  issuer-url="http://keycloak:8080/realms/caresync"

echo "==> Création de la clé Transit pour données sensibles"
vault write -f transit/keys/patient-data type="aes256-gcm96"
vault write -f transit/keys/phi-data     type="aes256-gcm96"   # Protected Health Information

echo "==> Création de la policy CareSync"
vault policy write caresync-policy - <<EOF
# Lecture des secrets applicatifs
path "secret/data/caresync/*" {
  capabilities = ["read", "list"]
}
# Chiffrement/déchiffrement via Transit
path "transit/encrypt/patient-data" { capabilities = ["update"] }
path "transit/decrypt/patient-data" { capabilities = ["update"] }
path "transit/encrypt/phi-data"     { capabilities = ["update"] }
path "transit/decrypt/phi-data"     { capabilities = ["update"] }
EOF

echo ""
echo "✅  Vault initialisé avec succès !"
echo "   UI  : http://localhost:8200/ui  (token: caresync-root-token)"
