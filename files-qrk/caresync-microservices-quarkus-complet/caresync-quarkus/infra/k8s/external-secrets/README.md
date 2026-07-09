# Production (et staging) K8s — External Secrets Operator

Prérequis : [External Secrets Operator](https://external-secrets.io/) installé
sur le cluster, et le module Terraform (`../terraform/vault/`) déjà appliqué
(il crée le rôle Vault Kubernetes référencé par le `ClusterSecretStore`).

## Ordre de déploiement

1. `terraform apply` (`../terraform/vault/`) — crée le mount KV, les secrets,
   les policies et le rôle d'auth Kubernetes.
2. `kubectl apply -f cluster-secret-store.yaml` — un `ClusterSecretStore` par
   service (voir le commentaire dans ce fichier).
3. `kubectl apply -f patient-external-secret.yaml` (et l'équivalent pour les 9
   autres services) — crée/rafraîchit le `Secret` K8s natif toutes les 5 min.
4. Déployer l'application (voir `patient-deployment-snippet.yaml` pour le
   câblage `envFrom.secretRef`).

## Ce que ça remplace

L'application **ne parle jamais directement à Vault** en staging/prod : elle
lit des variables d'environnement classiques (`DB_USER`, `DB_PASS`, ...)
injectées depuis un `Secret` K8s ordinaire. Vault reste la source de vérité,
mais le chemin de délivrance passe entièrement par ESO — aucune dépendance
réseau vers Vault depuis les pods applicatifs, aucun client `quarkus-vault`
actif hors du profil `%dev`.

## Les 9 autres services

Fait : `<service>-secret-store.yaml` + `<service>-external-secret.yaml` pour
`alert`, `analytics`, `audit`, `careplan`, `dossier`, `etablissement`, `iot`,
`messaging`, `prescription` (calqués sur le patron `patient-*`, mêmes clés
`DB_USER`/`DB_PASS`). `kustomization.yaml` dans ce dossier les référence tous
(sauf `patient-deployment-snippet.yaml`, un extrait illustratif, pas une
ressource à déployer) — c'est ce qui permet à
`../overlays/prod/kustomization.yaml` de référencer `../../external-secrets`
comme une seule ressource. Voir `../README.md` pour la vue d'ensemble CI/CD.
