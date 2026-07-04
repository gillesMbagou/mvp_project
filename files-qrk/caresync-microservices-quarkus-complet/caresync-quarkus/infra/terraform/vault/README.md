# Terraform/OpenTofu — provisioning Vault (staging / CI)

Ce module ne fait que **peupler la structure de Vault** (mount KV, un secret
par microservice, policies en lecture seule, rôles d'auth Kubernetes) pour un
environnement donné. Il ne configure pas la consommation côté application —
en staging comme en prod, c'est External Secrets Operator qui synchronise ces
secrets vers des `Secret` K8s (voir `../k8s/external-secrets/`).

## Utilisation (exemple staging)

```bash
cd infra/terraform/vault
terraform init   # ou : tofu init

terraform plan \
  -var="vault_addr=https://vault-staging.caresync.internal:8200" \
  -var="environment=staging" \
  -var="kubernetes_host=https://staging-cluster-api:6443" \
  -var="kubernetes_ca_cert=$(cat staging-ca.pem)"
```

Les vraies valeurs de secrets (`secret_values`) ne sont **jamais** commitées :
elles arrivent via `TF_VAR_secret_values` (JSON), alimenté par le pipeline CI
depuis son propre coffre-fort de secrets (GitHub Actions secrets, etc.).

Pour la prod, même module, `-var="environment=production"` et des credentials
Vault/K8s différents (idéalement un workspace Terraform séparé par environnement).

## Vérification (sans accès à un vrai Vault)

```bash
terraform fmt -check
terraform validate
```
