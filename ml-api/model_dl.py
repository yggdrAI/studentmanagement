from __future__ import annotations

import torch
import torch.nn as nn


class DietDeepModel(nn.Module):
    def __init__(self, input_dim: int, num_classes: int):
        super().__init__()

        self.shared = nn.Sequential(
            nn.Linear(input_dim, 64),
            nn.ReLU(),
            nn.Dropout(0.15),
            nn.Linear(64, 32),
            nn.ReLU(),
        )
        self.score_head = nn.Linear(32, 1)
        self.class_head = nn.Linear(32, num_classes)

    def forward(self, x):
        features = self.shared(x)
        score = self.score_head(features)
        logits = self.class_head(features)
        return score, logits
