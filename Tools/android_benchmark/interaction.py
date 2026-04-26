from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from .adb import ADB
from .collectors import parse_wm_size
from .paths import REPO_ROOT
from .perfetto import TraceRecorder
from .stats import median_metric, numeric_summary, numeric_values
from .utils import combined_output, log, sanitize_for_filename, write_csv


PACKAGE = "com.example.benchmark.interaction"
ACTIVITY = "com.example.benchmark.interaction.InteractionBenchmarkActivity"
# Small tail buffer after the tap to let SF render the response before perfetto stops.
TRACE_TAIL_BUFFER_SEC = 1.5


def build_interaction_apk(force: bool = False) -> Dict[str, Any]:
    apk = REPO_ROOT / "benchmark-interaction" / "build" / "outputs" / "apk" / "debug" / "benchmark-interaction-debug.apk"
    src = REPO_ROOT / "benchmark-interaction" / "src" / "main" / "java" / "com" / "example" / "benchmark" / "interaction" / "InteractionBenchmarkActivity.java"
    if apk.exists() and not force and apk.stat().st_mtime >= src.stat().st_mtime:
        return {"status": "success", "apk": str(apk), "cached": True}
    cmd = [str(REPO_ROOT / "gradlew"), ":benchmark-interaction:assembleDebug", "--no-daemon"]
    log("INFO", "Build interaction benchmark APK: " + " ".join(cmd))
    proc = subprocess.run(cmd, cwd=str(REPO_ROOT), capture_output=True, text=True, timeout=300)
    if proc.returncode != 0:
        return {"status": "failed", "returncode": proc.returncode, "output": combined_output(proc)[-4000:]}
    if not apk.exists():
        return {"status": "failed", "reason": f"APK not produced: {apk}"}
    return {"status": "success", "apk": str(apk), "cached": False}


def parse_screen_size(text: str) -> Tuple[int, int]:
    size = parse_wm_size(text)
    if size and "x" in size:
        w, _, h = size.partition("x")
        try:
            return int(w), int(h)
        except ValueError:
            pass
    return 1080, 2400


def parse_interaction_log(text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {"raw": text.strip()}
    for key, line_name in (("action_down", "ACTION_DOWN"), ("on_draw", "ON_DRAW"), ("frame_callback", "FRAME_CALLBACK")):
        line = next((x for x in text.splitlines() if line_name in x), "")
        if not line:
            continue
        result[f"{key}_line"] = line
        for metric in ("dispatch_latency_ms", "latency_from_event_ms", "latency_from_handle_ms"):
            match = re.search(metric + r"=(-?\d+)", line)
            if match:
                result[f"{key}_{metric}"] = int(match.group(1))
    return result


def run_interaction_benchmark(serial: str, adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if args.skip_interaction:
        return {"status": "skipped", "reason": "--skip-interaction"}
    details = device_dir / "details"
    trace_dir = device_dir / "interaction" / "traces"
    log_dir = device_dir / "interaction" / "logs"
    if adb.dry_run:
        write_csv(details / "click_response.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not execute interaction benchmark"}

    if args.interaction_apk:
        apk = Path(args.interaction_apk)
        build = {"status": "success", "apk": str(apk), "external": True}
    else:
        build = build_interaction_apk(force=args.rebuild_interaction)
        apk = Path(build.get("apk", ""))
    if build.get("status") != "success":
        return build
    if not apk.exists():
        return {"status": "failed", "reason": f"interaction APK missing: {apk}"}

    if not args.skip_interaction_install:
        proc = adb.run(["install", "-r", "-d", str(apk)], timeout=180)
        if proc.returncode != 0:
            return {"status": "failed", "reason": "interaction APK install failed", "output": combined_output(proc)}

    adb.shell(f"am force-stop {PACKAGE}; logcat -c", timeout=20)
    start = adb.shell(f"am start -W -n {PACKAGE}/{ACTIVITY}", timeout=45)
    if start.returncode != 0:
        return {"status": "failed", "reason": "interaction activity launch failed", "output": combined_output(start)}
    time.sleep(args.interaction_settle_sec)
    width, height = parse_screen_size(adb.shell_out("wm size", timeout=10))
    x = int(width * args.interaction_tap_x_ratio)
    y = int(height * args.interaction_tap_y_ratio)

    rows: List[Dict[str, Any]] = []
    for iteration in range(1, args.interaction_iterations + 1):
        adb.shell("logcat -c", timeout=10)
        trace_path = trace_dir / f"{sanitize_for_filename(serial)}-tap-{iteration}.ptrace"
        trace_duration = (
            args.interaction_trace_warmup_sec
            + args.interaction_post_tap_sec
            + TRACE_TAIL_BUFFER_SEC
        )
        recorder = TraceRecorder(
            serial=serial,
            output_path=trace_path,
            dry_run=adb.dry_run,
            duration_sec=trace_duration,
        )
        ok, trace_msg = recorder.start()
        if not ok:
            rows.append({"iteration": iteration, "status": "failed", "reason": f"trace start failed: {trace_msg}"})
            continue
        time.sleep(args.interaction_trace_warmup_sec)
        tap_start = time.monotonic()
        tap_proc = adb.shell(f"input tap {x} {y}", timeout=15)
        adb_overhead_ms = round((time.monotonic() - tap_start) * 1000.0, 3)
        time.sleep(args.interaction_post_tap_sec)
        trace_ok, trace_stop = recorder.stop(timeout_sec=args.interaction_trace_stop_timeout_sec)
        logcat = adb.shell_out("logcat -d -v time -s HPFCInteraction:D '*:S'", timeout=20)
        log_dir.mkdir(parents=True, exist_ok=True)
        (log_dir / f"tap-{iteration}.log").write_text(logcat + "\n", encoding="utf-8")
        parsed = parse_interaction_log(logcat)
        rows.append(
            {
                "iteration": iteration,
                "status": "success" if tap_proc.returncode == 0 and trace_ok else "failed",
                "tap_x": x,
                "tap_y": y,
                "adb_overhead_ms": adb_overhead_ms,
                "dispatch_latency_ms": parsed.get("action_down_dispatch_latency_ms"),
                "on_draw_from_event_ms": parsed.get("on_draw_latency_from_event_ms"),
                "on_draw_from_handle_ms": parsed.get("on_draw_latency_from_handle_ms"),
                "frame_from_event_ms": parsed.get("frame_callback_latency_from_event_ms"),
                "frame_from_handle_ms": parsed.get("frame_callback_latency_from_handle_ms"),
                "trace_path": str(trace_path) if trace_path.exists() else "",
                "trace_ok": trace_ok,
                "trace_stop_output": trace_stop[:500],
                "log_path": str(log_dir / f"tap-{iteration}.log"),
            }
        )
        time.sleep(args.interaction_case_interval_sec)

    write_csv(details / "click_response.csv", rows)
    good_rows = [row for row in rows if row.get("status") == "success"]
    return {
        "status": "advisory" if good_rows else "failed",
        "advisory_reason": (
            "on_draw timestamps mark when the view records draw commands, not when "
            "pixels reach the display. True perceived latency needs correlation with "
            "SurfaceFlinger present_fence_time from the Perfetto trace."
        ),
        "build": build,
        "iterations": args.interaction_iterations,
        "tap": {"x": x, "y": y},
        "summary": {
            "dispatch_latency_ms": numeric_summary(numeric_values(good_rows, "dispatch_latency_ms")),
            "on_draw_from_event_ms": numeric_summary(numeric_values(good_rows, "on_draw_from_event_ms")),
            "frame_from_event_ms": numeric_summary(numeric_values(good_rows, "frame_from_event_ms")),
            "on_draw_median_ms": median_metric(good_rows, "on_draw_from_event_ms"),
            "frame_median_ms": median_metric(good_rows, "frame_from_event_ms"),
        },
        "csv": str(details / "click_response.csv"),
        "trace_dir": str(trace_dir),
        "log_dir": str(log_dir),
    }
