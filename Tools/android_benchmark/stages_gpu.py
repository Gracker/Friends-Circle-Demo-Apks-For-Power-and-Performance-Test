"""GPU benchmark via glmark2-es2 (open source).

This stage detects whether `glmark2-es2` is available on the device under one
of the standard locations. If absent, the stage returns `status=skipped` with
explicit instructions for how to push the binary.

We don't auto-download because the official glmark2 doesn't ship Android
binaries; the user must build it once (https://github.com/glmark2/glmark2)
and push to /data/local/tmp/glmark2-es2.
"""
from __future__ import annotations

import argparse
import re
import shlex
import time
from pathlib import Path
from typing import Any, Dict, List

from .adb import ADB
from .utils import combined_output, log, parse_float, parse_int, write_csv

GLMARK2_REMOTE_CANDIDATES = (
    "/data/local/tmp/glmark2-es2",
    "/data/local/tmp/glmark2/glmark2-es2",
    "/system/bin/glmark2-es2",
)


def detect_glmark2(adb: ADB) -> Dict[str, Any]:
    if adb.dry_run:
        return {"status": "dry_run", "found_at": None}
    for path in GLMARK2_REMOTE_CANDIDATES:
        out = adb.shell_out(f"[ -x {shlex.quote(path)} ] && echo OK || echo MISSING", timeout=10).strip()
        if out.endswith("OK"):
            return {"status": "found", "found_at": path}
    return {"status": "missing", "found_at": None,
            "hint": ("build glmark2-es2 from https://github.com/glmark2/glmark2 and push to "
                     "/data/local/tmp/glmark2-es2; chmod 755")}


def parse_glmark2_output(text: str) -> Dict[str, Any]:
    score: Any = None
    scenes: List[Dict[str, Any]] = []
    score_match = re.search(r"glmark2\s+Score:\s*(\d+)", text)
    if score_match:
        score = int(score_match.group(1))
    for line in text.splitlines():
        m = re.match(r"\s*\[([^\]]+)\]\s*([^:]+):\s*FPS:\s*(\d+)", line)
        if m:
            scenes.append({
                "scene": m.group(1).strip(),
                "options": m.group(2).strip(),
                "fps": int(m.group(3)),
            })
    return {"glmark2_score": score, "scenes": scenes}


def run_gpu_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if getattr(args, "skip_gpu", False):
        return {"status": "skipped", "reason": "--skip-gpu"}
    if adb.dry_run:
        write_csv(details / "gpu_glmark2.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not run glmark2"}
    detection = detect_glmark2(adb)
    if detection["status"] != "found":
        return {"status": "skipped", "reason": "glmark2-es2 not on device", **detection}
    bin_path = detection["found_at"]
    duration = int(getattr(args, "gpu_duration_sec", 60))
    cmd = f"{shlex.quote(bin_path)} -b :duration={duration}.0"
    log("INFO", f"GPU glmark2-es2 ({duration}s): {bin_path}")
    start = time.monotonic()
    proc = adb.shell(cmd, timeout=max(120, duration * 4))
    elapsed = time.monotonic() - start
    out = combined_output(proc)
    parsed = parse_glmark2_output(out)
    rows = [{"scene": s["scene"], "options": s["options"], "fps": s["fps"]} for s in parsed["scenes"]]
    rows.append({"scene": "TOTAL", "options": "", "fps": parsed.get("glmark2_score")})
    write_csv(details / "gpu_glmark2.csv", rows)
    return {
        "status": "success" if proc.returncode == 0 and parsed["glmark2_score"] is not None else "failed",
        "duration_sec": round(elapsed, 3),
        "binary": bin_path,
        "glmark2_score": parsed["glmark2_score"],
        "scenes": parsed["scenes"],
        "raw_tail": out[-1500:],
    }
