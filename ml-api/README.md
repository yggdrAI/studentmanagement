# Diet ML API

## Setup

```bash
pip install -r requirements.txt
```

## Run

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## Endpoints

- `GET /health`
- `POST /predict`

Request body:

```json
{
  "calories": 1450,
  "junk_ratio": 0.34
}
```

Response body:

```json
{
  "score": 54.2,
  "prediction": "moderate"
}
```

## Spring integration

Set in `application.properties`:

```properties
app.ml.api.base-url=http://localhost:8000
```
