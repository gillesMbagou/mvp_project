# Broker MQTT (dev local)

`caresync-q-iot` consomme trois topics MQTT (`caresync/devices/+/glucose`,
`.../spo2`, `.../weight`) via `quarkus-messaging-mqtt`. Cette extension,
contrairement à Kafka/PostgreSQL/Vault, n'a **pas** de Quarkus Dev Services :
aucun broker n'est démarré automatiquement en `mvn quarkus:dev`.

## Démarrer le broker

```bash
docker compose -f infra/mosquitto/docker-compose.mqtt-dev.yml up -d
```

Un [Eclipse Mosquitto](https://mosquitto.org/) écoute alors sur
`localhost:1883`, en accès anonyme (dev uniquement). `caresync-q-iot` s'y
connecte automatiquement (host/port par défaut de son `application.yml`,
overridables via `MQTT_HOST`/`MQTT_PORT`).

## Tester manuellement

```bash
docker exec caresync-mosquitto-dev mosquitto_pub \
  -t caresync/devices/device-1/glucose \
  -m '{"deviceId":"device-1","value":5.4,"unit":"mmol/L"}'
```

Vérifier ensuite `GET http://localhost:8092/q/health` : le check
`mqtt-glucose` doit passer à `[OK]`.

## Pourquoi pas de Dev Services

Si l'extension gagne un jour son propre Dev Services (comme Kafka ou
PostgreSQL), ce docker-compose pourra être retiré au profit de la
configuration automatique. En attendant, ce broker doit être lancé
manuellement avant de démarrer `caresync-q-iot` — ou celui-ci restera en
`health=DOWN` sur ses canaux MQTT (Kafka et la base de données, eux,
fonctionnent indépendamment).
