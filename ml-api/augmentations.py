from __future__ import annotations

import albumentations as A


def build_train_transform() -> A.Compose:
    return A.Compose([
        A.Resize(224, 224),
        A.RandomBrightnessContrast(p=0.5),
        A.GaussianBlur(p=0.3),
        A.ImageCompression(quality_range=(30, 95), p=0.5),
        A.GaussNoise(p=0.3),
        A.HorizontalFlip(p=0.5),
        A.Rotate(limit=15, p=0.3),
        A.MotionBlur(p=0.3),
        A.RandomShadow(p=0.3),
        A.RandomFog(p=0.2),
    ])


def build_eval_transform() -> A.Compose:
    return A.Compose([
        A.Resize(224, 224),
    ])
