# Diet ML API

This service provides a real supervised-learning pipeline for the diet assistant flow:

Diet Logs -> Feature Engineering -> Training -> Saved Models -> FastAPI Prediction API -> Java Backend -> UI

## Files

- `diet_dataset.csv` - starter tabular dataset with regression and classification targets
- `train_model.py` - trains and saves `reg_model.pkl` and `clf_model.pkl`
- `main.py` - FastAPI serving layer used by the Java backend

## Install

```bash
pip install -r requirements.txt
```

## Train

```bash
python train_model.py
```

The script prints MAE, classification accuracy, and feature importance, then saves:

- `reg_model.pkl`
- `clf_model.pkl`

## Run API

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## Predict request

The API accepts this payload:

```json
{
  "calories": 2500,
  "junk_ratio": 0.5,
  "protein": 50,
  "carbs": 320,
  "fat": 90,
  "meal_time": 22,
  "activity_level": 1,
  "sleep_hours": 6.5,
  "water_intake": 2.0,
  "steps": 5000,
  "bmi": 25.1,
  "calories_history": [1800, 2100, 1950, 2250]
}
```

## Response

```json
{
  "score": 52.3,
  "health_score": 52.3,
  "prediction": "moderate",
  "recommendation": "Veg Oats",
  "recommendation_reason": "Higher protein, lower fat, matches calorie target",
  "future_risk": "High calorie intake tomorrow",
  "next_day_calories": 2144.8,
  "forecast_model": "exp-smoothing",
  "explanation": [
    {"feature": "junk_ratio", "importance": 0.31}
  ],
  "recommendations": []
}
```

## Java integration

Set in `application.properties`:

```properties
app.ml.api.base-url=http://localhost:8000
```

The Java service reads either `health_score` or the legacy `score` field, so the API stays backward compatible.

## Continuous learning

Append production logs to `diet_dataset.csv` and rerun `train_model.py` on a schedule.
