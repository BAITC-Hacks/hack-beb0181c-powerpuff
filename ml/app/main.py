from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI

load_dotenv()

from app.model import model_service  # noqa: E402
from app.schemas import PredictRequest, PredictResponse  # noqa: E402


@asynccontextmanager
async def lifespan(_: FastAPI):
    model_service.load()
    yield


app = FastAPI(title="Powerpuff ML service", lifespan=lifespan)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "model_loaded": model_service.loaded}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest) -> PredictResponse:
    prediction, confidence = model_service.predict(req.features)
    return PredictResponse(
        prediction=prediction,
        confidence=confidence,
        model_loaded=model_service.loaded,
    )
