variable "vault_addr" {
  description = "URL du serveur Vault (staging ou prod selon le workspace Terraform utilisé)."
  type        = string
}

variable "environment" {
  description = "Nom de l'environnement, utilisé comme préfixe du chemin KV (ex. \"staging\", \"production\")."
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment doit valoir \"staging\" ou \"production\"."
  }
}

variable "kv_mount_path" {
  description = "Chemin de montage du secret engine KV v2."
  type        = string
  default     = "secret"
}

variable "services" {
  description = "Liste des microservices CareSync ayant un chemin Vault dédié."
  type        = list(string)
  default = [
    "alert",
    "analytics",
    "audit",
    "careplan",
    "dossier",
    "etablissement",
    "iot",
    "messaging",
    "patient",
    "prescription",
  ]
}

variable "secret_values" {
  description = <<-EOT
    Valeurs réelles des secrets par service, ex. { patient = { "quarkus.datasource.username" = "...", "quarkus.datasource.password" = "..." } }.
    Alimenté en CI via TF_VAR_secret_values à partir du coffre-fort de secrets du pipeline
    (GitHub Actions secrets, etc.) — ne jamais committer de valeurs réelles ici.
  EOT
  type        = map(map(string))
  sensitive   = true
}

variable "kubernetes_host" {
  description = "URL de l'API server du cluster K8s (pour le backend d'auth Kubernetes utilisé par ESO)."
  type        = string
}

variable "kubernetes_ca_cert" {
  description = "Certificat CA du cluster K8s, au format PEM."
  type        = string
  sensitive   = true
}
