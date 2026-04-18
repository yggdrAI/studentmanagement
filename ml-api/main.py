from __future__ import annotations

from pathlib import Path
from typing import Any, Dict

import joblib
import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    import torch
except Exception:  # pragma: no cover - optional dependency at runtime
    torch = None

try:
    from cf_recommender import recommend_for_user, train_cf_from_csv
except Exception:  # pragma: no cover - optional dependency at runtime
    recommend_for_user = None
    train_cf_from_csv = None

try:
    from model_dl import DietDeepModel
except Exception:  # pragma: no cover - optional dependency at runtime
    DietDeepModel = None

try:
    from statsmodels.tsa.arima.model import ARIMA
except Exception:  # pragma: no cover - optional dependency at runtime
    ARIMA = None


ROOT = Path(__file__).resolve().parent
REG_MODEL_PATH = ROOT / "reg_model.pkl"
CLF_MODEL_PATH = ROOT / "clf_model.pkl"
DL_MODEL_PATH = ROOT / "diet_model.pt"
DL_SCALER_PATH = ROOT / "dl_scaler.pkl"
DL_LABELS_PATH = ROOT / "dl_labels.pkl"
CF_RATINGS_PATH = ROOT / "user_meal_ratings.csv"


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
dl_model = None
dl_scaler = None
dl_label_encoder = None
cf_cache: dict[str, Any] | None = None


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


class PredictDLRequest(BaseModel):
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


class CFRequest(BaseModel):
    user_id: int
    top_n: int = Field(default=3, ge=1, le=10)


class LocationPoint(BaseModel):
    lat: float
    lng: float
    timestamp: int


class BehaviorRequest(BaseModel):
    points: list[LocationPoint] = Field(default_factory=list)
    speed_threshold_kmh: float = Field(default=50.0, ge=1.0, le=300.0)


def load_models() -> None:
    global reg_model, clf_model, dl_model, dl_scaler, dl_label_encoder, cf_cache

    if REG_MODEL_PATH.exists() and CLF_MODEL_PATH.exists():
        reg_model = joblib.load(REG_MODEL_PATH)
        clf_model = joblib.load(CLF_MODEL_PATH)

    if torch is not None and DietDeepModel is not None and DL_MODEL_PATH.exists() and DL_SCALER_PATH.exists() and DL_LABELS_PATH.exists():
        dl_scaler = joblib.load(DL_SCALER_PATH)
        dl_label_encoder = joblib.load(DL_LABELS_PATH)
        dl_model = DietDeepModel(input_dim=len(FEATURES), num_classes=len(dl_label_encoder.classes_))
        dl_model.load_state_dict(torch.load(DL_MODEL_PATH, map_location="cpu"))
        dl_model.eval()

    if train_cf_from_csv is not None and CF_RATINGS_PATH.exists():
        cf_cache = train_cf_from_csv(CF_RATINGS_PATH)


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


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius_km = 6371.0
    lat1_r = np.radians(lat1)
    lon1_r = np.radians(lon1)
    lat2_r = np.radians(lat2)
    lon2_r = np.radians(lon2)
    dlat = lat2_r - lat1_r
    dlon = lon2_r - lon1_r
    a = np.sin(dlat / 2) ** 2 + np.cos(lat1_r) * np.cos(lat2_r) * np.sin(dlon / 2) ** 2
    c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1 - a))
    return float(radius_km * c)


def analyze_behavior(points: list[LocationPoint], speed_threshold_kmh: float) -> dict[str, Any]:
    if len(points) < 2:
        return {
            "suspicious": False,
            "reason": "Insufficient location points",
            "max_speed_kmh": 0.0,
            "anomaly_score": 0.0,
        }

    sorted_points = sorted(points, key=lambda item: item.timestamp)
    speeds: list[float] = []
    distances: list[float] = []

    for idx in range(1, len(sorted_points)):
        prev = sorted_points[idx - 1]
        curr = sorted_points[idx]
        dist_km = haversine_km(prev.lat, prev.lng, curr.lat, curr.lng)
        time_hours = max((curr.timestamp - prev.timestamp) / 3_600_000.0, 1e-6)
        speed_kmh = dist_km / time_hours
        speeds.append(float(speed_kmh))
        distances.append(float(dist_km))

    max_speed = max(speeds)
    suspicious = max_speed > speed_threshold_kmh
    reason = "Unrealistic movement speed detected" if suspicious else "Movement pattern normal"

    # Basic anomaly score based on 95th percentile speed normalized by threshold.
    percentile_speed = float(np.percentile(speeds, 95))
    anomaly_score = min(1.0, percentile_speed / max(speed_threshold_kmh, 1.0))

    return {
        "suspicious": suspicious,
        "reason": reason,
        "max_speed_kmh": round(max_speed, 2),
        "avg_speed_kmh": round(float(np.mean(speeds)), 2),
        "total_distance_km": round(float(np.sum(distances)), 3),
        "anomaly_score": round(anomaly_score, 3),
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


@app.post("/predict-dl")
def predict_dl(data: PredictDLRequest) -> Dict[str, Any]:
    if torch is None or DietDeepModel is None or dl_model is None or dl_scaler is None or dl_label_encoder is None:
        return {
            "error": "Deep learning model not available. Train with train_dl.py first.",
            "health_score": None,
            "prediction": "unknown",
            "source": "dl-unavailable",
        }

    raw_features = np.array([[getattr(data, feature) for feature in FEATURES]], dtype=float)
    scaled = dl_scaler.transform(raw_features)
    tensor_x = torch.tensor(scaled, dtype=torch.float32)

    with torch.no_grad():
        score_out, class_out = dl_model(tensor_x)
        health_score = float(score_out.item())
        class_idx = int(torch.argmax(class_out, dim=1).item())
        prediction = str(dl_label_encoder.inverse_transform([class_idx])[0])

    return {
        "health_score": round(max(0.0, min(100.0, health_score)), 2),
        "prediction": prediction,
        "source": "pytorch-dl",
    }


@app.post("/recommend-cf")
def recommend_cf(data: CFRequest) -> Dict[str, Any]:
    if recommend_for_user is None or train_cf_from_csv is None:
        return {
            "recommended_meals": [],
            "source": "cf-unavailable",
            "reason": "CF dependencies unavailable",
        }

    global cf_cache
    if cf_cache is None and CF_RATINGS_PATH.exists():
        cf_cache = train_cf_from_csv(CF_RATINGS_PATH)

    if cf_cache is None:
        return {
            "recommended_meals": [],
            "source": "cf-empty",
            "reason": "No collaborative rating data available",
        }

    meals = recommend_for_user(cf_cache, user_id=data.user_id, top_n=data.top_n)
    return {
        "recommended_meals": meals,
        "source": "cf-svd",
    }


@app.post("/analyze-behavior")
def analyze_behavior_endpoint(data: BehaviorRequest) -> Dict[str, Any]:
    result = analyze_behavior(data.points, data.speed_threshold_kmh)
    result["source"] = "behavior-ai-v1"
    return result
