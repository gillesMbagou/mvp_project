output "kv_mount_path" {
  description = "Chemin de montage du secret engine KV (à référencer dans les ExternalSecret)."
  value       = vault_mount.kv.path
}

output "secret_path_prefix" {
  description = "Préfixe de chemin pour cet environnement (ex. caresync-staging ou caresync)."
  value       = local.path_prefix
}

output "kubernetes_auth_backend_path" {
  description = "Chemin du backend d'auth Kubernetes (à référencer dans le ClusterSecretStore ESO)."
  value       = vault_auth_backend.kubernetes.path
}

output "service_roles" {
  description = "Nom du rôle Vault Kubernetes par service (à référencer dans le ClusterSecretStore ESO)."
  value       = { for s in var.services : s => vault_kubernetes_auth_backend_role.service[s].role_name }
}
