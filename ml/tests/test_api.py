from fastapi.testclient import TestClient

from app.main import app


def test_health():
    with TestClient(app) as client:
        r = client.get("/health")
        assert r.status_code == 200
        assert r.json()["status"] == "ok"


def test_predict():
    with TestClient(app) as client:
        r = client.post("/predict", json={"features": [5.1, 3.5, 1.4, 0.2]})
        assert r.status_code == 200
        assert "prediction" in r.json()
