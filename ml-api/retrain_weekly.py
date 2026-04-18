from __future__ import annotations

import argparse
import subprocess
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def run_training() -> int:
    script = ROOT / "train_model.py"
    completed = subprocess.run([sys.executable, str(script)], cwd=str(ROOT), check=False)
    return completed.returncode


def sleep_until_next_monday_2am() -> None:
    now = datetime.now()
    days_ahead = (7 - now.weekday()) % 7
    target = (now + timedelta(days=days_ahead)).replace(hour=2, minute=0, second=0, microsecond=0)
    if target <= now:
        target += timedelta(days=7)

    seconds = max(0.0, (target - now).total_seconds())
    time.sleep(seconds)


def main() -> int:
    parser = argparse.ArgumentParser(description="Retrain diet ML models from accumulated app data.")
    parser.add_argument("--watch", action="store_true", help="Run weekly in a loop")
    args = parser.parse_args()

    if not args.watch:
        return run_training()

    while True:
        sleep_until_next_monday_2am()
        exit_code = run_training()
        print(f"Weekly retrain completed with exit code {exit_code}")


if __name__ == "__main__":
    raise SystemExit(main())