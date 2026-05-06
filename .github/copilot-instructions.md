# CircleGuard — Copilot Agent Instructions

## Project Context
This is a Java/Spring Boot microservices monorepo called **CircleGuard**.
- **Build tool:** Gradle multi-module (Kotlin DSL — `build.gradle.kts`)
- **Language:** Java 17 — ALL source files are in `src/main/java` and `src/test/java` (NOT Kotlin)
- **Architecture:** Standard Spring Boot layered — `controller → service → repository → model`
- **GitHub user:** JRuiz1601
- **Registry:** `ghcr.io/jruiz1601`

## Infrastructure (docker-compose.dev.yml)
- **PostgreSQL 16** — host: `localhost`, port: `5432`, DB: `circleguard`, user: `admin`, password: `password`
- **Neo4j 5.26** — bolt: `7687`, user: `neo4j`, password: `password`
- **Kafka** — port: `9092` (with Zookeeper on `2181`)
- **Redis 7.2** — port: `6379`
- **OpenLDAP** — port: `389`, domain: `circleguard.edu`, admin password: `admin`

## Microservices in Scope (6 selected)
1. `circleguard-gateway-service` — API Gateway (Spring Cloud Gateway)
2. `circleguard-auth-service` — Authentication, JWT tokens, Spring Security
3. `circleguard-identity-service` — User identity management
4. `circleguard-form-service` — Form management
5. `circleguard-file-service` — File storage
6. `circleguard-dashboard-service` — Dashboard and reports

The other 2 services (`notification-service`, `promotion-service`) are OUT OF SCOPE — do not modify them.

## CI/CD Stack
- **Jenkins** running in Docker locally (Declarative Pipeline syntax ONLY)
- **Docker** + **GitHub Container Registry** (`ghcr.io/jruiz1601`)
- **Kubernetes** via **Minikube**, with 3 namespaces:
  - `circleguard-dev` (development)
  - `circleguard-stage` (staging)
  - `circleguard-prod` (production)
- **SonarQube** available at `http://localhost:9000`

## Testing Stack
- **Unit tests:** JUnit 5 + Mockito → `src/test/java`, `@ExtendWith(MockitoExtension.class)`
- **Integration tests:** Spring Boot Test + MockMvc + Testcontainers → `src/test/java`
- **E2E tests:** RestAssured → `/tests/e2e/`
- **Performance tests:** Locust (Python 3) → `/tests/performance/`

## File & Folder Conventions
```
/pipelines/
  {service-name}/
    Jenkinsfile.dev
    Jenkinsfile.stage
    Jenkinsfile.master
/k8s/
  dev/
    {service-name}/
      deployment.yaml
      service.yaml
  stage/
    {service-name}/
      deployment.yaml
      service.yaml
  prod/
    {service-name}/
      deployment.yaml
      service.yaml
/tests/
  unit/           ← (tests live inside each service src/test/java)
  integration/    ← (tests live inside each service src/test/java)
  e2e/
    {service-name}Test.java
  performance/
    locustfile.py
/scripts/
  generate-release-notes.sh
```

## Critical Rules — READ BEFORE WRITING ANY CODE

### ✅ ALWAYS
- Use **Java** for all test files (not Kotlin)
- Use `@ExtendWith(MockitoExtension.class)` for unit tests (never `@SpringBootTest` in unit tests)
- Use **Testcontainers** for any integration test that needs a real database
- Use **Declarative Jenkinsfile** syntax: `pipeline { agent any stages { stage('name') { steps { ... } } } }`
- Tag Docker images as: `ghcr.io/jruiz1601/circleguard-{service-name}:${BUILD_NUMBER}`
- Use `credentials('GHCR_TOKEN')` and `credentials('GITHUB_USER')` for Jenkins secrets
- Use `kubectl apply -f` for K8s deployments, with `--namespace=circleguard-{env}`
- Follow **Conventional Commits**: `feat:`, `fix:`, `chore:`, `test:`, `docs:`
- Add `imagePullPolicy: Always` in K8s Deployment specs
- Use `namespace: circleguard-dev/stage/prod` in all K8s manifests

### ❌ NEVER
- Do NOT create a frontend, mobile app, or UI
- Do NOT modify `docker-compose.dev.yml`
- Do NOT create new microservices
- Do NOT use `@SpringBootTest` in unit tests
- Do NOT hardcode passwords, tokens, or secrets anywhere
- Do NOT use Spring WebFlux unless already present in the service
- Do NOT use Lombok unless already imported in the service's `build.gradle.kts`
- Do NOT use Kotlin in test files
- Do NOT modify `notification-service` or `promotion-service`
- Do NOT use `latest` tag for Docker images in K8s manifests (always use `${BUILD_NUMBER}` or a specific tag)

## Jenkinsfile Stage Structure

### Dev Pipeline (Jenkinsfile.dev)
```
Checkout → Build (./gradlew :services:{name}:build) → Unit Tests → Docker Build → Push to ghcr.io → Deploy to circleguard-dev
```

### Stage Pipeline (Jenkinsfile.stage)
```
Checkout → Build → Unit Tests → Docker Build → Push to ghcr.io → Deploy to circleguard-stage → Integration Tests
```

### Master Pipeline (Jenkinsfile.master)
```
Checkout → Build → Unit Tests → Docker Build → Push to ghcr.io → Deploy to circleguard-stage → Integration Tests → System/E2E Tests → Deploy to circleguard-prod → Generate Release Notes
```

## Release Notes Script
The script `/scripts/generate-release-notes.sh` must:
1. Read `git log` from the last tag to HEAD
2. Group commits by type: `feat`, `fix`, `chore`, `test`
3. Output a markdown file `RELEASE_NOTES.md` with the version and date
4. Append to `CHANGELOG.md`
