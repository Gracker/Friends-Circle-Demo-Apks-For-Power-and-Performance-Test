"""NPU / AI benchmark via TFLite `benchmark_model`.

We use the official Google `benchmark_model` aarch64 Android binary plus a
quantized model. Both must be pushed to /data/local/tmp by the user:

    https://www.tensorflow.org/lite/performance/measurement
    Recommended model: MobileNetV2 1.0 224 quantized.

If the binary or model is not present, the stage returns skipped with hints.

Output parsed: average inference time, plus per-delegate timings if --use-gpu
or --use-nnapi succeed.
"""
from __future__ import annotations

import argparse
import re
import shlex
import time
from pathlib import Path
from typing import Any, Dict, List

from .adb import ADB
from .utils import combined_output, log, parse_float, write_csv

AI_DELEGATES = ("cpu", "xnnpack", "gpu", "nnapi")

BINARY_CANDIDATES = (
    "/data/local/tmp/benchmark_model",
    "/data/local/tmp/tflite/benchmark_model",
)
MODEL_CANDIDATES = (
    "/data/local/tmp/mobilenet_v2.tflite",
    "/data/local/tmp/mobilenet_v2_1.0_224_quant.tflite",
    "/data/local/tmp/tflite/mobilenet_v2.tflite",
)


def detect(adb: ADB) -> Dict[str, Any]:
    if adb.dry_run:
        return {"status": "dry_run", "binary": None, "model": None}
    binary = None
    model = None
    for path in BINARY_CANDIDATES:
        if adb.shell_out(f"[ -x {shlex.quote(path)} ] && echo OK || echo MISSING", timeout=10).strip().endswith("OK"):
            binary = path
            break
    for path in MODEL_CANDIDATES:
        if adb.shell_out(f"[ -f {shlex.quote(path)} ] && echo OK || echo MISSING", timeout=10).strip().endswith("OK"):
            model = path
            break
    if not binary or not model:
        return {
            "status": "missing",
            "binary": binary,
            "model": model,
            "hint": ("download TFLite benchmark_model aarch64 + a TFLite model to "
                     "/data/local/tmp; see "
                     "https://www.tensorflow.org/lite/performance/measurement"),
        }
    return {"status": "found", "binary": binary, "model": model}


_AVG_RE = re.compile(r"Inference\s*\(avg\)\s*:\s*(\d+(?:\.\d+)?)", re.IGNORECASE)
_INIT_RE = re.compile(r"Initialized session in (\d+(?:\.\d+)?)\s*ms", re.IGNORECASE)


def parse_benchmark_model(text: str) -> Dict[str, Any]:
    avg_us = None
    init_ms = None
    avg_match = _AVG_RE.search(text)
    if avg_match:
        avg_us = float(avg_match.group(1))
    init_match = _INIT_RE.search(text)
    if init_match:
        init_ms = float(init_match.group(1))
    return {"inference_avg_us": avg_us, "init_ms": init_ms}


def _run_one(adb: ADB, binary: str, model: str, delegate: str, threads: int, runs: int, timeout: int) -> Dict[str, Any]:
    flags: List[str] = [
        f"--graph={shlex.quote(model)}",
        f"--num_threads={threads}",
        f"--num_runs={runs}",
        "--warmup_runs=2",
    ]
    if delegate == "cpu":
        pass
    elif delegate == "gpu":
        flags.append("--use_gpu=true")
    elif delegate == "nnapi":
        flags.append("--use_nnapi=true")
    elif delegate == "xnnpack":
        flags.append("--use_xnnpack=true")
    cmd = f"{shlex.quote(binary)} " + " ".join(flags)
    log("INFO", f"AI benchmark_model delegate={delegate}: {cmd}")
    start = time.monotonic()
    proc = adb.shell(cmd, timeout=timeout)
    elapsed = time.monotonic() - start
    out = combined_output(proc)
    parsed = parse_benchmark_model(out)
    return {
        "delegate": delegate,
        "threads": threads,
        "runs": runs,
        "duration_sec": round(elapsed, 3),
        "status": "success" if proc.returncode == 0 and parsed["inference_avg_us"] is not None else "failed",
        "inference_avg_us": parsed["inference_avg_us"],
        "init_ms": parsed["init_ms"],
        "tail": out[-800:],
    }


def run_ai_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if getattr(args, "skip_ai", False):
        return {"status": "skipped", "reason": "--skip-ai"}
    if adb.dry_run:
        write_csv(details / "ai_inference.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not run TFLite"}
    detection = detect(adb)
    if detection["status"] != "found":
        return {"status": "skipped", "reason": "TFLite benchmark_model and/or model missing", **detection}
    binary = detection["binary"]
    model = detection["model"]
    runs = int(getattr(args, "ai_runs", 50))
    timeout = int(getattr(args, "ai_timeout_sec", 180))
    requested = getattr(args, "ai_delegates", None) or AI_DELEGATES
    delegates = [d for d in requested if d in AI_DELEGATES]
    if not delegates:
        return {"status": "skipped",
                "reason": f"no valid delegates in {list(requested)!r}; expected subset of {list(AI_DELEGATES)!r}"}
    rows: List[Dict[str, Any]] = []
    summary: Dict[str, Any] = {"binary": binary, "model": model}
    for delegate in delegates:
        threads = 1 if delegate in ("gpu", "nnapi") else int(getattr(args, "ai_cpu_threads", 4))
        row = _run_one(adb, binary, model, delegate, threads, runs, timeout)
        rows.append(row)
        if row.get("status") == "success":
            summary[f"{delegate}_inference_avg_us"] = row.get("inference_avg_us")
    write_csv(details / "ai_inference.csv", rows)
    cpu_us = summary.get("cpu_inference_avg_us")
    best_us = min(
        (v for v in (summary.get(f"{d}_inference_avg_us") for d in delegates) if v is not None),
        default=None,
    )
    summary["best_inference_avg_us"] = best_us
    summary["accel_speedup"] = (
        round(cpu_us / best_us, 3) if cpu_us and best_us and best_us > 0 else None
    )
    return {
        "status": "success" if rows and any(r.get("status") == "success" for r in rows) else "failed",
        "summary": summary,
        "rows": rows,
    }
