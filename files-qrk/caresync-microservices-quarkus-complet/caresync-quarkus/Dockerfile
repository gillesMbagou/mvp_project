# Dockerfile générique JVM-mode pour les 11 microservices HTTP CareSync
# (caresync-q-common exclu : lib partagée sans service HTTP).
#
# Usage :
#   docker build --build-arg SERVICE_MODULE=caresync-q-patient --build-arg SERVICE_PORT=8097 \
#     -t caresync-q-patient .
#
# Le build Maven se fait dans ce stage (pas de build préalable requis côté CI) :
# `-am` reconstruit caresync-q-common au passage, `-DskipTests` car les tests
# tournent déjà dans le job CI séparé (backend-ci.yml) avant que ce Dockerfile
# ne soit invoqué.

# ── Stage 1 : build Maven ────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE_MODULE

WORKDIR /workspace
COPY pom.xml ./
COPY caresync-q-common/pom.xml caresync-q-common/pom.xml
COPY ${SERVICE_MODULE}/pom.xml ${SERVICE_MODULE}/pom.xml

# Cache Maven : résout les dépendances avant de copier le code source, pour que
# les layers Docker restent réutilisables tant que les pom.xml ne changent pas.
RUN mvn -q -pl ${SERVICE_MODULE} -am dependency:go-offline -DskipTests || true

COPY caresync-q-common/src caresync-q-common/src
COPY ${SERVICE_MODULE}/src ${SERVICE_MODULE}/src

RUN mvn -q -pl ${SERVICE_MODULE} -am package -DskipTests

# ── Stage 2 : image d'exécution ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_MODULE
ARG SERVICE_PORT=8080
ENV SERVICE_MODULE=${SERVICE_MODULE}

RUN addgroup -S caresync && adduser -S caresync -G caresync
WORKDIR /deployments

COPY --from=build --chown=caresync:caresync /workspace/${SERVICE_MODULE}/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=caresync:caresync /workspace/${SERVICE_MODULE}/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=caresync:caresync /workspace/${SERVICE_MODULE}/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=caresync:caresync /workspace/${SERVICE_MODULE}/target/quarkus-app/quarkus/ /deployments/quarkus/

USER caresync
EXPOSE ${SERVICE_PORT}
ENV JAVA_OPTS_APPEND="-XX:MaxRAMPercentage=75.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
