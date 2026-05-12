# Changelog

All notable changes to CircleGuard will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

---

## [Unreleased]

### Added
- CI/CD pipeline structure (dev, stage, prod)
- Kubernetes manifests for 6 microservices
- Unit, integration, E2E, and performance tests
- Automated Release Notes generation script (`scripts/generate-release-notes.sh`) y CI Testcontainers (`scripts/testcontainers-ci-env.sh`)
- Pipelines `Jenkinsfile.master` (prod) para los 6 microservicios
