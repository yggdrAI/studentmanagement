from __future__ import annotations

from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.metrics import accuracy_score, mean_absolute_error
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler


ROOT = Path(__file__).resolve().parent
DATASET_PATH = ROOT / "diet_dataset.csv"
REG_MODEL_PATH = ROOT / "reg_model.pkl"
CLF_MODEL_PATH = ROOT / "clf_model.pkl"


def build_pipeline(estimator):
    return Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler()),
        ("model", estimator),
    ])


def main() -> None:
    df = pd.read_csv(DATASET_PATH)

    feature_columns = [
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

    X = df[feature_columns]
    y_reg = df["health_score"]
    y_clf = df["label"]

    X_train, X_test, y_reg_train, y_reg_test, y_clf_train, y_clf_test = train_test_split(
        X,
        y_reg,
        y_clf,
        test_size=0.2,
        random_state=42,
        stratify=y_clf,
    )

    reg_model = build_pipeline(
        RandomForestRegressor(
            n_estimators=250,
            random_state=42,
            min_samples_leaf=1,
        )
    )
    clf_model = build_pipeline(
        RandomForestClassifier(
            n_estimators=250,
            random_state=42,
            class_weight="balanced_subsample",
        )
    )

    reg_model.fit(X_train, y_reg_train)
    clf_model.fit(X_train, y_clf_train)

    reg_pred = reg_model.predict(X_test)
    clf_pred = clf_model.predict(X_test)

    mae = mean_absolute_error(y_reg_test, reg_pred)
    accuracy = accuracy_score(y_clf_test, clf_pred)

    joblib.dump(reg_model, REG_MODEL_PATH)
    joblib.dump(clf_model, CLF_MODEL_PATH)

    feature_importance = reg_model.named_steps["model"].feature_importances_.tolist()

    print("Dataset rows:", len(df))
    print("MAE (health_score):", round(mae, 4))
    print("Accuracy (label):", round(accuracy, 4))
    print("Feature importance:")
    for name, score in sorted(zip(feature_columns, feature_importance), key=lambda item: item[1], reverse=True):
        print(f"  {name}: {score:.4f}")
    print("Models saved to:")
    print(" ", REG_MODEL_PATH)
    print(" ", CLF_MODEL_PATH)


if __name__ == "__main__":
    main()
