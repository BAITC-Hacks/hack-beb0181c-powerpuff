"""Загрузка модели и инференс. Вся ML-логика для API — здесь."""

import os
from pathlib import Path

import joblib

MODEL_PATH = Path(os.getenv("MODEL_PATH", "models/model.joblib"))


class ModelService:
    def __init__(self) -> None:
        self.model = None

    def load(self) -> None:
        if MODEL_PATH.exists():
            self.model = joblib.load(MODEL_PATH)
            print(f"Model loaded from {MODEL_PATH}")
        else:
            print(f"No model at {MODEL_PATH} — работаем в режиме заглушки. Запустите: python -m src.train")

    @property
    def loaded(self) -> bool:
        return self.model is not None

    def predict(self, features: list[float]) -> tuple[str | float | int, float | None]:
        if self.model is None:
            # Заглушка, чтобы бэкенд и фронт могли интегрироваться до готовности модели
            return "stub", None

        pred = self.model.predict([features])[0]
        confidence = None
        if hasattr(self.model, "predict_proba"):
            confidence = float(max(self.model.predict_proba([features])[0]))
        return (pred.item() if hasattr(pred, "item") else pred), confidence


model_service = ModelService()
