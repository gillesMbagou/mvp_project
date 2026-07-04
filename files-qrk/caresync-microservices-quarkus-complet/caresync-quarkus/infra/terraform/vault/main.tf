# Provisionne la structure Vault (mount KV + un secret par microservice) pour
# un environnement donné (staging ou production). Les VALEURS des secrets
# viennent de var.secret_values (sensible, alimentée par la CI) — ce module ne
# fait que déclarer la structure et pousser les valeurs, il ne les invente pas.

resource "vault_mount" "kv" {
  path        = var.kv_mount_path
  type        = "kv-v2"
  description = "Secrets applicatifs CareSync (${var.environment})"
}

locals {
  # ex. "caresync-staging/patient" ou "caresync/patient" en prod
  path_prefix = var.environment == "production" ? "caresync" : "caresync-${var.environment}"
}

resource "vault_kv_secret_v2" "service" {
  for_each = toset(var.services)

  mount = vault_mount.kv.path
  name  = "${local.path_prefix}/${each.value}"

  data_json = jsonencode(lookup(var.secret_values, each.value, {}))
}
