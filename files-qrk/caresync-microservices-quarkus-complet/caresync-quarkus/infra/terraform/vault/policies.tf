# Une policy par service, moindre privilège : lecture seule sur son propre
# chemin uniquement. C'est cette policy que le rôle Kubernetes (kubernetes_auth.tf)
# attache au ServiceAccount du pod correspondant, pour qu'External Secrets
# Operator ne puisse lire QUE le secret de son propre microservice.

resource "vault_policy" "service" {
  for_each = toset(var.services)

  name = "caresync-${var.environment}-${each.value}-read"

  policy = <<-EOT
    path "${vault_mount.kv.path}/data/${local.path_prefix}/${each.value}" {
      capabilities = ["read"]
    }
    path "${vault_mount.kv.path}/metadata/${local.path_prefix}/${each.value}" {
      capabilities = ["read"]
    }
  EOT
}
