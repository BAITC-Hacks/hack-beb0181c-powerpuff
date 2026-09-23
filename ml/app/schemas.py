from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    # Пример: вектор признаков. Замените на свой формат входа (текст, картинка и т.д.)
    features: list[float] = Field(..., min_length=1, examples=[[5.1, 3.5, 1.4, 0.2]])


class PredictResponse(BaseModel):
    prediction: str | float | int
    confidence: float | None = None
    model_loaded: bool
