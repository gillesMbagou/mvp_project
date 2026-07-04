# CareSync Microservices — Quarkus 3 / Java 21

## Avantages Quarkus vs Spring Boot pour CareSync

| Aspect | Spring Boot 4 | Quarkus 3 |
|---|---|---|
| Startup | ~3-5s | ~0.3-0.8s |
| Mémoire | ~300MB | ~80MB |
| Native | Spring Native (complexe) | `mvn package -Pnative` |
| Réactif | Reactor (Mono/Flux) | Mutiny (Uni/Multi) |
| REST | @RestController + WebFlux | @Path + RESTEasy Reactive |
| ORM | Spring Data JPA + @Repository | Panache (entity auto-repository) |
| Kafka | @KafkaListener | @Incoming/@Outgoing (déclaratif) |
| MQTT | Spring Integration | quarkus-messaging-mqtt |
| Sécurité | Spring Security OAuth2 | quarkus-oidc |
| Dev mode | DevTools (hot reload) | `quarkus dev` (live coding) |
| Test | @SpringBootTest | @QuarkusTest (+ Dev Services) |

## Démarrage en dev (Docker auto par Dev Services!)

```bash
cd caresync-q-iot
mvn quarkus:dev
# Quarkus démarre automatiquement Kafka, PostgreSQL, Keycloak en Docker
# Pas besoin de docker-compose pour le développement !
```

## Build natif

```bash
# Image native (~10MB, démarrage < 50ms)
mvn package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t caresync-iot:native .
```

## Commandes SmallRye vs Spring Boot

### Kafka Consumer
```java
// Spring Boot
@KafkaListener(topics="caresync.iot.observations", groupId="my-group")
public void consume(ConsumerRecord<String, MyEvent> record) { ... }

// Quarkus
@Incoming("iot-observations")  // configuré dans application.properties
public void consume(MyEvent event) { ... }
```

### Kafka Producer
```java
// Spring Boot
kafkaTemplate.send("caresync.alerts", key, alert);

// Quarkus
@Outgoing("clinical-alerts")  // dans la signature de méthode
public AlertEvent process(ObsEvent obs) { return buildAlert(obs); }
```

### Pipeline MQTT → Kafka
```java
// Spring Boot (80 lignes : MqttConfig + MqttToKafkaBridge)
// Quarkus (10 lignes)
@Incoming("mqtt-glucose")
@Outgoing("observations-out")
public ObsEvent bridge(byte[] mqttPayload) {
    return parse(mqttPayload);
}
```

### SSE (Server-Sent Events)
```java
// Spring Boot
@GetMapping(produces = TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<T>> stream() {
    return sink.asFlux().map(e -> ServerSentEvent.builder(e).build());
}

// Quarkus
@Channel("observations-sse")
Multi<T> observations; // injecté automatiquement

@GET @Produces(SERVER_SENT_EVENTS)
public Multi<T> stream() { return observations; }
```
