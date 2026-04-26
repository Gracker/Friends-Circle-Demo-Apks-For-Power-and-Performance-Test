from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parents[1]
REPO_ROOT = SCRIPT_DIR.parent
DEFAULT_RUNS_ROOT = SCRIPT_DIR / "results" / "android-comprehensive-runs"
