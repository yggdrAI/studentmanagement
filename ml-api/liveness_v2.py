from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import io

import cv2
import numpy as np
import torch
import torchvision.transforms as T
from PIL import Image

from deepfake_detector import DeepfakeDetector
from vit_liveness import ViTLiveness


@dataclass
class LivenessScores:
    vit_score: float
    cnn_score: float
    deepfake_score: float
    final_score: float


class LivenessV2Engine:
    def __init__(self, vit_model: ViTLiveness, cnn_model: DeepfakeDetector, deepfake_model: DeepfakeDetector, device: str):
        self.device = torch.device(device)
        self.vit_model = vit_model.to(self.device).eval()
        self.cnn_model = cnn_model.to(self.device).eval()
        self.deepfake_model = deepfake_model.to(self.device).eval()
        self.to_tensor = T.Compose([
            T.Resize((224, 224)),
            T.ToTensor(),
        ])

    def preprocess_bytes(self, content: bytes) -> torch.Tensor:
        pil = Image.open(io.BytesIO(content)).convert("RGB")
        tensor = self.to_tensor(pil).unsqueeze(0)
        return tensor.to(self.device)

    def _fft_image_tensor(self, tensor: torch.Tensor) -> torch.Tensor:
        # tensor shape: [1, 3, H, W]
        img = tensor.squeeze(0).detach().cpu().numpy().transpose(1, 2, 0)
        gray = cv2.cvtColor((img * 255).astype(np.uint8), cv2.COLOR_RGB2GRAY)
        f = np.fft.fft2(gray)
        fshift = np.fft.fftshift(f)
        magnitude = np.log(np.abs(fshift) + 1)
        magnitude = cv2.normalize(magnitude, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)
        magnitude_rgb = cv2.cvtColor(magnitude, cv2.COLOR_GRAY2RGB)
        mag_tensor = torch.from_numpy(magnitude_rgb).permute(2, 0, 1).float() / 255.0
        return mag_tensor.unsqueeze(0).to(self.device)

    @torch.no_grad()
    def predict_from_tensor(self, tensor: torch.Tensor) -> LivenessScores:
        vit_score = float(self.vit_model(tensor).squeeze().item())
        cnn_score = float(self.cnn_model(tensor).squeeze().item())

        fft_tensor = self._fft_image_tensor(tensor)
        deepfake_score = float(self.deepfake_model(fft_tensor).squeeze().item())

        final_score = float((0.6 * vit_score) + (0.3 * cnn_score) + (0.1 * deepfake_score))
        return LivenessScores(vit_score=vit_score, cnn_score=cnn_score, deepfake_score=deepfake_score, final_score=final_score)

    @torch.no_grad()
    def predict_from_bytes(self, content: bytes) -> LivenessScores:
        tensor = self.preprocess_bytes(content)
        return self.predict_from_tensor(tensor)



def create_liveness_engine(root: Path) -> LivenessV2Engine:
    device = "cuda" if torch.cuda.is_available() else "cpu"

    vit_model = ViTLiveness(pretrained=False)
    cnn_model = DeepfakeDetector()
    deepfake_model = DeepfakeDetector()

    vit_weights = root / "vit_liveness.pt"
    cnn_weights = root / "cnn_liveness.pt"
    deepfake_weights = root / "deepfake_detector.pt"

    if not vit_weights.exists() and not cnn_weights.exists() and not deepfake_weights.exists():
        raise FileNotFoundError("No liveness-v2 model weights found")

    if vit_weights.exists():
        vit_model.load_state_dict(torch.load(vit_weights, map_location=device))
    if cnn_weights.exists():
        cnn_model.load_state_dict(torch.load(cnn_weights, map_location=device))
    if deepfake_weights.exists():
        deepfake_model.load_state_dict(torch.load(deepfake_weights, map_location=device))

    return LivenessV2Engine(vit_model=vit_model, cnn_model=cnn_model, deepfake_model=deepfake_model, device=device)
