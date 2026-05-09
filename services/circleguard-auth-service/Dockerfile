# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copiar archivos de configuración Gradle
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY services/circleguard-auth-service/ services/circleguard-auth-service/

RUN chmod +x gradlew
RUN ./gradlew :services:circleguard-auth-service:bootJar -x test --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/services/circleguard-auth-service/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
