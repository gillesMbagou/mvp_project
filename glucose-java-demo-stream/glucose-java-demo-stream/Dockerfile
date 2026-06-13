# ── Étape 1 : Build Maven ────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Télécharge les dépendances en cache (invalidé seulement si pom.xml change)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Étape 2 : Image de production (JRE slim) ──────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Utilisateur non-root pour sécurité
RUN addgroup -S caresync && adduser -S caresync -G caresync

COPY --from=builder /app/target/*.jar app.jar
RUN chown caresync:caresync app.jar

USER caresync

EXPOSE 8080 8081

# Liveness probe Spring Boot (/actuator/health/liveness)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1

ENV JAVA_OPTS="\
  -XX:+UseG1GC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=docker"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
