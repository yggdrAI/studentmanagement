from __future__ import annotations

from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.metrics import accuracy_score, mean_absolute_error
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

from model_dl import DietDeepModel


ROOT = Path(__file__).resolve().parent
DATASET_PATH = ROOT / "diet_dataset.csv"
MODEL_PATH = ROOT / "diet_model.pt"
SCALER_PATH = ROOT / "dl_scaler.pkl"
LABELS_PATH = ROOT / "dl_labels.pkl"

FEATURE_COLUMNS = [
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


def main() -> None:
    df = pd.read_csv(DATASET_PATH)

    x = df[FEATURE_COLUMNS].values.astype(np.float32)
    y_score = df["health_score"].values.astype(np.float32)

    label_encoder = LabelEncoder()
    y_class = label_encoder.fit_transform(df["label"].astype(str).values)

    x_train, x_test, y_score_train, y_score_test, y_class_train, y_class_test = train_test_split(
        x,
        y_score,
        y_class,
        test_size=0.2,
        random_state=42,
        stratify=y_class,
    )

    scaler = StandardScaler()
    x_train_scaled = scaler.fit_transform(x_train)
    x_test_scaled = scaler.transform(x_test)

    x_train_tensor = torch.tensor(x_train_scaled, dtype=torch.float32)
    y_score_tensor = torch.tensor(y_score_train, dtype=torch.float32).view(-1, 1)
    y_class_tensor = torch.tensor(y_class_train, dtype=torch.long)

    model = DietDeepModel(input_dim=len(FEATURE_COLUMNS), num_classes=len(label_encoder.classes_))

    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
    score_loss_fn = nn.MSELoss()
    class_loss_fn = nn.CrossEntropyLoss()

    model.train()
    for epoch in range(250):
        pred_score, pred_class = model(x_train_tensor)

        loss_score = score_loss_fn(pred_score, y_score_tensor)
        loss_class = class_loss_fn(pred_class, y_class_tensor)
        loss = loss_score + 0.4 * loss_class

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if epoch % 25 == 0:
            print(f"Epoch {epoch:03d} | total={loss.item():.4f} score={loss_score.item():.4f} class={loss_class.item():.4f}")

    model.eval()
    with torch.no_grad():
        test_tensor = torch.tensor(x_test_scaled, dtype=torch.float32)
        pred_score_test, pred_class_test = model(test_tensor)

    score_pred = pred_score_test.numpy().reshape(-1)
    class_pred = torch.argmax(pred_class_test, dim=1).numpy()

    mae = mean_absolute_error(y_score_test, score_pred)
    accuracy = accuracy_score(y_class_test, class_pred)

    torch.save(model.state_dict(), MODEL_PATH)
    joblib.dump(scaler, SCALER_PATH)
    joblib.dump(label_encoder, LABELS_PATH)

    print("DL rows:", len(df))
    print("DL MAE:", round(float(mae), 4))
    print("DL Accuracy:", round(float(accuracy), 4))
    print("Saved:", MODEL_PATH)
    print("Saved:", SCALER_PATH)
    print("Saved:", LABELS_PATH)


if __name__ == "__main__":
    main()
