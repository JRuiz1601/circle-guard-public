# CircleGuard - Design patterns Sprint 2

Fecha: 2026-06-11

Este documento resume los patrones de diseno presentes en CircleGuard y las piezas principales que los implementan. El objetivo es que cualquier integrante del equipo pueda ubicar rapidamente donde vive cada patron y que responsabilidad cumple dentro de la arquitectura.

## Alcance

La descripcion se basa en la implementacion actual bajo `services/`, `observability/`, `.github/workflows/quality.yml`, `tests/performance/` y `tests/e2e/`.

## API Gateway

El patron API Gateway esta implementado por `circleguard-gateway-service`.

Archivos principales:

- `services/circleguard-gateway-service/src/main/java/com/circleguard/gateway/controller/GateController.java`
- `services/circleguard-gateway-service/src/main/java/com/circleguard/gateway/service/QrValidationService.java`
- `services/circleguard-gateway-service/src/main/resources/application.yml`

Responsabilidades:

- Expone `POST /api/v1/gate/validate`.
- Recibe un body con `token`.
- Delega validacion en `QrValidationService`.
- `QrValidationService` valida el JWT con `qr.secret`, extrae el `anonymousId` y consulta Redis con la llave `user:status:{anonymousId}`.
- Si el estado de salud es de riesgo (`CONTAGIED` o `POTENTIAL`), responde con acceso denegado; si el token es invalido o expiro, responde como token invalido; si no hay riesgo, permite entrada.
- En Sprint 2 Tomas agrego el contador de negocio `circleguard.gateway.qr.validations` con label `result=allowed|denied|invalid`.

El gateway tambien contiene un filtro de diagnostico agregado por Juan en `master`: `DiagnosticHeaderFilter`, condicionado por `feature.diagnostic-header.enabled`.

## Repository

CircleGuard usa Spring Data repositories para desacoplar servicios de la persistencia concreta. La implementacion combina repositorios JPA y Neo4j.

Repositorios JPA:

- `circleguard-auth-service`: `LocalUserRepository extends JpaRepository<LocalUser, UUID>`.
- `circleguard-identity-service`: `IdentityMappingRepository extends JpaRepository<IdentityMapping, UUID>`.
- `circleguard-form-service`: `QuestionRepository`, `QuestionnaireRepository`, `HealthSurveyRepository`.
- `circleguard-promotion-service`: `AccessPointRepository`, `BuildingRepository`, `FloorRepository`, `SystemSettingsRepository`.

Repositorios Neo4j:

- `circleguard-promotion-service`: `UserNodeRepository extends Neo4jRepository<UserNode, String>`.
- `circleguard-promotion-service`: `CircleNodeRepository extends Neo4jRepository<CircleNode, Long>`.

Uso relevante:

- `HealthStatusService` combina `UserNodeRepository`, `CircleNodeRepository`, `SystemSettingsRepository`, `Neo4jClient`, Redis y Kafka para actualizar estados, propagar contactos y emitir eventos.
- `LocationResolutionService` usa `AccessPointRepository` para resolver puntos de acceso fisicos antes de registrar proximidad.
- `IdentityVaultService` persiste el mapeo identidad real -> identidad anonima a traves del repositorio de identity.

## Event-Driven / Observer

El patron Event-Driven aparece implementado con Kafka. Los servicios publican eventos de dominio y otros servicios reaccionan mediante listeners anotados con `@KafkaListener`.

Productores reales:

- `IdentityVaultController` publica `audit.identity.accessed` al resolver identidades protegidas.
- `HealthSurveyService` publica `survey.submitted` cuando se envia una encuesta y `certificate.validated` cuando se aprueba un certificado.
- `HealthStatusService` publica `promotion.status.changed`, `circle.fenced` y `alert.priority` cuando cambian estados, circulos o alertas.
- `LocationResolutionService` publica `proximity.detected` a partir de senales WiFi resueltas.
- `StatusLifecycleService` publica `promotion.status.changed` durante transiciones automaticas.
- `AuditLogService` publica `notification.audit` para auditoria de entregas.

Consumidores reales:

- `SurveyListener` en promotion escucha `survey.submitted` y `certificate.validated`.
- `ExposureNotificationListener` en notification escucha `promotion.status.changed`.
- `CircleFencedListener` en notification escucha `circle.fenced`.
- `PriorityAlertListener` en notification escucha `alert.priority`.

Este diseno permite que form, promotion, identity y notification evolucionen de forma mas desacoplada: un servicio emite el evento y los suscriptores aplican efectos secundarios sin llamada HTTP directa entre todos los participantes.

## Observability / Micrometer

La observabilidad combina metricas tecnicas de Spring Boot Actuator con metricas de negocio agregadas por Tomas en Sprint 2.

Base tecnica:

- `observability/helm/servicemonitors.yaml` define `ServiceMonitor` para los 8 servicios activos y raspa `/actuator/prometheus`.
- `quality.yml` incluye JaCoCo, SonarCloud, OWASP ZAP y Trivy como puertas de calidad/seguridad.
- `observability/alerts/circleguard-pod-alerts.yaml` agrega reglas Prometheus/Alertmanager para pods CircleGuard no ready o down por mas de 2 minutos.

Metricas de negocio agregadas por Tomas:

- Auth: `circleguard.auth.logins`, exportada como `circleguard_auth_logins_total`, con `outcome=success|failure|error`.
- Form: `circleguard.form.questionnaires.created`, exportada como `circleguard_form_questionnaires_created_total`.
- Form: `circleguard.form.surveys.submitted`, exportada como `circleguard_form_surveys_submitted_total`.
- Gateway: `circleguard.gateway.qr.validations`, exportada como `circleguard_gateway_qr_validations_total`, con `result=allowed|denied|invalid`.

Dashboard Sprint 2:

- `observability/dashboards/circleguard-sprint2-business-dashboard.json`
- `observability/dashboards/circleguard-sprint2-business-dashboard-configmap.yaml`

Paneles reales:

- Logins por minuto por outcome.
- Actividad de formularios por minuto.
- Validaciones QR por minuto por resultado.
- Fallos de login por minuto.

## Circuit Breaker y Retry

Implementado en `circleguard-auth-service` como parte del track de resiliencia de Sprint 2.

Archivos principales:

- `services/circleguard-auth-service/src/main/java/com/circleguard/auth/client/IdentityClient.java`
- `services/circleguard-auth-service/src/main/java/com/circleguard/auth/client/ServiceUnavailableException.java`
- `services/circleguard-auth-service/src/main/resources/application.yml`
- `services/circleguard-auth-service/build.gradle.kts`

Implementacion:

- `IdentityClient.getAnonymousId()` llama a identity-service para mapear identidad real a `anonymousId`.
- Usa `@CircuitBreaker(name = "identityService", fallbackMethod = "getAnonymousIdFallback")`.
- Usa `@Retry(name = "identityService")`.
- La dependencia `io.github.resilience4j:resilience4j-spring-boot3:2.2.0` esta en el build del servicio auth.
- El fallback lanza `ServiceUnavailableException` cuando identity-service no responde o devuelve una respuesta invalida.

Dentro de la arquitectura, este patron protege el flujo de login cuando `auth-service` depende de `identity-service`.

## Feature Toggle

Implementado en `circleguard-gateway-service` como parte del track de configuracion de Sprint 2.

Archivos principales:

- `services/circleguard-gateway-service/src/main/java/com/circleguard/gateway/filter/DiagnosticHeaderFilter.java`
- `services/circleguard-gateway-service/src/test/java/com/circleguard/gateway/filter/DiagnosticHeaderFilterTest.java`
- `services/circleguard-gateway-service/src/main/resources/application.yml`
- `k8s/dev/circleguard-gateway-service/deployment.yaml`
- `k8s/stage/circleguard-gateway-service/deployment.yaml`
- `k8s/prod/circleguard-gateway-service/deployment.yaml`

Implementacion:

- El toggle se llama `feature.diagnostic-header.enabled`.
- `DiagnosticHeaderFilter` se activa con `@ConditionalOnProperty(name = "feature.diagnostic-header.enabled", havingValue = "true")`.
- Cuando esta activo, agrega el header `X-CircleGuard-Debug: enabled`.
- El toggle operativo actual controla el header de diagnostico del gateway.

## Ownership Sprint 2

- Calidad, pruebas, metricas de negocio, Alertmanager, dashboard Grafana y esta documentacion forman parte del track Tomas.
- Circuit Breaker, Retry, Feature Toggle, Terraform multiambiente, Secrets, RBAC, TLS y approvals forman parte del track Juan.
