---
# CircleGuard — Architecture Documentation

## 1. System Overview

CircleGuard is a microservices-based system for security guard management, built on Spring Boot 4 / Java 21 and deployed on Kubernetes. The system consists of 8 microservices orchestrated via an API Gateway, with a full observability stack, automated CI/CD pipelines, and infrastructure-as-code provisioning.

**Repository:** github.com/JRuiz1601/circle-guard-public  
**Team:** Juan Ruiz (infrastructure, CI/CD, security) · Tomás Quintero (observability, testing)  
**Environments:** dev (Kind/Docker Desktop) · stage · prod

---

## 2. Microservices Architecture

### Services and Ports

| Service | Port | Responsibility |
|---|---|---|
| circleguard-gateway-service | 8080 | API Gateway — routes requests, validates QR tokens, JWT auth |
| circleguard-auth-service | 8081 | Authentication — login, JWT issuance, LDAP integration |
| circleguard-identity-service | 8082 | Identity mapping — anonymous identity vault |
| circleguard-form-service | 8083 | Form management — questionnaires and health surveys |
| circleguard-file-service | 8084 | File storage — attachments and uploads |
| circleguard-dashboard-service | 8085 | Analytics — health board, time series, summaries |
| circleguard-notification-service | 8082* | Notifications — email, SMS, push via Kafka listeners |
| circleguard-promotion-service | 8088 | Promotion management — Neo4j graph, Kafka events |

*notification-service shares port 8082 with identity-service; no conflict in Kubernetes (separate pod IPs). Use port 18082 for local port-forwarding.

### Technology Stack

| Layer | Technology |
|---|---|
| Services | Spring Boot 4 / Java 21 / Gradle 8.14 (monorepo) |
| Databases | PostgreSQL 16, Neo4j 5.26 |
| Messaging | Apache Kafka 7.6 + Zookeeper |
| Cache / LDAP | Redis 7.2, OpenLDAP 1.5 |
| Containers | Docker / Docker Compose v2 |
| Orchestration | Kubernetes (Kind dev · Docker Desktop stage/prod) |
| Service Mesh | None (plain Kubernetes services) |

### Inter-service Communication

- **Synchronous:** REST over HTTP (gateway → auth → identity)
- **Asynchronous:** Apache Kafka topics (auth, form, gateway → notification, promotion)
- **Service discovery:** Kubernetes DNS (`circleguard-<service>:<port>`)

---

## 3. Infrastructure Architecture

### Kubernetes Namespaces

| Namespace | Purpose |
|---|---|
| circleguard-dev | Development environment — auto-deploy on master push |
| circleguard-stage | Staging environment — requires manual approval |
| circleguard-prod | Production environment — requires manual approval |
| monitoring | Observability stack (Prometheus, Grafana, Loki, Alertmanager) |
| cert-manager | TLS certificate management |
| ingress-nginx | Nginx Ingress Controller |

### Terraform Modules

Infrastructure is provisioned via modular Terraform with remote state on HCP Terraform (org: circleguard-icesi).

| Module | Resources |
|---|---|
| `namespace` | `kubernetes_namespace` |
| `microservice` | `kubernetes_deployment`, `kubernetes_service`, `kubernetes_config_map` |
| `observability` | `helm_release` (kube-prometheus-stack, loki-stack) |

Each environment (dev/stage/prod) instantiates all modules with environment-specific variables. State is stored remotely on HCP Terraform free tier.

**Terraform architecture diagram:** `docs/terraform-architecture.dot` (render at dreampuf.github.io/GraphvizOnline)

### Kubernetes Security

- **Secrets:** `circleguard-secrets` k8s Secret per namespace — JWT keys stored as base64 (`secretKeyRef`, not hardcoded)
- **RBAC:** `circleguard-app-sa` ServiceAccount + `circleguard-app-role` Role (get/list on configmaps and secrets) per namespace
- **TLS:** cert-manager v1.15.0 with self-signed ClusterIssuer (`selfsigned-issuer`), Certificate valid until Sep 2026, nginx Ingress on `circleguard.local`

---

## 4. CI/CD Pipeline

### Workflows

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci-packages.yml` | push to master, PR (services/**, .github/**) | 8 builds + GHCR push + deploy dev/stage/prod |
| `quality.yml` | push to master, PR | SonarCloud analysis, Trivy filesystem scan, JaCoCo coverage, OWASP ZAP |
| `release.yml` | push to master | semantic-release (tag + GitHub Release) |

### Deployment Flow

```
Code push → CI builds (8 parallel) → GHCR push
  → deploy-dev (automatic)
  → deploy-stage (manual approval required)
  → deploy-prod (manual approval required)
```

### Container Registry

All images published to GHCR: `ghcr.io/jruiz1601/circleguard-<service>:<run_number>` and `:<latest>`

### Versioning

Automated semantic versioning via `semantic-release`:
- `feat:` commit → minor bump (v1.1.0)
- `fix:` commit → patch bump (v1.0.1)
- `BREAKING CHANGE` → major bump (v2.0.0)

---

## 5. Observability Stack

Deployed on the `monitoring` namespace of Docker Desktop Kubernetes (Tomás's cluster).

| Tool | Purpose | Access |
|---|---|---|
| Prometheus | Metrics scraping (8 ServiceMonitors) | port-forward 9090 |
| Grafana | Dashboards (Sprint 1 technical + Sprint 2 business) | port-forward 3000 |
| Loki + Promtail | Log aggregation from all pods | via Grafana |
| Jaeger | Distributed tracing | port-forward 16686 |
| Elasticsearch + Kibana | Log search and analysis | docker-compose elk |
| Filebeat DaemonSet | Log shipping from k8s pods → Logstash | k8s DaemonSet |
| Alertmanager | Alert routing | Prometheus Operator |

### Business Metrics (Micrometer)

| Metric | Label | Service |
|---|---|---|
| `circleguard_auth_logins_total` | `outcome=success\|failure\|error` | auth |
| `circleguard_form_questionnaires_created_total` | — | form |
| `circleguard_form_surveys_submitted_total` | — | form |
| `circleguard_gateway_qr_validations_total` | `result=allowed\|denied\|invalid` | gateway |

### Alertmanager Rules

- `CircleGuardPodNotReady` — fires if a `circleguard-*` pod is Running but not Ready for >2 min
- `CircleGuardPodDown` — fires if a `circleguard-*` pod is in Failed or Unknown state for >2 min

---

## 6. Design Patterns

### Implemented Patterns

| Pattern | Category | Service | Implementation |
|---|---|---|---|
| Circuit Breaker | Resiliencia | auth-service | `@CircuitBreaker` (Resilience4j) on `IdentityClient.getAnonymousId()` — opens after 50% failure rate over 5 calls, stays open 10s |
| Retry | Resiliencia | auth-service | `@Retry` (Resilience4j) on same method — 3 attempts, 500ms backoff before Circuit Breaker counts the failure |
| Feature Toggle | Configuración | gateway-service | `@ConditionalOnProperty(name="feature.diagnostic-header.enabled")` — `DiagnosticHeaderFilter` active in dev/stage, inactive in prod via env var |
| API Gateway | Estructural | gateway-service | Single entry point for all client requests — JWT validation, QR token verification, request routing |
| Repository | Datos | identity, form | Spring Data JPA repositories abstracting PostgreSQL persistence |
| Event-Driven | Mensajería | auth, form, notification, promotion | Kafka topics decouple producers (auth, form) from consumers (notification, promotion) |

### Existing Patterns (identified in original codebase)

- **Microservices:** 8 independently deployable services with bounded contexts
- **Service Locator:** Kubernetes DNS for inter-service discovery
- **Health Check:** Spring Actuator `/actuator/health` on all services

---

## 7. Security Architecture

### Continuous Scanning

- **Trivy** — filesystem vulnerability scan on every push to master; results in GitHub Security tab (SARIF)
- **OWASP ZAP** — DAST scan against gateway-service in ephemeral GHA environment on every push

### Code Quality

- **SonarCloud** — static analysis on every push and PR; Quality Gate: no new blocker issues, duplications < 30%
- **JaCoCo** — coverage threshold 70% for auth, identity, gateway services

### Note on Secrets Management

For academic deployment, JWT secrets are stored as base64-encoded Kubernetes Secrets (`circleguard-secrets`). In a production environment, this would be replaced with HashiCorp Vault, Sealed Secrets, or External Secrets Operator.

---

## 8. Infrastructure Costs

All environments run locally — infrastructure cost is **$0**.

Hypothetical cloud estimate (AWS ap-south-1, 3 environments):

| Resource | Config | Monthly |
|---|---|---|
| EKS Cluster (1) | 2x t3.medium nodes | ~$140 |
| RDS PostgreSQL | db.t3.micro Multi-AZ | ~$35 |
| ElastiCache Redis | cache.t3.micro | ~$15 |
| MSK Kafka | kafka.t3.small (2 brokers) | ~$90 |
| ECR (GHCR equivalent) | 8 images, ~500MB each | ~$5 |
| **Total estimate** | | **~$285/month** |

HCP Terraform: free tier (up to 500 resources, remote state).  
SonarCloud: free tier (unlimited public repos).

---

## 9. Operations Manual

### Prerequisites

```bash
# Juan's cluster (Kind)
kind create cluster --name circleguard
kubectl create namespace circleguard-dev

# Tomás's cluster (Docker Desktop)
kubectl config use-context docker-desktop
kubectl create namespace circleguard-dev
kubectl create namespace monitoring
```

### Deploy via Terraform

```bash
export TF_API_TOKEN=<hcp-terraform-token>
cd terraform/environments/dev
terraform init
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=jruiz1601 \
  --docker-password=<CR_PAT> \
  -n circleguard-dev
kubectl apply -f k8s/dev/secret.yaml
kubectl apply -f k8s/dev/rbac.yaml
kubectl apply -f k8s/dev/tls/
terraform apply -auto-approve
```

### Observability Stack

```bash
# kube-prometheus-stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install monitoring prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace \
  -f observability/helm/prometheus-values.yaml

# Loki
helm repo add grafana https://grafana.github.io/helm-charts
helm install loki grafana/loki-stack \
  -n monitoring \
  -f observability/helm/loki-values.yaml

# Apply dashboards and alerts
kubectl apply -f observability/dashboards/
kubectl apply -f observability/alerts/
kubectl apply -f observability/jaeger/jaeger-all-in-one.yaml
```

### ELK Stack

```bash
cd observability/elk
docker compose -f docker-compose.elk.yml up -d
kubectl apply -f filebeat-daemonset.yaml
```

### Port Forwards for local access

```bash
kubectl port-forward svc/monitoring-grafana 3000:80 -n monitoring
kubectl port-forward svc/monitoring-kube-prometheus-stack-prometheus 9090 -n monitoring
kubectl port-forward svc/jaeger-query 16686 -n monitoring
```

### Rollback procedure

```bash
# Rollback a specific service to previous image
kubectl set image deployment/circleguard-auth-service \
  circleguard-auth-service=ghcr.io/jruiz1601/circleguard-auth-service:<previous-tag> \
  -n circleguard-dev
kubectl rollout status deployment/circleguard-auth-service -n circleguard-dev

# Full environment rollback via Terraform
terraform apply -var="image_tag=<previous-run-number>" -auto-approve
```

