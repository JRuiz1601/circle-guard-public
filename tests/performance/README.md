# CircleGuard Performance Tests

Locust scenarios for Sprint 2 performance and stress evidence.

## Scope

The suite covers the flows required by `PlanSprint2.md`:

- Auth login and protected QR generation.
- Form questionnaire reads/creates and survey submits.
- File multipart upload.
- Dashboard analytics reads.

## Local Target

Expose services with port-forward before running:

```powershell
kubectl port-forward -n circleguard-dev svc/circleguard-auth-service 8081:80
kubectl port-forward -n circleguard-dev svc/circleguard-form-service 8083:80
kubectl port-forward -n circleguard-dev svc/circleguard-file-service 8084:80
kubectl port-forward -n circleguard-dev svc/circleguard-dashboard-service 8085:80
```

Defaults can be overridden:

```powershell
$env:CIRCLEGUARD_AUTH_BASE = "http://localhost:8081"
$env:CIRCLEGUARD_FORM_BASE = "http://localhost:8083"
$env:CIRCLEGUARD_FILE_BASE = "http://localhost:8084"
$env:CIRCLEGUARD_DASHBOARD_BASE = "http://localhost:8085"
```

## Load Profile

```powershell
python -m locust -f tests/performance/locustfile.py --headless --users 8 --spawn-rate 2 --run-time 2m --csv evidence/sprint2/locust-load --html evidence/sprint2/locust-load-report.html
```

Acceptance target for a local developer cluster:

- No unhandled Locust/runtime errors.
- P95 below 2000 ms for read endpoints.
- Failure rate documented and investigated if above 5%.

## Stress Profile

```powershell
python -m locust -f tests/performance/locustfile.py --headless --users 20 --spawn-rate 5 --run-time 3m --csv evidence/sprint2/locust-stress --html evidence/sprint2/locust-stress-report.html
```

Acceptance target:

- The stack remains reachable during the run.
- Any saturation, timeout, or failure burst is documented in `evidence/sprint2/`.
- Stress failures are acceptable only if they identify a capacity limit and do not hide service crashes.
