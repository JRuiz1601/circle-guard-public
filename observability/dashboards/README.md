# Grafana dashboards

Este directorio queda reservado para los dashboards JSON exportados desde Grafana despues de validar el stack local en Docker Desktop Kubernetes.

Dashboards versionados:

- `circleguard-sprint1-dashboard.json`: dashboard Sprint 1 con throughput, p95, errores 4xx/5xx y estado de targets.
- `circleguard-sprint1-dashboard-configmap.yaml`: ConfigMap para auto-provisioning con el sidecar de Grafana.

Dashboards previstos para Sprint 2:

- Estado de pods y reinicios.
- Metricas de negocio: logins, formularios enviados y sesiones activas.
