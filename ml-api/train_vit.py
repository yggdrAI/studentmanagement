from __future__ import annotations

from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms

from vit_liveness import ViTLiveness


def train(data_dir: str, epochs: int = 10, lr: float = 1e-4, batch_size: int = 16) -> None:
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    train_transform = transforms.Compose([
        transforms.Resize((224, 224)),
        transforms.RandomHorizontalFlip(),
        transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
        transforms.ToTensor(),
    ])

    dataset = datasets.ImageFolder(root=data_dir, transform=train_transform)
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True, num_workers=2)

    model = ViTLiveness(pretrained=True).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)
    loss_fn = nn.BCELoss()

    model.train()
    for epoch in range(epochs):
        running_loss = 0.0
        for images, labels in loader:
            images = images.to(device)
            labels = labels.to(device).float().unsqueeze(1)

            preds = model(images)
            loss = loss_fn(preds, labels)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            running_loss += float(loss.item())

        avg_loss = running_loss / max(1, len(loader))
        print(f"Epoch {epoch + 1}/{epochs} - loss={avg_loss:.4f}")

    output = Path(data_dir).resolve().parent / "vit_liveness.pt"
    torch.save(model.state_dict(), output)
    print(f"Saved weights to {output}")


if __name__ == "__main__":
    train(data_dir="./datasets/liveness/train", epochs=10)
