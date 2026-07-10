# Jeu de données de démo + simulateur IoT

Peuple une base Postgres persistante avec des données réalistes (200 patients,
20 médecins, 50 infirmiers, 10 dispositifs IoT) et simule des mesures IoT en
continu via MQTT, pour observer le flux temps réel dans le dashboard sans
dispositif physique.

## Pourquoi un simulateur MQTT (et pas de dispositif réel)

Aucun matériel IoT n'étant disponible, ce script publie en MQTT exactement le
JSON attendu par `IoTProcessor` (`caresync-q-iot`) — c'est le point d'entrée
naturel du pipeline existant (MQTT → Kafka → SSE → dashboard, et → alert-svc
pour la persistance des alertes), donc aucune modification de code backend
n'est nécessaire.

## 1. Base de données

Le schéma (`patients`, `professionnels`, `etablissements`, `iot_devices`,
`alerts`) est auto-créé par Hibernate (`hibernate-orm.database.generation:
update`), pas de migration Flyway/Liquibase. Il faut donc démarrer chaque
service une fois contre la base cible avant d'insérer les données :

```bash
# Depuis caresync-q-<service>/, pour patient/etablissement/iot/alert :
mvn quarkus:dev \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://<host>:<port>/<db> \
  -Dquarkus.datasource.username=<user> \
  -Dquarkus.datasource.password=<pass> \
  -Dquarkus.oidc.tenant-enabled=false   # évite de dépendre de Keycloak pour juste créer le schéma
# Ctrl+C une fois "Listening on: http://localhost:<port>" affiché.
```

Puis régénérer et appliquer le seed (déterministe, `random.seed(42)` — mêmes
données à chaque exécution) :

```bash
python3 generate_seed.py
# Produit 01_etablissement.sql, 02_patient.sql, 03_iot.sql, devices_for_simulator.json

psql -h <host> -p <port> -U <user> -d caresync_etablissement -f 01_etablissement.sql
psql -h <host> -p <port> -U <user> -d caresync_patient       -f 02_patient.sql
psql -h <host> -p <port> -U <user> -d caresync_iot           -f 03_iot.sql
```

Environnement de référence utilisé pour générer ces données (session de dev
locale) : conteneur `caresync-postgres` (`docker ps`), port hôte `5434`, user
`caresyncadmin`, 4 bases `caresync_patient` / `caresync_etablissement` /
`caresync_iot` / `caresync_alerts` créées manuellement (`CREATE DATABASE`).
Le port JDBC `5432` étant codé en dur dans chaque `application.yml` (`%prod`),
il faut passer par `quarkus.datasource.jdbc.url` explicite pour cibler un
port différent — `DB_HOST` seul ne suffit pas.

Les `id` étant des UUID générés à la volée par le script (pas par la base),
relancer `generate_seed.py` puis ré-appliquer les `.sql` sur une base déjà
peuplée dupliquerait les lignes plutôt que de les mettre à jour — vider les
4 tables au préalable (`TRUNCATE ... CASCADE`) si vous voulez régénérer.

## 1bis. Dossier patient (plans de soins + prescriptions)

Même principe pour `caresync-q-careplan` (port 8094, base `caresync_careplan`)
et `caresync-q-prescription` (port 8095, base `caresync_prescription`) — créer
les bases, démarrer chaque service une fois pour créer le schéma
(`care_plans`, `care_plan_tasks`, `medicaments`, `prescriptions`,
`prescription_lines`), puis :

```bash
docker exec caresync-postgres psql -U caresyncadmin -d caresync_patient \
  -t -A -F'|' -c "SELECT id, pathology, assignedmedicid FROM patients;" \
  > patients_export.psv

python3 generate_dossier_seed.py
# Produit 04_careplan.sql, 05_prescription.sql (10 médicaments de référence,
# 1 plan de soins + tâches par patient selon sa pathologie — mêmes types de
# tâches que CarePlanResource.POST /, 1 prescription pour ~80% des patients)

psql -h <host> -p <port> -U <user> -d caresync_careplan     -f 04_careplan.sql
psql -h <host> -p <port> -U <user> -d caresync_prescription -f 05_prescription.sql
```

`caresync-q-dossier` (port 8096, agrégateur pur — aucune table à lui, mais une
base `caresync_dossier` doit exister pour qu'Agroal puisse initialiser son
pool de connexions au démarrage) appelle patient-svc/careplan-svc/
prescription-svc via 3 `@RestClient` dont l'URL par défaut
(`http://caresync-q-<service>:<port>`) est un nom DNS style Kubernetes — à
surcharger en dev local :

```bash
mvn quarkus:dev \
  -Dquarkus.rest-client.patient-api.url=http://localhost:8097 \
  -Dquarkus.rest-client.careplan-api.url=http://localhost:8094 \
  -Dquarkus.rest-client.prescription-api.url=http://localhost:8095 \
  ... # + les overrides datasource/oidc habituels
```

## 2. Simulateur IoT

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python simulate_iot.py --host localhost --port 1883 --interval 6
```

- Lit `devices_for_simulator.json` (généré par `generate_seed.py`, mapping
  device → patient/baseline) et publie sur `caresync/devices/<serial>/{glucose,spo2,weight}`.
- Valeurs qui fluctuent (±6% glucose/poids, ±2% SpO2) autour de la baseline
  du patient assigné, avec ~8% de mesures hors plage pour démontrer le flux
  d'alerte (seuils réels d'`IoTProcessor`/`AlertProcessor` : glucose CRITIQUE
  <0.60 ou >4.00 g/L, SpO2 CRITIQUE <85%, poids URGENTE >84kg).
- Nécessite un broker MQTT joignable sur `--host:--port` (en dev local,
  conteneur `caresync-mosquitto-dev`, port 1883 déjà exposé) et
  `caresync-q-iot` démarré et abonné aux topics `caresync/devices/+/*`.
- `Ctrl+C` pour arrêter ; aucune donnée n'est perdue (rien n'est persisté par
  le simulateur lui-même, seul `caresync-q-iot`/`caresync-q-alert` écrivent en base).

## 3. Vérification bout-en-bout

Avec `caresync-q-gateway`, `caresync-q-patient`, `caresync-q-iot`,
`caresync-q-alert` et le frontend démarrés (+ Keycloak, Kafka joignables) :
dashboard → "Mesures IoT temps réel" (SSE `/api/stream/observations`) et
"Alertes récentes" (SSE `/api/stream/alerts` + historique
`GET /api/v1/alertes`) doivent s'actualiser en continu pendant que le
simulateur tourne.
