terraform {
  required_version = ">= 1.5.0"

  required_providers {
    vault = {
      source  = "hashicorp/vault"
      version = "~> 4.0"
    }
  }
}

# Compatible aussi bien avec `terraform` (HashiCorp) qu'avec `tofu` (OpenTofu) :
# aucune fonctionnalité propriétaire n'est utilisée ici, seul le provider
# `hashicorp/vault` (disponible sur les deux registres) est requis.

provider "vault" {
  address = var.vault_addr
  # Le token est fourni par VAULT_TOKEN (variable d'env), jamais en dur ici.
  # En CI : un token à durée de vie courte, généré par l'étape d'auth du pipeline.
}
