from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD


def train_cf_from_csv(csv_path: str | Path, n_components: int = 10) -> dict[str, Any]:
    ratings = pd.read_csv(csv_path)
    if ratings.empty:
        raise ValueError("No ratings data found for collaborative filtering")

    required = {"user_id", "meal_id", "rating"}
    if not required.issubset(set(ratings.columns)):
        raise ValueError("Ratings CSV must contain user_id, meal_id, rating")

    pivot = ratings.pivot_table(index="user_id", columns="meal_id", values="rating", fill_value=0.0)
    matrix = pivot.values

    max_components = min(matrix.shape[0], matrix.shape[1]) - 1
    n_components = max(1, min(n_components, max_components))

    svd = TruncatedSVD(n_components=n_components, random_state=42)
    user_factors = svd.fit_transform(matrix)
    item_factors = svd.components_.T

    return {
        "pivot": pivot,
        "svd": svd,
        "user_factors": user_factors,
        "item_factors": item_factors,
        "meal_ids": pivot.columns.to_list(),
    }


def recommend_for_user(cache: dict[str, Any], user_id: int, top_n: int = 3) -> list[str]:
    pivot = cache["pivot"]
    meal_ids = cache["meal_ids"]
    user_factors = cache["user_factors"]
    item_factors = cache["item_factors"]

    if user_id in pivot.index:
        idx = pivot.index.get_loc(user_id)
        scores = user_factors[idx] @ item_factors.T
    else:
        # Cold-start fallback: global meal popularity by average rating.
        scores = pivot.mean(axis=0).values

    ranked_indices = np.argsort(scores)[::-1][:top_n]
    return [str(meal_ids[idx]) for idx in ranked_indices]
