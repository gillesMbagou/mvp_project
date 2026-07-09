# Manifestes Kubernetes CareSync

```
k8s/
├── base/                    # 11 microservices HTTP (Deployment/Service/ServiceAccount)
│   ├── namespace.yaml
│   ├── kustomization.yaml
│   └── services/<service>/  # 1 dossier par microservice
├── overlays/
│   ├── dev/                 # replicas=1, dépendances in-cluster, secrets via secretGenerator
│   └── prod/                # replicas=3, HPA, dépendances externes, secrets via ESO
├── frontend/                 # Deployment/Service nginx (Angular buildé)
├── external-secrets/          # ClusterSecretStore + ExternalSecret x11 (prod/staging), cf. son propre README
└── traefik/                   # Middleware rate-limit devant le gateway
```

## Convention de nommage (à ne pas casser)

- **Deployment / Service : `caresync-q-<service>`** — `caresync-q-gateway`
  route en interne par ce nom DNS exact (voir
  `caresync-q-gateway/.../GatewayProxy.java`, `ROUTES`). Seul le gateway est
  exposé via Ingress ; les 10 autres restent `ClusterIP`.
- **ServiceAccount / Secret : `caresync-<service>` / `caresync-<service>-secrets`**
  (sans le `q-`) — doit correspondre aux rôles Vault Kubernetes déjà créés par
  `infra/terraform/vault/kubernetes_auth.tf` (`bound_service_account_names`).

## Commandes utiles

```bash
# Valider sans appliquer
kubectl kustomize infra/k8s/base | kubectl apply --dry-run=client -f -

# Dev : nécessite secrets/db.env (jamais committé, cf. secrets/db.env.example)
echo -e "DB_USER=caresync\nDB_PASS=devpassword" > infra/k8s/overlays/dev/secrets/db.env
kubectl kustomize infra/k8s/overlays/dev | kubectl apply -f -

# Prod : nécessite configmap-prod.env (jamais committé, cf. configmap-prod.env.example)
kubectl kustomize infra/k8s/overlays/prod | kubectl apply -f -
```

## Simplifications assumées (à revoir si besoin)

- `overlays/prod/resources-patch.yaml` applique les mêmes `requests`/`limits`
  CPU/mémoire aux 11 microservices **et** au frontend (même patch, sélecteur
  `app.kubernetes.io/part-of=caresync`) — le frontend nginx n'a probablement
  pas besoin d'autant de ressources qu'un service Quarkus ; à affiner par
  service si la métrique réelle le justifie.
- Pas de `NetworkPolicy` : tous les pods du namespace `caresync` peuvent se
  joindre librement entre eux. À ajouter si le modèle de menace l'exige.
- `overlays/dev/ingress-dev.yaml` et `overlays/prod/ingress-prod.yaml`
  utilisent des hosts placeholder (`*.caresync.local`, `api.caresync.be`,
  `app.caresync.be`) — à adapter à votre DNS réel.
