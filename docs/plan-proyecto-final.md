# Plan de Implementación — Proyecto Final IngeSoft V
> **Repo:** `github.com/JRuiz1601/circle-guard-public` | **Equipo:** Juan + Tomás  
> **Período:** Jun 4–12, 2026 | **Metodología:** GitHub Flow (Scrum adaptado, 2 sprints)

---

## Estado actual — Jun 9, 2026

| Sprint | Área | Estado |
|---|---|---|
| Sprint 0 | Todo | ✅ 100% completo |
| Sprint 1 | Terraform IaC — módulos namespace/microservice/middleware, Kind dev, HCP state | ✅ PR #16–#20 |
| Sprint 1 | GHA: build + push GHCR (6 servicios activos) — ci-packages.yml | ✅ GHA verde |
| Sprint 1 | Semantic-release + git-cliff — release.yml independiente | ✅ PR #21–#25 |
| Sprint 1 | 8 servicios en Docker Desktop Kubernetes de Tomás (1/1 Running) | ✅ Sprint 1 Tomás |
| Sprint 1 | Observabilidad: Prometheus/Grafana/Loki/Jaeger/ELK+Filebeat, ServiceMonitors, dashboards | ✅ Sprint 1 Tomás |
| Sprint 1 | GHA: SonarCloud + Trivy | ⏳ Pendiente — Sprint 2 |
| Sprint 1 | Iteración 1 en GitHub Projects (screenshots) | ⏳ Pendiente |
| Sprint 2 | Circuit Breaker + Retry + Feature Toggle (auth-service, gateway-service) | ⏳ Jun 9–11 — Juan |
| Sprint 2 | k8s Secrets + RBAC + TLS (cert-manager) | ⏳ Jun 9–11 — Juan |
| Sprint 2 | OWASP ZAP + JaCoCo + E2E/unit/integration tests | ⏳ Jun 9–11 — Tomás |
| Sprint 2 | Alertmanager + docs (design-patterns, change-management) | ⏳ Jun 9–11 — Tomás |

---

## 1. Decisiones técnicas confirmadas

| Aspecto | Decisión |
|---|---|
| CI/CD | **GitHub Actions** (no Jenkins) — corre en la nube, cero instalación local |
| Container registry | **GHCR** (`ghcr.io/jruiz1601/...`) — ya estaba en el GHA workflow |
| K8s Juan (8 GB RAM) | **Kind** (`kind create cluster`) — ~500 MB overhead, solo para validar TF/RBAC/TLS |
| K8s Tomás (16 GB RAM) | **Docker Desktop Kubernetes** (`docker-desktop`) — stack completo + observabilidad local |
| K8s en CI (GHA) | **Kind efímero** dentro del runner — corre integration/E2E tests por PR |
| Terraform state | **HCP Terraform free tier** (hasta 500 recursos, gratis) |
| Terraform providers | `kubernetes` + `helm` — Juan aplica en Kind local, demo final en cluster de Tomás |
| SonarQube | **SonarCloud** (SaaS gratuito para repos públicos — cero instalación) |
| Logs | **Grafana Loki** (Helm en Docker Desktop Kubernetes de Tomás) + **ELK funcional** (Filebeat DaemonSet en k8s → Logstash + Elasticsearch + Kibana en docker-compose) |
| Métricas | **kube-prometheus-stack** (Helm — Prometheus + Grafana incluidos) |
| Tracing | **Jaeger** all-in-one en Docker Desktop Kubernetes de Tomás |
| Seguridad k8s | `cert-manager` (TLS) + k8s Secrets + ServiceAccounts + RBAC |
| Semantic versioning | **semantic-release** en GHA (commit convencional → tag vX.Y.Z) |
| Release Notes | **git-cliff** (auto-genera CHANGELOG.md desde commits) |
| OWASP ZAP | `zaproxy/action-full-scan` en GHA pipeline |
| Circuit Breaker | **Resilience4j** en `circleguard-auth-service` |
| Retry | **Resilience4j** `@Retry` en `circleguard-auth-service` — reintenta hasta 3 veces con backoff antes de activar el CB |
| Feature Toggle | Flag en `application.yml` con `@ConditionalOnProperty` |

**Jenkinsfiles existentes (18):** NO se usan como CI/CD activo. Se conservan en el repo como
documentación del diseño de pipelines del Taller 2. La lógica se porta a GHA (2–3 horas de
trabajo, no empezar desde cero — el workflow existente ya tiene la estructura, solo necesita
correcciones y pasos adicionales).

**Nota para evaluador (incluir en docs):** SonarCloud es la versión SaaS de SonarQube — usa
el mismo motor de análisis estático. Se documenta explícitamente en `docs/architecture.md`.

---

## 2. Arquitectura de ambientes

```
┌─────── JUAN (8 GB) ─────────────┐   ┌──────── TOMÁS (16 GB) ───────────────────┐
│                                  │   │                                           │
│  Kind cluster (dev local)        │   │  Docker Desktop Kubernetes              │
│  ┌──────────────────────────┐    │   │  ┌────────────────────────────────────┐  │
│  │ namespaces dev/stage/prod │   │   │  │ 8 servicios + middleware completo  │  │
│  │ Terraform modules test    │   │   │  │ Prometheus + Grafana + Loki        │  │
│  │ RBAC / Secrets / TLS      │   │   │  │ Jaeger + Alertmanager              │  │
│  └──────────────────────────┘    │   │  └────────────────────────────────────┘  │
│                                  │   │  + ELK: Filebeat→Logstash→ES→Kibana (logs mgmt) │
│  docker-compose: Postgres+Redis  │   │                                           │
│  (solo para builds locales)      │   │  ← DEMO / PRESENTACIÓN desde aquí        │
└─────────────────────────────────-┘   └───────────────────────────────────────────┘
            ↕ mismo repo GitHub, ramas independientes
┌──────────────────── GHA Runner (nube GitHub) ───────────────────────────────────┐
│  Build + Unit Tests + SonarCloud + Trivy                                        │
│  Kind efímero → Integration/E2E Tests → Terraform apply (validación CI)         │
│  Deploy image a GHCR → Notificación resultado                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                          ↕ state remoto
                  HCP Terraform (free tier)
```

**Por qué Kind y no Minikube para Juan:**
- Minikube necesita una VM completa: 6–8 GB mínimo para el stack CircleGuard. Inviable en 8 GB Windows.
- Kind corre el control plane de k8s como contenedores Docker: ~500 MB overhead. Juan solo despliega
  lo que necesita para validar su trabajo (namespaces, RBAC, un servicio de prueba, cert-manager).
- Para integración completa (E2E entre servicios) Juan usa el cluster de Tomás o confía en GHA.

---

## 3. División de trabajo

### Track Juan — Infra / CI-CD (8 GB, Kind local)
Área de archivos: `terraform/`, `.github/workflows/`, `k8s/` base, seguridad k8s

| Componente | Peso | Sprint | Necesita cluster |
|---|---|---|---|
| Branching doc, GitHub Projects setup, proteger master | 10% | 0 | ❌ |
| Corregir + expandir GHA (Java 21, contexto Docker, SonarCloud, Trivy, aprobaciones) | 15% | 1 | ❌ GHA |
| Semantic-release + git-cliff (Release Notes automáticas) | 5% | 1 | ❌ |
| Terraform estructura modular dev/stage/prod + backend HCP | 20% | 1 | ✅ Kind local |
| Circuit Breaker Resilience4j en auth-service | 10% | 2 | ✅ Kind local |
| Retry Resilience4j en auth-service (mismo archivo que CB, ~10 líneas) | 10% | 2 | ✅ Kind local |
| Feature Toggle en gateway-service | 10% | 2 | ✅ Kind local |
| k8s Secrets (JWT hardcodeados → secretKeyRef) | 5% | 2 | ✅ Kind local |
| RBAC: ServiceAccount + Role + RoleBinding por namespace | 5% | 2 | ✅ Kind local |
| TLS: cert-manager + ClusterIssuer autofirmado + Ingress TLS | 5% | 2 | ✅ Kind local |

### Track Tomás — Calidad / Observabilidad (16 GB, Docker Desktop Kubernetes)
Área de archivos: `observability/`, `tests/`, código de servicios (Micrometer), docs

| Componente | Peso | Sprint | Necesita cluster |
|---|---|---|---|
| Dockerfiles + k8s para notification(8082) y promotion(8088) | infra base | 1 | ✅ Docker Desktop Kubernetes |
| kube-prometheus-stack + Loki + Jaeger en k8s | 10% | 1 | ✅ Docker Desktop Kubernetes |
| Instrumentar servicios con Micrometer (`/actuator/prometheus`) + Counters de negocio: intentos login (ok/fail), forms enviados, usuarios en sesión activa | 10% | 1 | ✅ Docker Desktop Kubernetes |
| ELK funcional: `observability/elk/docker-compose.elk.yml` (ES+Logstash+Kibana) + Filebeat DaemonSet en k8s que envía logs al Logstash | 10% | 1 | ✅ Docker Desktop Kubernetes + docker-compose |
| OWASP ZAP en GHA pipeline | 15% | 2 | ❌ GHA |
| JaCoCo + ampliar unit/integration/E2E (≥5 de cada tipo) | 15% | 2 | ✅ GHA Kind |
| Documentar patrones existentes + Circuit Breaker + Feature Toggle | 10% | 2 | ❌ |
| Alertas en Alertmanager (servicio down → alerta) | 10% | 2 | ✅ Docker Desktop Kubernetes |
| Docs: arquitectura + diagramas + manual de operaciones | 10% | 2 | ❌ |

### Compartido
| Tarea | Sprint |
|---|---|
| User stories + criterios de aceptación en GitHub Projects | 0 |
| docs/change-management.md (proceso, rollback, etiquetado) | 2 |
| Presentación + video 20-30 min (desde máquina de Tomás) | Final |

---

## 4. Estrategia de branching (GitHub Flow)

```
master ──●──────────────────●──────────────────────●──── (protegida)
          │                  │                       │
feat/...──┘  feat/...────────┘  fix/...─────────────┘
```

**Reglas:**
- `master` bloqueada para push directo. Solo entra por PR.
- Cada PR requiere: CI verde (build + tests + Sonar + Trivy) + review del otro integrante.
- Nombres: `feat/<numero-issue>-descripcion`, `fix/<numero-issue>-descripcion`, `chore/...`
- Tags en master: `v1.0.0`, `v1.1.0` — generados automáticamente por semantic-release.
- **Artefacto calificable:** `docs/branching-strategy.md`

---

## 5. Gestión ágil

**Herramienta:** GitHub Projects (tablero Kanban en el repo)
**Columnas:** `Backlog | Sprint 1 | Sprint 2 | In Progress | Review | Done`

**Formato de user story (Issues):**
```
Título: [S1] Como operador quiero ver métricas de latencia en Grafana
Labels: sprint-1, observabilidad, tomas
Criterios de aceptación:
- Dashboard Grafana muestra p50/p95/p99 de cada servicio
- Alerta configurada si p95 > 500ms
- Screenshot en evidence/
```
**Dos iteraciones** Sprint 1 (review 8 jun) + Sprint 2 (review 11 jun) = cumple requisito.

---

## 6. Sprint 0 — Setup y correcciones (Jun 4–5)
> **Meta:** ambos desarrolladores con entorno listo antes de arrancar Sprint 1.

### 6.1 Acciones en GitHub (Juan, 15 min)
1. Agregar a Tomás: `Settings → Collaborators → Add people`
2. Proteger master: `Settings → Branches → Add rule → master` → require PR + CI + 1 review
3. Crear GitHub Projects board con columnas del punto 5
4. Crear Issues de Sprint 1 y Sprint 2 como user stories

### 6.2 Correcciones de bugs (3 bloqueantes del setup report)

**Bug 1 — identity-service puerto incorrecto en dev**
```
Prompt Claude Code:
"En k8s/dev/circleguard-identity-service/deployment.yaml y service.yaml,
cambia containerPort y targetPort de 8083 a 8082. También actualiza el
liveness y readiness probe si apuntan a 8083. No toques stage ni prod."
```

**Bug 2 — GitHub Actions usa Java 17**
```
Prompt Claude Code:
"En .github/workflows/ci-packages.yml:
1. Cambia java-version: '17' a java-version: '21' en TODOS los jobs.
2. En cada job build-*-service, corrige el docker/build-push-action:
   - Cambia context: services/circleguard-XXX-service  →  context: .
   - Agrega: file: services/circleguard-XXX-service/Dockerfile
   Esto es necesario porque los Dockerfiles referencian gradlew y
   settings.gradle.kts del root del monorepo."
```

**Bug 3 — Kafka solo escucha en localhost (rompe contenedores Docker)**
```
Prompt Claude Code:
"En docker-compose.dev.yml, servicio circleguard-kafka, reemplaza el bloque
de environment por:
  KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,INTERNAL://circleguard-kafka:29092
  KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,INTERNAL:PLAINTEXT
  KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,INTERNAL://0.0.0.0:29092
  KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

Luego en application.yml de notification-service y promotion-service,
cambia bootstrap-servers: localhost:9092 a:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:circleguard-kafka:29092}
Esto permite que en docker-compose los contenedores se hablen entre sí,
y en K8s se use la variable de entorno del deployment."
```

### 6.3 Instalación — Juan (máquina de 8 GB)

**JDK 21 (bloqueante)**
```
Prompt Claude Code:
"Instala Temurin JDK 21 en Windows 11 sin desinstalar el JDK 17 actual.
Usa winget si está disponible: winget install EclipseAdoptium.Temurin.21.JDK
Después configura JAVA_HOME para que apunte a 21 y verifica con:
  java -version
  ./gradlew --version  (debe mostrar JVM 21)
Dame los comandos exactos y cómo cambiar JAVA_HOME en variables de entorno
de Windows 11."
```

**Kind (cluster ligero para Juan)**
```
Prompt Claude Code:
"Instala Kind en Windows 11: winget install Kubernetes.kind
Luego crea el cluster de desarrollo:
  kind create cluster --name circleguard
Verifica que kubectl apunta a él: kubectl cluster-info --context kind-circleguard
Crea los 3 namespaces:
  kubectl create namespace circleguard-dev
  kubectl create namespace circleguard-stage
  kubectl create namespace circleguard-prod
Muéstrame la salida de: kubectl get namespaces"
```

**Terraform**
```
Prompt Claude Code:
"Instala Terraform en Windows 11: winget install Hashicorp.Terraform
Verifica: terraform --version
No inicialices nada todavía."
```

### 6.4 Instalación — Tomás (máquina de 16 GB)
```
Prompt Claude Code (Tomás lo corre en SU máquina):
"Usa Docker Desktop Kubernetes en Windows 11 como cluster local de Tomás.
Verifica en Docker Desktop que Kubernetes esté habilitado y luego ejecuta:
  kubectl config use-context docker-desktop
  kubectl get nodes
Crea los 3 namespaces: circleguard-dev, circleguard-stage, circleguard-prod
Verifica con: kubectl get nodes y kubectl get namespaces
Exporta el kubeconfig: kubectl config view --minify --flatten > kubeconfig-tomas.yaml
(Juan necesitará este archivo para apuntar Terraform al cluster de Tomás en
los momentos que necesiten validación de infra completa)"
```

### 6.5 Crear docs/branching-strategy.md
```
Prompt Claude Code:
"Crea docs/branching-strategy.md con:
- Título: Estrategia de Branching — CircleGuard
- Metodología: GitHub Flow (justificación: equipo pequeño + timeline corto)
- Diagrama ASCII del flujo
- Reglas detalladas: protección master, PR + CI + review
- Nombres de ramas: feat/N-desc, fix/N-desc, chore/...
- Ciclo de releases: semantic-release → vX.Y.Z en cada merge a master
- Ambientes: dev (automático), stage (aprobación manual), prod (aprobación manual)
Hazlo en español, profesional."
```

### 6.6 Verificación del middleware local (ambos)
```
Prompt Claude Code:
"Levanta solo Postgres y Redis del docker-compose.dev.yml:
  docker compose -f docker-compose.dev.yml up circleguard-postgres circleguard-redis -d
Verifica que están healthy: docker compose -f docker-compose.dev.yml ps
Luego corre el build completo: ./gradlew build --parallel
Si falla por JDK, muéstrame el error. No arranques nada más todavía."
```

### DoD Sprint 0
- [x] Tomás agregado como colaborador, master protegida — Tomás mergeó PRs #10 y #11
- [x] GitHub Projects board con user stories de Sprint 1 y Sprint 2 — creado con script PowerShell
- [x] 3 bugs corregidos (identity port, GHA java version, Kafka listener) — PR #10
- [x] Juan: JDK 21 instalado, Kind cluster corriendo, 3 namespaces — BUILD SUCCESSFUL 7m 33s
- [x] Tomás: Docker Desktop Kubernetes (`docker-desktop`) corriendo, 3 namespaces — nodo `Ready`, namespaces creados Jun 9
- [x] `./gradlew build --parallel` pasa sin errores de toolchain — BUILD SUCCESSFUL 7m 33s
- [x] `docs/branching-strategy.md` en el repo — PR #11

---

## 7. Sprint 1 — Infraestructura núcleo + Observabilidad (Jun 6–8)
> **Meta:** CI básico verde en GHA + Terraform funcional + 8 servicios en k8s de Tomás + observabilidad visible.

### Juan — Terraform + GHA
1. Estructura `terraform/` con módulos: `namespace`, `microservice`, `middleware`, `observability`
2. Configurar backend HCP Terraform (crear cuenta free + workspace `circleguard-dev`)
3. Módulo `microservice`: Deployment + Service + ConfigMap genérico parametrizable
4. `terraform/environments/dev/main.tf`: instanciar los 6 servicios + namespaces en Kind local
5. Expandir `ci-packages.yml` → renombrar a `ci-cd.yml`:
   - Agregar job `unit-tests` (separado del build)
   - Agregar SonarCloud (`sonarsource/sonarcloud-github-action`)
   - Agregar Trivy (`aquasecurity/trivy-action`)
   - Agregar job `integration-tests` (Kind efímero en GHA runner)
   - Agregar notificación en `on.failure` (GitHub step summary + email)
6. Configurar GitHub Environments (`dev`, `stage`, `prod`) con required reviewers en stage+prod
7. Semantic-release: crear `.releaserc.json`, configurar en GHA para tagear en merge a master

*→ Prompts de Claude Code para estos 7 puntos: se entregan al inicio del Sprint 1*

### Tomás — Observabilidad + Servicios faltantes
1. Crear Dockerfile + k8s manifests para `notification-service` (puerto 8082) y `promotion-service` (8088)
   — usar el mismo patrón multi-stage de auth-service
2. Levantar los 8 servicios en Docker Desktop Kubernetes: validar que todos están `1/1 Running`
3. Crear `observability/helm/` con values overrides
4. Desplegar kube-prometheus-stack: `helm install monitoring prometheus-community/kube-prometheus-stack -f observability/helm/prometheus-values.yaml`
5. Agregar dependencia Micrometer a cada servicio (build.gradle.kts) + habilitar `/actuator/prometheus`
6. Crear ServiceMonitor CRDs para que Prometheus raspe los 8 servicios
7. Desplegar Loki stack: `helm install loki grafana/loki-stack --set promtail.enabled=true`
8. Desplegar Jaeger all-in-one: `kubectl apply -f observability/jaeger/jaeger-all-in-one.yaml`
9. Crear `observability/elk/docker-compose.elk.yml` con Elasticsearch + Logstash + Kibana.
   Crear `observability/elk/filebeat-daemonset.yaml` (DaemonSet en k8s) que recolecta logs
   de los contenedores del cluster y los envía a Logstash. Verificar que los logs de los
   servicios aparecen en Kibana.
10. Crear dashboard Grafana básico (latencia + throughput + errores por servicio)

*→ Prompts de Claude Code para estos 10 puntos: se entregan al inicio del Sprint 1*

### DoD Sprint 1
- [x] `terraform apply` funciona en Kind de Juan — 8 servicios + namespace desplegados (Jun 8)
- [x] HCP Terraform workspace `circleguard-dev` con state remoto visible — backend.tf + .terraform.lock.hcl
- [ ] GHA pipeline verde: build + unit tests + SonarCloud + Trivy por push a master
- [x] Los 8 servicios corriendo `1/1` en Docker Desktop Kubernetes de Tomás (namespace `circleguard-dev`)
- [x] kube-prometheus-stack instalado en Docker Desktop Kubernetes (namespace `monitoring`) con Prometheus, Grafana, Alertmanager, Operator y kube-state-metrics en `Running`
- [x] ServiceMonitors creados para los 8 servicios de `circleguard-dev`
- [x] Evidencia Sprint 1 de Tomás documentada en `evidence/sprint1/tomas-observability-foundation.txt`
- [x] Prometheus raspando métricas de los 8 servicios
- [x] Dashboard Grafana con latencia/throughput visible
- [x] Loki recibiendo logs
- [x] Jaeger mostrando trazas
- [x] Filebeat DaemonSet enviando logs k8s → Kibana muestra logs de al menos 3 servicios
- [ ] Iteración 1 documentada en GitHub Projects (sprint review con screenshots)

---

## 8. Sprint 2 — CI/CD completo + Seguridad + Patrones + Pruebas (Jun 9–11)
> **Meta:** pipeline de 3 ambientes con aprobaciones + seguridad k8s + patrones implementados + pruebas completas.

### Juan — CI/CD multi-ambiente + Seguridad
1. GHA workflow multi-ambiente: `build-test` → `deploy-dev` (auto) → `deploy-stage` (manual) → `deploy-prod` (manual)
2. Terraform para stage y prod: `environments/stage/` y `environments/prod/` + workspaces HCP
3. Terraform apply completo sobre cluster de Tomás (compartir kubeconfig vía secret de GHA)
4. Circuit Breaker + **Retry** Resilience4j en `circleguard-auth-service`:
   - Circuit Breaker: envuelve llamada a identity-service, activa fallback tras N fallos
   - Retry: reintenta la llamada hasta 3 veces con backoff exponencial antes de activar el CB
   - Ambos en la misma clase, misma dependencia `resilience4j-spring-boot3`
5. Feature Toggle en `circleguard-gateway-service`: property `feature.qr-validation.enabled` controla el flujo de validación QR
6. k8s Secrets: reemplazar todos los JWT_SECRET hardcodeados por `secretKeyRef` en los 3 ambientes
7. RBAC: ServiceAccount + Role + RoleBinding por cada servicio en dev/stage/prod
8. TLS: instalar cert-manager, ClusterIssuer autofirmado, Ingress con TLS en namespace dev

### Tomás — Pruebas + Patrones + Documentación
1. OWASP ZAP: agregar `zaproxy/action-full-scan` en GHA apuntando al gateway (usando Kind efímero del runner)
2. JaCoCo: agregar al `build.gradle.kts` raíz, publicar reporte en GHA Artifacts y step summary
3. Ampliar tests a mínimos requeridos:
   - ≥5 unit tests nuevas (auth, identity, gateway como prioridad)
   - ≥5 integration tests entre servicios relacionados (auth↔identity, gateway↔auth)
   - ≥5 E2E tests: login → lookup → form submit → status check → logout
4. Locust: ampliar `tests/performance/locustfile.py` con escenarios de carga + estrés documentados
5. Documentar patrones existentes en `docs/design-patterns.md`:
   - API Gateway (gateway-service)
   - Repository pattern (identity, auth)
   - Event-Driven / Observer (Kafka topics)
   - Circuit Breaker (implementado por Juan en Sprint 2)
   - **Retry** (implementado por Juan en Sprint 2, junto al CB)
   - Feature Toggle (implementado por Juan en Sprint 2)
6. git-cliff: configurar `cliff.toml`, generar `CHANGELOG.md` desde commits convencionales
7. `docs/change-management.md`: proceso formal, plan de rollback, etiquetado de releases
8. Alertas Alertmanager: alerta si algún pod está down > 2 minutos
9. Dashboard Grafana final: agregar panel de métricas de negocio (logins/min, forms activos)

### DoD Sprint 2
- [ ] Pipeline de 3 ambientes con aprobaciones funciona de punta a punta
- [ ] Terraform apply cubre los 3 ambientes (dev auto, stage/prod con approval)
- [ ] Tags semánticos en repo: mínimo v1.0.0 y v1.1.0
- [ ] CHANGELOG.md auto-generado y coherente con los tags
- [ ] OWASP ZAP corre en pipeline y genera reporte (sin críticos no documentados)
- [ ] JaCoCo reporta cobertura ≥70% en auth, identity, gateway
- [ ] ≥5 E2E, ≥5 unit tests nuevas, ≥5 integration tests pasando en GHA
- [ ] Circuit Breaker funcional (test: bajar identity-service → auth devuelve fallback, no 500)
- [ ] Retry funcional (test: fallo transitorio → auth reintenta antes de activar CB)
- [ ] Feature Toggle funcional (cambiar property → comportamiento cambia)
- [ ] JWT_SECRET en k8s Secrets (no hardcodeado en ningún deployment.yaml)
- [ ] RBAC aplicado en dev/stage/prod (kubectl get rolebindings no vacío)
- [ ] TLS activo en Ingress (curl https:// devuelve respuesta, no error SSL)
- [ ] `docs/design-patterns.md` y `docs/change-management.md` completos
- [ ] Alertas Alertmanager configuradas y disparándose en prueba
- [ ] Iteración 2 documentada en GitHub Projects

---

## 9. Sprint Final — Entrega (Jun 12)

| Entregable | Responsable | Herramienta |
|---|---|---|
| Diagrama arquitectura general (servicios + observabilidad + flujo CI/CD) | Juan | draw.io / Mermaid |
| Diagrama Terraform (`terraform graph \| dot -Tsvg`) — módulos, recursos, dependencias | Juan | CLI + draw.io |
| Costos de infraestructura (local = $0; análisis hipotético cloud documentado) | Juan | markdown |
| Manual de operaciones (cómo levantar, monitorear, hacer rollback) | Tomás | markdown |
| Análisis de pruebas (Locust, JaCoCo, ZAP) | Tomás | markdown |
| Presentación 30 min: arq → CI/CD → app → dashboards → pruebas → lecciones | Ambos | Canva / Slides |
| Video 20-30 min grabado con **OBS Studio** (no Win+G — graba pantalla completa) | Ambos | OBS |

---

## 10. Riesgos y mitigaciones

| # | Riesgo | Mitigación |
|---|---|---|
| R1 | Juan (8 GB) no puede correr el stack completo | Kind solo para validar IaC/RBAC/TLS. Tests de integración completa → GHA runner o cluster de Tomás |
| R2 | promotion-service usa Neo4j — puede tener bugs ocultos | Probar primero en docker-compose. En k8s dar `initialDelaySeconds: 180` y `failureThreshold: 10` |
| R3 | notification-service (8082) = identity-service (8082) en stage/prod | Para port-forward usar `kubectl port-forward svc/notification 18082:8082`. En k8s no hay conflicto |
| R4 | Terraform apply necesita acceso al cluster de Tomás | Tomás exporta kubeconfig → Juan lo agrega como secret `KUBE_CONFIG` en GitHub. GHA apply usa ese secret |
| R5 | SonarCloud requiere token configurado | Crear cuenta en sonarcloud.io, generar token, guardarlo en GitHub Secrets como `SONAR_TOKEN` antes del Sprint 1 |
| R6 | HCP Terraform: crear cuenta antes del Sprint 1 | app.terraform.io → crear org → crear workspaces dev/stage/prod → copiar token en `TF_API_TOKEN` en GitHub Secrets |
| R7 | Código "arreglado" pero no reconstruido (error del Taller 2) | Regla de oro: ningún cambio cuenta hasta GHA verde → `kubectl rollout status` en el ambiente objetivo |
| R8 | Locust + OWASP ZAP en GHA necesitan servicios corriendo | Usar Kind efímero con servicios desplegados, o apuntar al cluster Docker Desktop Kubernetes de Tomás con port-forward en el runner |
| R9 | Deadline Jun 12 muy ajustado | Si algo no llega: prioridad Terraform(20%) > CI/CD(15%) > Pruebas(15%) > Obs(10%) > Ágil(10%). Seguridad(5%) y Patrones(10%) tienen "efecto demostrable mínimo" |

---

## 11. Estructura de carpetas objetivo

```
circle-guard-public/
├── .github/workflows/
│   └── ci-cd.yml                  ← reemplaza ci-packages.yml (pipeline completo)
├── .releaserc.json                ← config semantic-release
├── cliff.toml                     ← config git-cliff
├── CHANGELOG.md                   ← auto-generado
├── terraform/
│   ├── modules/
│   │   ├── namespace/             ← k8s namespace + RBAC básico
│   │   ├── microservice/          ← Deployment + Service + ConfigMap genérico
│   │   ├── middleware/            ← Postgres, Neo4j, Kafka, Redis en k8s
│   │   └── observability/         ← Helm releases Prometheus/Loki/Jaeger
│   └── environments/
│       ├── dev/main.tf + terraform.tfvars
│       ├── stage/main.tf + terraform.tfvars
│       └── prod/main.tf + terraform.tfvars
├── observability/
│   ├── helm/                      ← values overrides para kube-prometheus-stack, loki
│   ├── elk/
│   │   ├── docker-compose.elk.yml ← Elasticsearch + Logstash + Kibana
│   │   └── filebeat-daemonset.yaml← DaemonSet en k8s que envía logs a Logstash
│   └── dashboards/                ← JSONs dashboards Grafana exportados
├── k8s/
│   ├── dev/   (8 servicios + middleware + namespace.yaml)
│   ├── stage/ (ídem)
│   └── prod/  (ídem)
├── services/
│   └── (código + Micrometer + Counters negocio + Resilience4j CB+Retry en auth + Feature Toggle en gateway)
├── tests/
│   ├── e2e/   (≥5 flows completos)
│   └── performance/locustfile.py
├── docs/
│   ├── branching-strategy.md      ← Sprint 0
│   ├── design-patterns.md         ← Sprint 2 (CB + Retry + Feature Toggle + patrones existentes)
│   ├── change-management.md       ← Sprint 2
│   ├── architecture.md            ← Sprint Final (incluye nota SonarCloud = SonarQube)
│   ├── terraform-architecture.svg ← Sprint Final (generado con terraform graph)
│   └── operations-manual.md       ← Sprint Final
├── pipelines/                     ← Jenkinsfiles conservados como documentación
└── evidence/                      ← screenshots por sprint (convención de Tomás adoptada)
```
