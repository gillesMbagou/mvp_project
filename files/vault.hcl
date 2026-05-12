# infra/vault/config/vault.hcl
# Configuration Vault — mode développement local

ui = true

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true   # TLS désactivé en dev — activer en staging/prod
}

storage "file" {
  path = "/vault/data"
}

api_addr     = "http://0.0.0.0:8200"
cluster_addr = "http://0.0.0.0:8201"

# Mode dev : auto-unseal + root token fixe
# NE PAS utiliser en production — utiliser Auto-Unseal KMS ou Shamir
default_lease_ttl = "168h"
max_lease_ttl     = "720h"

log_level = "INFO"
log_file  = "/vault/logs/vault.log"
