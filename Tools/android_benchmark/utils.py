from __future__ import annotations

import csv
import json
import re
import shlex
import subprocess
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

def now_ts() -> str:
    return datetime.now().strftime("%H:%M:%S")


def log(level: str, message: str) -> None:
    print(f"[{now_ts()}] [{level}] {message}", flush=True)


def sanitize_for_filename(value: str) -> str:
    value = value.strip().replace(" ", "_")
    value = re.sub(r"[^A-Za-z0-9._-]+", "-", value)
    value = re.sub(r"-{2,}", "-", value)
    return value.strip("-_.") or "unknown"


def combined_output(proc: subprocess.CompletedProcess[str]) -> str:
    """Concat stdout+stderr as strings, tolerating bytes from subprocess timeouts.

    `subprocess.TimeoutExpired.stdout/stderr` can be bytes even when `text=True`
    was set, depending on Python version and the kill timing. We robustly decode.
    """
    def _to_str(x: Any) -> str:
        if x is None:
            return ""
        if isinstance(x, bytes):
            return x.decode("utf-8", errors="replace")
        return x
    return (_to_str(proc.stdout) + _to_str(proc.stderr)).strip()


def parse_float(value: Any) -> Optional[float]:
    if value is None:
        return None
    try:
        return float(str(value).strip())
    except (TypeError, ValueError):
        return None


def parse_int(value: Any) -> Optional[int]:
    if value is None:
        return None
    try:
        return int(float(str(value).strip()))
    except (TypeError, ValueError):
        return None


def percentile(values: Sequence[float], pct: float) -> Optional[float]:
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    pos = (len(ordered) - 1) * pct
    lower = int(pos)
    upper = min(lower + 1, len(ordered) - 1)
    weight = pos - lower
    return ordered[lower] * (1.0 - weight) + ordered[upper] * weight


def write_json(path: Path, payload: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False, default=str), encoding="utf-8")


def write_csv(path: Path, rows: Sequence[Dict[str, Any]], fieldnames: Optional[List[str]] = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if fieldnames is None:
        keys: List[str] = []
        for row in rows:
            for key in row.keys():
                if key not in keys:
                    keys.append(key)
        fieldnames = keys
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in fieldnames})


def count_lines(text: str) -> int:
    if not text:
        return 0
    return text.count("\n") + (0 if text.endswith("\n") else 1)


def run_host_command(
    cmd: Sequence[str],
    timeout: Optional[int] = None,
    log_file: Optional[Path] = None,
    cwd: Optional[Path] = None,
) -> subprocess.CompletedProcess[str]:
    start = time.monotonic()
    if log_file is None:
        try:
            proc = subprocess.run(
                list(cmd),
                cwd=str(cwd) if cwd else None,
                capture_output=True,
                text=True,
                timeout=timeout,
            )
        except subprocess.TimeoutExpired as exc:
            return subprocess.CompletedProcess(list(cmd), 124, exc.stdout or "", exc.stderr or "timeout")
        proc.elapsed_sec = time.monotonic() - start  # type: ignore[attr-defined]
        return proc

    log_file.parent.mkdir(parents=True, exist_ok=True)
    with log_file.open("w", encoding="utf-8") as handle:
        handle.write("$ " + " ".join(shlex.quote(x) for x in cmd) + "\n\n")
        handle.flush()
        try:
            proc = subprocess.run(
                list(cmd),
                cwd=str(cwd) if cwd else None,
                stdout=handle,
                stderr=subprocess.STDOUT,
                text=True,
                timeout=timeout,
            )
            stdout = ""
            stderr = ""
        except subprocess.TimeoutExpired:
            handle.write("\nTIMEOUT\n")
            proc = subprocess.CompletedProcess(list(cmd), 124, "", "timeout")
            stdout = ""
            stderr = "timeout"
    if isinstance(proc, subprocess.CompletedProcess):
        proc.stdout = stdout
        proc.stderr = stderr
        proc.elapsed_sec = time.monotonic() - start  # type: ignore[attr-defined]
        return proc
    raise RuntimeError("unexpected subprocess result")
