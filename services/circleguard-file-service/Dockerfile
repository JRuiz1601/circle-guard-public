# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copiar archivos de configuración Gradle
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY services/circleguard-file-service/ services/circleguard-file-service/

RUN chmod +x gradlew
RUN ./gradlew :services:circleguard-file-service:bootJar -x test --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/services/circleguard-file-service/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
