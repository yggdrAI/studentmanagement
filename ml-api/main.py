from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Diet ML API", version="1.0.0")


class PredictRequest(BaseModel):
    calories: float
    junk_ratio: float


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/predict")
def predict(data: PredictRequest) -> dict:
    score = 100.0 - (data.calories * 0.02 + data.junk_ratio * 50.0)
    score = max(0.0, min(100.0, score))

    if score < 50.0:
        prediction = "unhealthy"
    elif score < 75.0:
        prediction = "moderate"
    else:
        prediction = "healthy"

    return {
        "score": round(score, 1),
        "prediction": prediction,
    }
