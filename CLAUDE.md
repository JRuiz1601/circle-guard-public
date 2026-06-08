# CircleGuard — Contexto y Reglas para Claude Code

## Proyecto
Sistema de microservicios para gestión de guardias de seguridad.
**Repo:** github.com/JRuiz1601/circle-guard-public
**Equipo:** Juan Ruiz (infra/CI-CD) · Tomás Quintero (observabilidad/pruebas)
**Plan completo:** ver `docs/plan-proyecto-final.md`

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Servicios | Spring Boot 4 / Java **21** / Gradle 8.14 (monorepo) |
| Base de datos | PostgreSQL 16, Neo4j 5.26 |
| Mensajería | Apache Kafka 7.6 + Zookeeper |
| Cache / LDAP | Redis 7.2, OpenLDAP 1.5 |
| Contenedores | Docker 29 / Docker Compose v2 |
| Orquestación | Kubernetes — Kind (Juan, 8GB) + Minikube (Tomás, 16GB) |
| CI/CD | GitHub Actions (.github/workflows/ci-packages.yml) |
| IaC | Terraform + HCP Terraform (remote state) |
| Observabilidad | Prometheus + Grafana + Loki + Jaeger + ELK |
| Patrones resiliencia | Resilience4j (Circuit Breaker + Retry en auth-service) |

---

## Servicios y puertos

| Servicio | Puerto | Dockerfile | k8s |
|---|---|---|---|
| circleguard-gateway-service | 8080 | ✅ | ✅ dev/stage/prod |
| circleguard-auth-service | 8081 | ✅ | ✅ dev/stage/prod |
| circleguard-identity-service | 8082 | ✅ | ✅ dev/stage/prod |
| circleguard-form-service | 8083 | ✅ | ✅ dev/stage/prod |
| circleguard-file-service | 8084 | ✅ | ✅ dev/stage/prod |
| circleguard-dashboard-service | 8085 | ✅ | ✅ dev/stage/prod |
| circleguard-notification-service | 8082* | ❌ pendiente | ❌ pendiente |
| circleguard-promotion-service | 8088 | ❌ pendiente | ❌ pendiente |

*notification-service comparte puerto con identity-service. En k8s no hay conflicto
(pods con IPs distintas). Para port-forward local usar puerto host 18082.

---

## Reglas de Git — OBLIGATORIAS

- **NUNCA** hacer push directo a `master` — siempre rama feature/fix + Pull Request
- `master` está protegida: requiere PR + 1 review + CI verde
- Nombres de rama: `feat/descripcion`, `fix/descripcion`, `chore/descripcion` (sin número de issue)
- Formato de commits (**semantic-release lo lee para generar tags**):
  - `feat: descripcion` → bump minor (v1.1.0)
  - `fix: descripcion` → bump patch (v1.0.1)
  - `chore: descripcion` → sin bump
  - `docs: descripcion` → sin bump
  - `BREAKING CHANGE: descripcion` en el body → bump major (v2.0.0)

---

## Terraform — Reglas operativas

### Antes de terraform apply (siempre)
```bash
# 1. terraform init es obligatorio incluso si existe .terraform.lock.hcl
cd terraform/environments/dev
terraform init

# 2. El namespace y ghcr-secret deben existir ANTES del apply
#    o todos los pods quedarán en ImagePullBackOff
kubectl get ns circleguard-dev || kubectl create namespace circleguard-dev
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=jruiz1601 \
  --docker-password=<CR_PAT> \    # GitHub PAT con scope read:packages
  -n circleguard-dev
```

### Variables clave
- `image_tag` por defecto: `latest` — las imágenes deben existir en GHCR con ese tag
- `TF_API_TOKEN` debe estar en GitHub Secrets para que HCP Terraform funcione en CI

### GHCR — paquetes privados por defecto
Los paquetes de GHCR son **privados** aunque el repo sea público. Requieren:
- `ghcr-secret` en cada namespace k8s (`circleguard-dev`, `circleguard-stage`, `circleguard-prod`)
- El PAT es `CR_PAT` con scope `read:packages` (≠ `TF_API_TOKEN` que es de HCP Terraform)
- Imágenes disponibles: `ghcr.io/jruiz1601/<servicio>:latest` y `:<run_number>`
- notification-service y promotion-service **no tienen imagen** hasta que Tomás cree sus Dockerfiles

---

## Semantic-release — NO tocar manualmente
- `.releaserc.json` en raíz configura el pipeline de release automático
- Cuando un PR con commits convencionales se mergea a master, GHA crea el tag (`v1.0.0`, etc.) y actualiza `CHANGELOG.md`
- El commit `chore(release): vX.Y.Z [skip ci]` es **generado automáticamente** — nunca modificarlo ni revertirlo

---

## Reglas de Docker / Build — CRÍTICAS

```
# CORRECTO — contexto siempre desde la raíz del monorepo
docker build -f services/circleguard-auth-service/Dockerfile .

# INCORRECTO — el Dockerfile usa gradlew y settings.gradle.kts del root
docker build services/circleguard-auth-service/
```

En GitHub Actions (`docker/build-push-action`):
```yaml
# CORRECTO
with:
  context: .
  file: services/circleguard-auth-service/Dockerfile

# INCORRECTO
with:
  context: services/circleguard-auth-service  # ← ROMPE el build
```

**Java:** siempre `java-version: '21'` en todos los workflows. Nunca `'17'`.

---

## Reglas de Kubernetes — CRÍTICAS

### Probes
```yaml
# Spring Boot tarda ~50s en arrancar — probes con valores generosos
livenessProbe:
  httpGet:
    path: /actuator/health
    port: <MISMO_QUE_SERVER_PORT>   # ← debe coincidir exactamente
  initialDelaySeconds: 120          # ← mínimo 120, nunca menos
  periodSeconds: 10
  failureThreshold: 5
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: <MISMO_QUE_SERVER_PORT>
  initialDelaySeconds: 120
  periodSeconds: 5
```

### Secretos — NUNCA hardcodeados
```yaml
# INCORRECTO
env:
  - name: JWT_SECRET
    value: "mi-secreto-hardcodeado"  # ← NUNCA

# CORRECTO
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: circleguard-secrets
        key: jwt-secret
```

### DNS entre servicios
```yaml
# INCORRECTO — localhost no funciona entre pods de k8s
IDENTITY_SERVICE_URL: http://localhost:8082

# CORRECTO — usar el nombre del Service de Kubernetes
IDENTITY_SERVICE_URL: http://circleguard-identity-service:8082
```

### Namespaces
Los 3 ambientes van en namespaces separados:
- `circleguard-dev` → desarrollo, deploy automático
- `circleguard-stage` → staging, requiere aprobación manual
- `circleguard-prod` → producción, requiere aprobación manual

---

## Kafka — configuración correcta para Docker/k8s

```yaml
# docker-compose.dev.yml — listeners correctos para que contenedores se hablen
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,INTERNAL://circleguard-kafka:29092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,INTERNAL:PLAINTEXT
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,INTERNAL://0.0.0.0:29092
KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

# En application.yml de los servicios — usar variable de entorno
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:circleguard-kafka:29092}
```

---

## Anti-patrones conocidos — NO repetir

Estos errores costaron horas de debugging en el proyecto anterior:

| ❌ Anti-patrón | ✅ Corrección |
|---|---|
| Decir "está arreglado" sin reconstruir la imagen | Todo fix = push → GHA verde → `kubectl rollout status` |
| Código corregido en el repo pero imagen sin rebuildar | Nunca validar con imagen/código viejo en el cluster |
| Probe apuntando a puerto distinto al SERVER_PORT | Verificar que containerPort == probe port == SERVER_PORT |
| `context: services/<svc>` en Docker build | Siempre `context: .` con `file: services/<svc>/Dockerfile` |
| `java-version: '17'` en CI | Java 21 en todo, sin excepción |
| `bootstrap-servers: localhost:9092` en pods k8s | Usar DNS del cluster: `circleguard-kafka:29092` |
| Secretos hardcodeados en deployment.yaml | Usar `secretKeyRef` apuntando a k8s Secret |
| Spring Security bloqueando `/actuator/health` | Permitir `/actuator/**` sin autenticación en SecurityConfig |

---

## Flujo de verificación obligatorio

Antes de declarar cualquier tarea como completa:

```
1. git push → rama feature
2. PR creado → CI (GHA) verde
3. PR mergeado a master
4. kubectl rollout status deployment/<servicio> -n circleguard-dev
5. kubectl logs -l app=<servicio> -n circleguard-dev --tail=50
   → sin errores de arranque
6. curl http://<servicio>/actuator/health → {"status":"UP"}
```

---

## Estructura del proyecto

```
circle-guard-public/
├── .github/workflows/ci-packages.yml  ← Pipeline principal GHA (build + push GHCR + semantic-release)
├── terraform/                     ← IaC modular (Juan)
├── observability/                 ← Prometheus, Grafana, Loki, ELK (Tomás)
├── k8s/{dev,stage,prod}/          ← Manifiestos por ambiente
├── services/                      ← 8 microservicios Spring Boot
├── tests/e2e/ + performance/      ← Pruebas E2E y Locust
├── docs/                          ← Documentación del proyecto
│   ├── plan-proyecto-final.md     ← Plan completo de implementación
│   ├── branching-strategy.md      ← Estrategia de branching
│   ├── design-patterns.md         ← Patrones implementados
│   └── change-management.md       ← Proceso de change management
├── pipelines/                     ← Jenkinsfiles (solo documentación histórica)
└── evidence/                      ← Screenshots por sprint
```
