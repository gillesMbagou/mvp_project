# Config Vault Agent pour le dev local (option B), voir infra/vault-agent/README.md.
# Démarré via docker-compose.vault-dev.yml, contre le Vault persistant "vault server -dev"
# défini dans le même fichier (jamais utilisé contre un Vault réel).

vault {
  address = "http://vault:8200"
}

auto_auth {
  method "token_file" {
    config = {
      token_file_path = "/vault/config/dev-token"
    }
  }
}

# Pas de cache/listener nécessaire : cet agent ne fait que du templating vers
# des fichiers .env, il n'est pas utilisé comme proxy Vault par les apps.
template_config {
  exit_on_retry_failure = false
}

# Un bloc "template" par microservice : chaque secret/caresync/<service> est
# rendu dans caresync-q-<service>/.env, avec un nom de clé compatible avec les
# ${DB_USER:...} déjà présents dans application.yml.

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/alert" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-alert/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/analytics" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-analytics/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/audit" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-audit/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/careplan" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-careplan/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/dossier" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-dossier/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/etablissement" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-etablissement/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/iot" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-iot/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/messaging" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-messaging/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/patient" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-patient/.env"
  error_on_missing_key = false
}

template {
  contents             = <<EOT
{{- with secret "secret/data/caresync/prescription" }}
{{- range $k, $v := .Data.data }}
{{ $k }}={{ $v }}
{{- end }}
{{- end }}
EOT
  destination          = "/workspace/caresync-q-prescription/.env"
  error_on_missing_key = false
}
