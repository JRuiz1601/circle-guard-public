"""
Misma definición que circle-guard-public/locustfile.py (mantener en sync).
Ejecutar: locust -f locustfile.py
"""
from locust import HttpUser, between, task
import requests

AUTH = "http://localhost:8081"
FORM = "http://localhost:8083"
FILE = "http://localhost:8084"
DASHBOARD = "http://localhost:8085"


def fetch_token():
    try:
        r = requests.post(
            f"{AUTH}/api/v1/auth/login",
            json={"username": "health_user", "password": "password"},
            timeout=30,
        )
        if r.status_code == 200:
            return r.json().get("token")
    except OSError:
        pass
    return None


class LoginStressUser(HttpUser):
    """Presión sobre login y uso del JWT en endpoint protegido (auth-service)."""

    host = AUTH
    wait_time = between(1, 2)

    def on_start(self):
        self.token = fetch_token()

    @task(3)
    def login_stress(self):
        self.client.post(
            "/api/v1/auth/login",
            json={"username": "health_user", "password": "password"},
            name="/api/v1/auth/login",
        )

    @task(2)
    def token_validation_flow(self):
        if not self.token:
            self.token = fetch_token()
        if self.token:
            self.client.get(
                "/api/v1/auth/qr/generate",
                headers={"Authorization": f"Bearer {self.token}"},
                name="/api/v1/auth/qr/generate",
            )


class FormLoadUser(HttpUser):
    """Listado / creación de cuestionarios (form-service), con JWT."""

    host = FORM
    wait_time = between(1, 3)

    def on_start(self):
        self.token = fetch_token()

    def _headers(self):
        return (
            {"Authorization": f"Bearer {self.token}"}
            if self.token
            else {}
        )

    @task(4)
    def form_submission_load(self):
        self.client.post(
            "/api/v1/questionnaires",
            json={
                "title": "locust-q",
                "description": "load",
                "version": 1,
                "isActive": False,
            },
            headers=self._headers(),
            name="/api/v1/questionnaires POST",
        )

    @task(2)
    def list_questionnaires(self):
        self.client.get(
            "/api/v1/questionnaires",
            headers=self._headers(),
            name="/api/v1/questionnaires GET",
        )


class FileUploadStressUser(HttpUser):
    """Multipart upload (file-service)."""

    host = FILE
    wait_time = between(2, 4)

    def on_start(self):
        self.token = fetch_token()

    @task
    def file_upload_stress(self):
        headers = {}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        self.client.post(
            "/api/v1/files/upload",
            files={"file": ("locust.bin", b"stress-bytes", "application/octet-stream")},
            headers=headers,
            name="/api/v1/files/upload",
        )


class DashboardReadsUser(HttpUser):
    """Lecturas concurrentes al tablero de analytics (dashboard-service)."""

    host = DASHBOARD
    wait_time = between(0.5, 2)

    @task(3)
    def dashboard_health_board(self):
        self.client.get("/api/v1/analytics/health-board", name="/api/v1/analytics/health-board")

    @task(1)
    def dashboard_summary(self):
        self.client.get("/api/v1/analytics/summary", name="/api/v1/analytics/summary")
