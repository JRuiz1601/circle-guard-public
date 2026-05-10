from locust import HttpUser, task, between

class CircleGuardUser(HttpUser):
    wait_time = between(1, 3)
    token = None

    def on_start(self):
        response = self.client.post("/auth/login", json={"username": "testuser", "password": "password"})
        if response.status_code == 200:
            self.token = response.json().get("token")

    @task(4)
    def login_stress(self):
        self.client.post("/auth/login", json={"username": "testuser", "password": "password"})

    @task(3)
    def form_submission(self):
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        self.client.post("/forms", json={"data": "test"}, headers=headers)

    @task(2)
    def file_upload(self):
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        files = {'file': ('test.txt', 'hello', 'text/plain')}
        self.client.post("/files/upload", files=files, headers=headers)

    @task(1)
    def dashboard_reads(self):
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        self.client.get("/dashboard", headers=headers)