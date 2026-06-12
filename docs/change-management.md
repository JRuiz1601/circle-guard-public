# CircleGuard - Change management

Fecha: 2026-06-11

Este documento formaliza como CircleGuard gestiona cambios durante Sprint 2 y entrega final: ramas, PR, puertas de calidad, versionado, release notes y rollback.

## Alcance

Aplica a cambios de codigo, observabilidad, pruebas, documentacion, Kubernetes, Terraform y workflows de GitHub Actions. El Sprint 2 se organiza en dos tracks: calidad/observabilidad/documentacion y CI-CD/seguridad/resiliencia.

## GitHub Flow real del proyecto

CircleGuard usa GitHub Flow sobre `master`:

1. Crear una rama corta desde `master`.
2. Implementar cambios por bloque funcional.
3. Mantener commits convencionales.
4. Abrir PR hacia `master`.
5. Esperar CI verde y revision del companero.
6. Mergear solo cuando el PR tenga evidencia suficiente.
7. Dejar que semantic-release genere version/tag cuando corresponda.

Rama de trabajo del cierre de calidad y observabilidad:

- `feat/sprint2-tomas-quality-observability`

Antes de abrir PR, la rama debe estar sincronizada con `origin/master`:

- `git fetch --all --prune`
- `git merge origin/master`

## Pull request hacia master

El PR de Tomas debe incluir:

- Resumen de cambios de calidad, observabilidad y documentacion.
- Lista de archivos creados/modificados.
- Evidencia en `evidence/sprint2/`.
- Comandos de validacion ejecutados.
- Riesgos o validaciones pendientes, especialmente ejecuciones que dependan de GitHub Actions o cluster local.
- Separacion clara entre cambios de calidad/observabilidad y cambios de CI-CD/seguridad/resiliencia.

No se debe hacer push directo a `master`.

## CI como quality gate

El workflow `.github/workflows/quality.yml` actua como puerta de calidad del PR:

- `jacoco`: ejecuta `./gradlew jacocoSprint2Coverage --continue` y exige cobertura minima de 70% en auth, identity y gateway.
- `sonarcloud`: ejecuta analisis SonarCloud.
- `zap`: construye gateway, levanta gateway + Redis efimero y ejecuta `zaproxy/action-full-scan@v0.13.0` contra `http://127.0.0.1:8080`.
- `trivy`: ejecuta escaneo de vulnerabilidades y publica SARIF.

Ademas, el repo tiene `ci-packages.yml` para build/package de imagenes y workflows de release/versionado. Los resultados de CI deben quedar verdes antes del merge final.

## Revision del companero

La revision debe comprobar:

- Que el alcance de Tomas no modifica responsabilidades de Juan.
- Que las rutas y evidencia referenciadas existen.
- Que los documentos describen estado real y no planes obsoletos.
- Que los cambios de observabilidad no rompen nombres de metricas usados por Prometheus/Grafana.
- Que el PR incluye comandos y resultados suficientes para reproducir la validacion.

Si la revision pide cambios, se corrigen en la misma rama y se vuelve a esperar CI verde.

## Versionado semantico y tags

CircleGuard usa conventional commits y semantic-release para versionado:

- `feat:` genera version minor.
- `fix:` genera version patch.
- Cambios incompatibles deben marcar breaking change.
- Cambios puramente documentales pueden usar `docs:`.
- Cambios de pruebas pueden usar `test:`.
- Cambios de CI pueden usar `ci:`.

El repo ya contiene `.releaserc.json`, `CHANGELOG.md` y `cliff.toml`. En `master` existen tags generados como `v1.0.0`, `v1.1.0`, `v1.1.1`, `v1.2.0`, `v1.3.0`, `v1.4.0`, `v1.4.1`, `v1.5.0`, `v1.6.0` y `v1.7.0`.

## Changelog y release notes

La fuente primaria del changelog es el historial de commits convencionales. Para un release Sprint 2, las notas deben separar:

- Calidad y pruebas: JaCoCo, unit/integration/E2E, OWASP ZAP, Locust.
- Observabilidad: Alertmanager, PrometheusRule, dashboard Grafana, metricas de negocio.
- Infraestructura y CI/CD: environments, approvals, Secrets, RBAC, TLS, Circuit Breaker, Retry y Feature Toggle.
- Documentacion: `docs/design-patterns.md`, `docs/change-management.md`, plan y bitacora.

Antes del release, revisar que `CHANGELOG.md` no mezcle autoria de tracks: lo de Juan debe quedar como infra/CI/CD/seguridad; lo de Tomas como calidad/observabilidad/documentacion.

## Rollback tecnico

Rollback de codigo por PR:

- Identificar el PR que introdujo el cambio.
- Usar revert del PR en GitHub o `git revert -m 1 <merge_commit>` si fue merge commit.
- Abrir PR de rollback hacia `master`.
- Ejecutar CI y validar que vuelve la version estable.

Rollback de deployment/image:

- Volver a desplegar la imagen estable anterior desde GHCR.
- En Kubernetes, actualizar el tag de imagen o restaurar el manifiesto previo.
- Ejecutar `kubectl rollout status` para confirmar que los pods quedan ready.
- Si el cambio esta en gateway/auth/form, revisar endpoints `/actuator/health` y `/actuator/prometheus`.

Rollback de manifests:

- Revertir el commit que modifico `k8s/`, `observability/` o `terraform/`.
- Para manifiestos Kubernetes, aplicar la version estable con `kubectl apply -f`.
- Para dashboards/alerts, restaurar el ConfigMap o `PrometheusRule` anterior y confirmar en Grafana/Prometheus.
- Para Terraform, revisar plan antes de aplicar cualquier rollback y no destruir recursos compartidos sin aprobacion.

## Rollback operativo

Comunicacion:

- Avisar al equipo que el cambio queda en rollback.
- Indicar impacto: servicio, ambiente, ventana de tiempo y usuario afectado.
- Registrar causa probable y enlace al PR/evidencia.

Revision de metricas y logs:

- Grafana: revisar latencia, errores, logins/min, formularios/min y validaciones QR/min.
- Prometheus/Alertmanager: confirmar si hay alertas `CircleGuardPodNotReady` o `CircleGuardPodDown`.
- Loki/ELK: revisar logs de gateway, auth, form, promotion y notification.
- Jaeger si aplica: revisar trazas de flujos login -> identity, form -> promotion, gateway -> Redis.

Restauracion de version estable:

- Confirmar que los pods quedan `Ready`.
- Confirmar que endpoints criticos responden.
- Confirmar que las metricas de negocio vuelven a aparecer en Prometheus.
- Registrar en bitacora y evidencia el cierre del rollback.

## Criterios de aceptacion de un cambio

Un cambio esta listo para merge cuando:

- Tiene PR hacia `master`.
- CI relevante esta verde.
- El companero reviso o aprobo.
- La evidencia esta en `evidence/sprint2/` si es parte de Sprint 2.
- La bitacora refleja decisiones, validaciones y riesgos.
- No invade responsabilidades del otro track.
- El rollback tecnico y operativo esta claro para los archivos tocados.
