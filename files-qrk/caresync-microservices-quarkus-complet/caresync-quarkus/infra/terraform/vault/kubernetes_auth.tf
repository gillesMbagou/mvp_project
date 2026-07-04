# Backend d'auth Kubernetes : c'est ce qu'External Secrets Operator utilise
# pour s'authentifier à Vault depuis le cluster (staging ou prod), sans jamais
# transporter de token Vault statique. Un rôle par service, lié au
# ServiceAccount K8s "caresync-<service>" dans le namespace "caresync".

resource "vault_auth_backend" "kubernetes" {
  type = "kubernetes"
  path = "kubernetes-${var.environment}"
}

resource "vault_kubernetes_auth_backend_config" "this" {
  backend                = vault_auth_backend.kubernetes.path
  kubernetes_host        = var.kubernetes_host
  kubernetes_ca_cert     = var.kubernetes_ca_cert
  disable_iss_validation = true
}

resource "vault_kubernetes_auth_backend_role" "service" {
  for_each = toset(var.services)

  backend                          = vault_auth_backend.kubernetes.path
  role_name                        = "caresync-${each.value}"
  bound_service_account_names      = ["caresync-${each.value}"]
  bound_service_account_namespaces = ["caresync"]
  token_policies                   = [vault_policy.service[each.value].name]
  token_ttl                        = 3600
}
