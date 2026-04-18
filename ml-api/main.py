from __future__ import annotations

from pathlib import Path
from typing import Any, Dict

import joblib
import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    from statsmodels.tsa.arima.model import ARIMA
except Exception:  # pragma: no cover - optional dependency at runtime
    ARIMA = None


ROOT = Path(__file__).resolve().parent
REG_MODEL_PATH = ROOT / "reg_model.pkl"
CLF_MODEL_PATH = ROOT / "clf_model.pkl"


FEATURES = [
    "calories",
    "junk_ratio",
    "protein",
    "carbs",
    "fat",
    "meal_time",
    "activity_level",
    "sleep_hours",
    "water_intake",
    "steps",
    "bmi",
]


class PredictRequest(BaseModel):
    calories: float = Field(ge=0)
    junk_ratio: float = Field(ge=0, le=1)
    protein: float = Field(default=0, ge=0)
    carbs: float = Field(default=0, ge=0)
    fat: float = Field(default=0, ge=0)
    meal_time: float = Field(default=12, ge=0, le=23)
    activity_level: float = Field(default=0, ge=0, le=3)
    sleep_hours: float = Field(default=7.0, ge=0, le=24)
    water_intake: float = Field(default=2.0, ge=0)
    steps: float = Field(default=5000, ge=0)
    bmi: float = Field(default=23.0, ge=0)
    calories_history: list[float] = Field(default_factory=list)


app = FastAPI(title="Diet ML API", version="2.0.0")

reg_model = None
clf_model = None


class Meal:
    def __init__(self, name: str, calories: float, protein: float, carbs: float, fat: float, is_junk: bool):
        self.name = name
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.is_junk = is_junk


MEALS = [
    Meal("Veg Oats", 320, 14, 46, 8, False),
    Meal("Paneer Salad Bowl", 380, 24, 20, 18, False),
    Meal("Dal + Brown Rice", 430, 18, 62, 10, False),
    Meal("Fruit Yogurt Combo", 290, 11, 42, 7, False),
    Meal("Idli Sambar", 300, 10, 48, 6, False),
    Meal("Vada Pao", 360, 8, 41, 18, True),
    Meal("Burger + Fries", 720, 17, 68, 38, True),
]


def load_models() -> None:
    global reg_model, clf_model

    if REG_MODEL_PATH.exists() and CLF_MODEL_PATH.exists():
        reg_model = joblib.load(REG_MODEL_PATH)
        clf_model = joblib.load(CLF_MODEL_PATH)


def heuristic_predict(payload: PredictRequest) -> tuple[float, str]:
    score = 100.0
    score -= payload.calories * 0.015
    score -= payload.junk_ratio * 55.0
    score += payload.protein * 0.08
    score -= max(0.0, payload.fat - 60.0) * 0.12
    score += min(payload.activity_level, 3) * 3.5
    score += min(payload.sleep_hours, 9.0) * 1.2
    score += min(payload.water_intake, 4.0) * 1.5
    score += min(payload.steps / 1000.0, 12.0) * 0.8
    score -= max(0.0, payload.bmi - 24.0) * 1.1
    score = max(0.0, min(100.0, score))

    if score < 45.0:
        label = "unhealthy"
    elif score < 75.0:
        label = "moderate"
    else:
        label = "healthy"

    return round(score, 2), label


def prepare_features(payload: PredictRequest) -> np.ndarray:
    return np.array([[getattr(payload, feature) for feature in FEATURES]], dtype=float)


def recommend_meals(payload: PredictRequest) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    best_meal = None
    best_score = -999999.0
    ranked: list[dict[str, Any]] = []

    for meal in MEALS:
        score = 0.0
        score += meal.protein * 0.4
        score += (100.0 - meal.fat) * 0.2
        score -= abs(payload.calories - meal.calories) * 0.02
        if meal.is_junk:
            score -= 20.0

        ranked.append(
            {
                "meal_name": meal.name,
                "score": round(score, 2),
                "reason": build_meal_reason(payload, meal),
            }
        )

        if score > best_score:
            best_score = score
            best_meal = meal

    ranked = sorted(ranked, key=lambda item: item["score"], reverse=True)

    if best_meal is None:
        return {"name": "Balanced Meal", "reason": "No matching meal found"}, ranked[:3]

    recommendation = {
        "name": best_meal.name,
        "reason": build_meal_reason(payload, best_meal),
    }
    return recommendation, ranked[:3]


def build_meal_reason(payload: PredictRequest, meal: Meal) -> str:
    reason_parts = []
    if meal.protein >= 14:
        reason_parts.append("higher protein")
    if meal.fat <= 10:
        reason_parts.append("lower fat")
    calorie_delta = abs(payload.calories - meal.calories)
    if calorie_delta <= 120:
        reason_parts.append("matches calorie target")
    if meal.is_junk:
        reason_parts.append("junk meal, keep occasional")

    if not reason_parts:
        return "Balanced nutrition profile"

    return ", ".join(reason_parts).capitalize()


def exponential_smoothing_forecast(calories_history: list[float], alpha: float = 0.7) -> float:
    if not calories_history:
        return 0.0

    prediction = float(calories_history[0])
    for value in calories_history[1:]:
        prediction = alpha * float(value) + (1.0 - alpha) * prediction
    return float(prediction)


def arima_forecast(calories_history: list[float]) -> float | None:
    if ARIMA is None or len(calories_history) < 6:
        return None

    try:
        model = ARIMA(calories_history, order=(2, 1, 2))
        model_fit = model.fit()
        forecast = model_fit.forecast(steps=1)
        return float(forecast[0])
    except Exception:
        return None


def forecast_future_calories(payload: PredictRequest) -> dict[str, Any]:
    history = [float(value) for value in payload.calories_history if value >= 0]
    if not history:
        history = [float(payload.calories)]

    exp_prediction = exponential_smoothing_forecast(history)
    arima_prediction = arima_forecast(history)
    final_prediction = arima_prediction if arima_prediction is not None else exp_prediction

    if final_prediction >= 2200:
        future_risk = "High calorie intake tomorrow"
    elif final_prediction >= 1800:
        future_risk = "Moderate calorie intake tomorrow"
    else:
        future_risk = "Calorie intake likely stable tomorrow"

    return {
        "next_day_calories": round(float(final_prediction), 2),
        "future_risk": future_risk,
        "forecast_model": "arima(2,1,2)" if arima_prediction is not None else "exp-smoothing",
    }


@app.on_event("startup")
def startup_event() -> None:
    load_models()


@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "status": "ok",
        "models_loaded": bool(reg_model and clf_model),
        "features": FEATURES,
    }


@app.post("/predict")
def predict(data: PredictRequest) -> Dict[str, Any]:
    features = prepare_features(data)

    if reg_model is not None and clf_model is not None:
        health_score = float(reg_model.predict(features)[0])
        prediction = str(clf_model.predict(features)[0])

        if hasattr(reg_model, "named_steps"):
            model_step = reg_model.named_steps.get("model")
            importances = getattr(model_step, "feature_importances_", None)
        else:
            importances = None

        explanation = None
        if importances is not None:
            ranked = sorted(zip(FEATURES, importances), key=lambda item: item[1], reverse=True)[:5]
            explanation = [{"feature": name, "importance": round(float(score), 4)} for name, score in ranked]
    else:
        health_score, prediction = heuristic_predict(data)
        explanation = [
            {"feature": "calories", "importance": 0.35},
            {"feature": "junk_ratio", "importance": 0.28},
            {"feature": "activity_level", "importance": 0.11},
        ]

    recommendation, recommendation_rankings = recommend_meals(data)
    forecast = forecast_future_calories(data)

    return {
        "score": round(float(health_score), 2),
        "health_score": round(float(health_score), 2),
        "prediction": prediction,
        "recommendation": recommendation["name"],
        "recommendation_reason": recommendation["reason"],
        "future_risk": forecast["future_risk"],
        "next_day_calories": forecast["next_day_calories"],
        "forecast_model": forecast["forecast_model"],
        "explanation": explanation,
        "recommendations": recommendation_rankings,
    }
