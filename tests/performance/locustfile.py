import os
import random
import uuid

import requests
from locust import HttpUser, between, task


AUTH = os.getenv("CIRCLEGUARD_AUTH_BASE", "http://localhost:8081").rstrip("/")
FORM = os.getenv("CIRCLEGUARD_FORM_BASE", "http://localhost:8083").rstrip("/")
FILE = os.getenv("CIRCLEGUARD_FILE_BASE", "http://localhost:8084").rstrip("/")
DASHBOARD = os.getenv("CIRCLEGUARD_DASHBOARD_BASE", "http://localhost:8085").rstrip("/")

USERNAME = os.getenv("CIRCLEGUARD_PERF_USERNAME", "health_user")
PASSWORD = os.getenv("CIRCLEGUARD_PERF_PASSWORD", "password")


def fetch_token():
    try:
        response = requests.post(
            f"{AUTH}/api/v1/auth/login",
            json={"username": USERNAME, "password": PASSWORD},
            timeout=30,
        )
        if response.status_code == 200:
            return response.json().get("token")
    except requests.RequestException:
        return None
    return None


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"} if token else {}


def unique_suffix():
    return uuid.uuid4().hex[:10]


class CircleGuardUser(HttpUser):
    abstract = True
    wait_time = between(1, 3)

    def on_start(self):
        self.token = fetch_token()

    def headers(self):
        if not self.token:
            self.token = fetch_token()
        return auth_headers(self.token)


class AuthLoadUser(CircleGuardUser):
    """Carga sostenida sobre login y generacion de QR."""

    host = AUTH
    weight = 4
    wait_time = between(1, 2)

    @task(4)
    def login(self):
        self.client.post(
            "/api/v1/auth/login",
            json={"username": USERNAME, "password": PASSWORD},
            name="auth: login",
        )

    @task(2)
    def generate_qr(self):
        self.client.get(
            "/api/v1/auth/qr/generate",
            headers=self.headers(),
            name="auth: qr generate",
        )


class FormLoadUser(CircleGuardUser):
    """Carga de formularios: lectura, creacion y envio de encuestas."""

    host = FORM
    weight = 3
    wait_time = between(1, 3)

    @task(4)
    def list_questionnaires(self):
        self.client.get(
            "/api/v1/questionnaires",
            headers=self.headers(),
            name="form: questionnaire list",
        )

    @task(2)
    def create_questionnaire(self):
        suffix = unique_suffix()
        self.client.post(
            "/api/v1/questionnaires",
            json={
                "title": f"locust-q-{suffix}",
                "description": "locust load scenario",
                "version": random.randint(1, 50),
                "isActive": False,
            },
            headers=self.headers(),
            name="form: questionnaire create",
        )

    @task(2)
    def submit_survey(self):
        self.client.post(
            "/api/v1/surveys",
            json={
                "anonymousId": str(uuid.uuid4()),
                "symptoms": random.choice(["NONE", "COUGH", "FEVER"]),
                "temperature": random.choice([36.4, 36.8, 37.5, 38.1]),
                "contactWithPositive": random.choice([False, True]),
                "locationId": str(uuid.uuid4()),
            },
            headers=self.headers(),
            name="form: survey submit",
        )


class FileStressUser(CircleGuardUser):
    """Estres de subida multipart en file-service."""

    host = FILE
    weight = 2
    wait_time = between(0.5, 2)

    @task
    def upload_file(self):
        payload = f"locust-upload-{unique_suffix()}".encode()
        self.client.post(
            "/api/v1/files/upload",
            files={"file": ("locust.bin", payload, "application/octet-stream")},
            headers=self.headers(),
            name="file: upload",
        )


class DashboardReadUser(HttpUser):
    """Lecturas concurrentes al dashboard para medir consultas de analytics."""

    host = DASHBOARD
    weight = 3
    wait_time = between(0.5, 2)

    @task(4)
    def health_board(self):
        self.client.get(
            "/api/v1/analytics/health-board",
            name="dashboard: health board",
        )

    @task(2)
    def summary(self):
        self.client.get(
            "/api/v1/analytics/summary",
            name="dashboard: summary",
        )

    @task(1)
    def time_series(self):
        self.client.get(
            "/api/v1/analytics/time-series?period=hourly&limit=24",
            name="dashboard: time series",
        )
