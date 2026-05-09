# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copiar archivos de configuración Gradle
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY services/circleguard-form-service/ services/circleguard-form-service/

RUN chmod +x gradlew
RUN ./gradlew :services:circleguard-form-service:bootJar -x test --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/services/circleguard-form-service/build/libs/*[0-9].jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
