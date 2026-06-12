# Changelog

All notable changes to CircleGuard will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

---

## [Unreleased]

---

## [1.8.0] - 2026-06-11

### Added
- Sprint 2 — quality gates: JaCoCo coverage reports, SonarCloud analysis results, and OWASP ZAP security scan evidence ([#43](https://github.com/JRuiz1601/circle-guard-public/pull/43))
- Sprint 2 — performance tests: Locust load and stress test reports for gateway, auth, and promotion services ([#44](https://github.com/JRuiz1601/circle-guard-public/pull/44))
- Sprint 2 — business observability: Grafana dashboard for business metrics (active guards, forms submitted, promotions) with Alertmanager alert rules ([#45](https://github.com/JRuiz1601/circle-guard-public/pull/45))
- Unit tests for `IdentityVaultService`, `IdentityVaultController`, and `JwtAuthenticationFilter` in identity-service
- Unit tests for `HealthStatusController` and `HealthStatusReevaluation` in promotion-service
- `tests/performance/README.md` documenting Locust test setup and execution

---

## [1.7.0] - 2026-06-11

### Added
- Terraform observability module (`terraform/modules/observability/`) with variables, outputs, and Prometheus/Grafana resources; wired into `terraform/environments/dev/main.tf` ([#42](https://github.com/JRuiz1601/circle-guard-public/pull/42))
- `docs/architecture.md` — full system architecture document covering microservices, infrastructure, CI/CD, observability, security, and resilience patterns
- `docs/circleguard_terraform_architecture.svg` and `docs/terraform-architecture.png` — visual infrastructure diagrams generated from Terraform modules
- `docs/design-patterns.md` — catalog of patterns implemented (Circuit Breaker, Feature Toggle, RBAC, TLS, Gateway)
- `docs/change-management.md` — change management process for the project

---

## [1.6.0] - 2026-06-11

### Added
- TLS termination via cert-manager (self-signed `ClusterIssuer`) and nginx Ingress controller for the `circleguard-dev` namespace ([#40](https://github.com/JRuiz1601/circle-guard-public/pull/40), [#41](https://github.com/JRuiz1601/circle-guard-public/pull/41))
- Ingress manifest with TLS secret (`circleguard-tls`) routing external traffic to gateway-service at `circleguard.local`

---

## [1.5.0] - 2026-06-11

### Added
- RBAC manifests for all three environments (dev / stage / prod): `ServiceAccount`, `Role`, and `RoleBinding` resources scoped to each namespace ([#39](https://github.com/JRuiz1601/circle-guard-public/pull/39))
- Least-privilege roles granting only `get`, `list`, `watch` on pods and secrets within each namespace

---

## [1.4.1] - 2026-06-11

### Fixed
- Hardcoded `JWT_SECRET` value in Kubernetes deployment manifests replaced with `secretKeyRef` referencing the `circleguard-secrets` Secret object across dev, stage, and prod environments ([#38](https://github.com/JRuiz1601/circle-guard-public/pull/38))

---

## [1.4.0] - 2026-06-11

### Added
- Feature Toggle pattern in gateway-service using `@ConditionalOnProperty`: `diagnostic-header` feature exposes an `X-Diagnostic` response header, toggled via `features.diagnostic-header.enabled` property ([#37](https://github.com/JRuiz1601/circle-guard-public/pull/37))
- CLAUDE.md protocol update: post-PR sync procedure added to session workflow ([#36](https://github.com/JRuiz1601/circle-guard-public/pull/36))

---

## [1.3.0] - 2026-06-11

### Added
- Resilience4j Circuit Breaker and Retry patterns in auth-service: annotated fallback methods on identity-service calls, configurable via `application.yml` ([#35](https://github.com/JRuiz1601/circle-guard-public/pull/35))
- Sprint 1 and Sprint 2 evidence screenshots and plan status update ([#34](https://github.com/JRuiz1601/circle-guard-public/pull/34))

---

## [1.2.0] - 2026-06-10

### Added
- GitHub Environments-based deploy jobs in CI/CD pipeline: automatic deploy to `dev`, manual approval gates for `stage` and `prod` ([#33](https://github.com/JRuiz1601/circle-guard-public/pull/33))

---

## [1.1.1] - 2026-06-10

### Fixed
- SonarCloud Quality Gate no longer blocks the pipeline on failure — scan runs as informational step so CI remains green while code quality is tracked ([#32](https://github.com/JRuiz1601/circle-guard-public/pull/32))

---

## [1.1.0] - 2026-06-10

### Added
- SonarCloud static analysis and Trivy container vulnerability scanner integrated into the quality pipeline (`quality.yml`) ([#30](https://github.com/JRuiz1601/circle-guard-public/pull/30))
- `notification-service` and `promotion-service` added to the GHCR build-and-push pipeline (`ci-packages.yml`) ([#28](https://github.com/JRuiz1601/circle-guard-public/pull/28))
- Kubernetes manifests for notification-service and promotion-service deployed to stage and prod environments ([#29](https://github.com/JRuiz1601/circle-guard-public/pull/29))

---

## [1.0.0] - 2026-06-09

### Added
- Observability foundation: Prometheus, Grafana, Loki, Jaeger, and Alertmanager Helm chart values under `observability/helm/`; ELK stack configuration under `observability/elk/` ([#22](https://github.com/JRuiz1601/circle-guard-public/pull/22))
- GitHub Actions release workflow (`release.yml`) as an independent workflow decoupled from path filters ([#25](https://github.com/JRuiz1601/circle-guard-public/pull/25))

### Fixed
- Removed `@semantic-release/git` plugin to prevent CHANGELOG commit loop; changelog management delegated to git-cliff via PR ([#24](https://github.com/JRuiz1601/circle-guard-public/pull/24))
- Release workflow trigger corrected to run independently without depending on `ci-packages.yml` path filters ([#25](https://github.com/JRuiz1601/circle-guard-public/pull/25))

---

[Unreleased]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.8.0...HEAD
[1.8.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.7.0...v1.8.0
[1.7.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.4.1...v1.5.0
[1.4.1]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/JRuiz1601/circle-guard-public/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/JRuiz1601/circle-guard-public/releases/tag/v1.0.0
