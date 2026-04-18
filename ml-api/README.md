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

Deep learning train:

```bash
python train_dl.py
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

## Deep learning inference

Endpoint:

```text
POST /predict-dl
```

Returns:

```json
{
  "health_score": 78.4,
  "prediction": "healthy",
  "source": "pytorch-dl"
}
```

## Collaborative filtering

Prepare `user_meal_ratings.csv` and call:

```text
POST /recommend-cf
```

Payload:

```json
{
  "user_id": 1,
  "top_n": 3
}
```

## Behavior anomaly API

Endpoint:

```text
POST /analyze-behavior
```

Payload:

```json
{
  "points": [
    {"lat": 28.4506, "lng": 77.5845, "timestamp": 1713410000000},
    {"lat": 28.4600, "lng": 77.5940, "timestamp": 1713410300000}
  ],
  "speed_threshold_kmh": 50
}
```

## Face embedding API

Endpoint:

```text
POST /embedding
```

Request: multipart form data with `file` image.

Response:

```json
{
  "embedding": [0.12, -0.44, 0.99],
  "dimension": 512,
  "model": "ArcFace-buffalo_l"
}
```

Notes:

- Uses InsightFace ArcFace (`buffalo_l`) for production-grade 512-d embeddings
- Enforces face detection and returns `422` when no face is found
- Intended for admin enrollment and verification pipelines that store only encrypted embeddings

## Liveness verification API

Endpoint:

```text
POST /liveness/verify
```

Payload:

```json
{
  "blink_detected": true,
  "head_turn_detected": true,
  "frame_count": 5,
  "motion_parallax_score": 0.08,
  "brightness_variance": 15.2,
  "frame_embeddings": [[0.1, 0.2], [0.11, 0.19], [0.08, 0.21]],
  "frame_snapshots": ["<base64-jpeg>"]
}
```

Checks performed:

- Active challenge checks (`blink_detected`, `head_turn_detected`)
- Minimum frame count check (at least 3)
- Motion parallax threshold check
- Frame embedding variation check (anti-replay)
- Brightness variance check (anti-screen/photo)
- Optional ViT/CNN/deepfake fusion check when trained model weights are present

## Liveness v2 fusion API

Endpoint:

```text
POST /liveness-v2
```

Request: multipart form data with `file` image.

Response:

```json
{
  "vit_score": 0.86,
  "cnn_score": 0.81,
  "deepfake_score": 0.78,
  "score": 0.835,
  "is_real": true,
  "threshold": 0.7,
  "source": "liveness-v2-fusion"
}
```

Fusion formula:

```text
final = 0.6 * vit_score + 0.3 * cnn_score + 0.1 * deepfake_score
```

## Model training files

- `vit_liveness.py` - ViT liveness model (binary anti-spoof)
- `deepfake_detector.py` - CNN detector head used for spoof/deepfake artifacts
- `augmentations.py` - robust augmentation pipeline for small datasets
- `train_vit.py` - baseline ViT training script
- `liveness_v2.py` - fusion inference runtime

## Train liveness model

```bash
python train_vit.py
```

Expected dataset layout:

```text
ml-api/datasets/liveness/train/live/
ml-api/datasets/liveness/train/spoof/
```

Saved weights:

```text
vit_liveness.pt
```

Optional extra weights used by fusion runtime:

```text
cnn_liveness.pt
deepfake_detector.pt
```

## Java integration

Set in `application.properties`:

```properties
app.ml.api.base-url=http://localhost:8000
app.face.embedding-service-url=http://localhost:8001
app.face.liveness-service-url=http://localhost:8001
app.face.embedding-encryption-key=<BASE64_32_BYTE_AES_KEY>
```

Scanner integration now sends:

- `frameEmbeddings` (multi-frame face descriptors)
- `motionParallaxScore`
- `brightnessVariance`
- `frameSnapshots` (base64 face crops for optional v2 fusion)

Spring backend forwards these to `/liveness/verify` and Redis-caches enrolled embeddings for faster matching.

The Java service reads either `health_score` or the legacy `score` field, so the API stays backward compatible.

## Continuous learning

Append production logs to `diet_dataset.csv` and rerun `train_model.py` on a schedule.

## Docker scaling examples (face embedding replicas)

Base services:

```bash
docker compose -f ../docker-compose.yml up -d
```

Scale face-embedding with edge proxy profile enabled:

```bash
docker compose -f ../docker-compose.yml -f ../docker-compose.scale.yml --profile edge up -d --scale face-embedding=3
```

Edge route policy example:

- `http://localhost:8088/embedding` -> reverse-proxied to face-embedding replicas
- `http://localhost:8088/health` -> health probe through nginx to embedding service

## GPU deployment

Use GPU-specific Dockerfile:

```bash
docker build -f Dockerfile.gpu -t studentmanagement-face-gpu .
docker run --gpus all -p 8001:8000 studentmanagement-face-gpu
```

Or compose overlay from repo root:

```bash
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d face-embedding
```
