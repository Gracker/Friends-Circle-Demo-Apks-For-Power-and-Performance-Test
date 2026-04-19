#!/usr/bin/env python3
"""Auto-record Perfetto traces for launch performance APKs.

Workflow:
1. Discover `launch-*-{light|medium|heavy}-release-*.apk` under apk-released.
2. For each (app, flavor) x N iterations:
   - force-stop + press-home + wait to guarantee a cold start
   - clear logcat, spawn a LaunchPerformance logcat monitor
   - start Perfetto trace
   - `am start -W` the launcher activity
   - wait for LaunchPerformance [Duration: Nms] log
   - stop Perfetto trace
3. After all captures, parse every trace via trace_processor_shell
   (android_startup metric + custom LoadSimulator_* slices).
4. Aggregate per (app, flavor) and emit JSON + Markdown reports.
"""

from __future__ import annotations

import argparse
import json
import re
import signal
import statistics
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field, asdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from Tools.auto_record_perfetto_scrolling import (  # noqa: E402
    ADB,
    TraceRecorder,
    combined_output,
    install_apk,
    log,
    sanitize_for_filename,
)


DEFAULT_APPS = ("launch-aosp", "launch-compose", "launch-webview", "launch-gl", "launch-game")
DEFAULT_FLAVORS = ("light", "medium", "heavy")
CUSTOM_SLICE_NAMES = ("LoadSimulator_AppInit", "RealInflation", "Final_UI_Freeze")

DEFAULT_THRESHOLDS_MS = {
    "excellent": 300,
    "good": 500,
    "acceptable": 1000,
    "poor": 2000,
}


@dataclass
class LaunchModuleMeta:
    """Metadata for a single (app, flavor) under test.

    The `package_name` alias is kept so this dataclass satisfies the informal
    protocol of `Tools.auto_record_perfetto_scrolling.install_apk` without
    having to adapt its signature.
    """
    app_name: str                 # "launch-aosp"
    stack: str                    # "aosp"
    flavor: str                   # "light"
    package: str                  # "com.example.launch.aosp.light"
    activity: str                 # "com.example.launch.aosp.MainActivity"
    apk_path: Path
    module_dir: Path
    module_name: str              # "launch-aosp-light" (for filenames)
    file_size_bytes: int
    file_mtime: str               # ISO-8601

    @property
    def package_name(self) -> str:
        return self.package


@dataclass
class LaunchCaptureResult:
    app_name: str
    stack: str
    flavor: str
    iteration: int
    status: str                                            # success / timeout / failed / skipped
    reason: str                                            # see FAILURE_REASONS
    logcat_duration_ms: Optional[int] = None
    logcat_start_type: Optional[str] = None                # cold / warm / hot (from PerformanceLogger)
    logcat_load_type: Optional[str] = None                 # light / medium / heavy
    logcat_app_type: Optional[str] = None
    am_status: Optional[str] = None                        # e.g. "ok", "Error", "unavailable"
    am_launch_state: Optional[str] = None                  # e.g. COLD / WARM / HOT / UNKNOWN
    am_total_time_ms: Optional[int] = None
    am_wait_time_ms: Optional[int] = None
    am_this_time_ms: Optional[int] = None
    trace_path: Optional[Path] = None
    trace_size_bytes: Optional[int] = None
    perfetto_startup_type: Optional[str] = None
    perfetto_to_first_frame_ms: Optional[float] = None
    perfetto_custom_slices_ms: Dict[str, float] = field(default_factory=dict)
    perfetto_parse_error: Optional[str] = None
    started_at: str = ""
    duration_wall_ms: Optional[int] = None


# -----------------------------------------------------------------------------
# Registry & discovery
# -----------------------------------------------------------------------------

def load_launch_registry(registry_path: Path) -> List[Dict[str, Any]]:
    with registry_path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    apps = data.get("launch_tests", {}).get("apps", [])
    if not apps:
        raise RuntimeError(f"No launch_tests.apps found in {registry_path}")
    return apps


def discover_launch_modules(
    registry_apps: List[Dict[str, Any]],
    apk_dir: Path,
    repo_root: Path,
    only_apps: Optional[List[str]],
    only_flavors: Optional[List[str]],
) -> Tuple[List[LaunchModuleMeta], List[Tuple[str, str]]]:
    """Return (modules, skipped) where skipped is [(label, reason)]."""
    modules: List[LaunchModuleMeta] = []
    skipped: List[Tuple[str, str]] = []

    app_filter = set(only_apps) if only_apps else None
    flavor_filter = set(only_flavors) if only_flavors else None

    # Registry hard-codes v1.0.0 filenames; the actual files may be any
    # version. Index once by stripped prefix so per-flavor lookups are O(1).
    apk_index: Dict[str, List[Path]] = {}
    for apk in sorted(apk_dir.glob("launch-*-release-*.apk")):
        prefix = apk.name.rsplit("-release-", 1)[0]  # e.g. "launch-aosp-light"
        apk_index.setdefault(prefix, []).append(apk)

    for app in registry_apps:
        app_name = app["name"]
        stack = app_name.removeprefix("launch-")
        if app_filter and app_name not in app_filter:
            continue

        activity = app["activity"]
        module_dir = repo_root / app_name

        for flavor_cfg in app.get("flavors", []):
            flavor = flavor_cfg["name"]
            if flavor_filter and flavor not in flavor_filter:
                continue

            package = flavor_cfg["package"]
            key = f"{app_name}-{flavor}"
            matches = apk_index.get(key, [])
            if not matches:
                skipped.append(
                    (f"{app_name}/{flavor}", f"no APK found matching {key}-release-*.apk under {apk_dir}")
                )
                continue

            apk_path = matches[-1]
            stat = apk_path.stat()
            modules.append(
                LaunchModuleMeta(
                    app_name=app_name,
                    stack=stack,
                    flavor=flavor,
                    package=package,
                    activity=activity,
                    apk_path=apk_path,
                    module_dir=module_dir,
                    module_name=key,
                    file_size_bytes=stat.st_size,
                    file_mtime=datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="seconds"),
                )
            )
    return modules, skipped


# -----------------------------------------------------------------------------
# `am start -W` output parsing
# -----------------------------------------------------------------------------

_AM_FIELD_PATTERNS = {
    "am_status": re.compile(r"^Status:\s*(\S+)", re.MULTILINE),
    "am_launch_state": re.compile(r"^LaunchState:\s*(\S+)", re.MULTILINE),
    "am_total_time_ms": re.compile(r"^TotalTime:\s*(\d+)", re.MULTILINE),
    "am_wait_time_ms": re.compile(r"^WaitTime:\s*(\d+)", re.MULTILINE),
    "am_this_time_ms": re.compile(r"^ThisTime:\s*(\d+)", re.MULTILINE),
}


def parse_am_start_output(output: str) -> Dict[str, Any]:
    parsed: Dict[str, Any] = {}
    for key, pat in _AM_FIELD_PATTERNS.items():
        match = pat.search(output)
        if not match:
            continue
        raw = match.group(1)
        parsed[key] = int(raw) if key.endswith("_ms") else raw
    return parsed


# -----------------------------------------------------------------------------
# Logcat monitor
# -----------------------------------------------------------------------------

_LAUNCH_PERF_RE = re.compile(
    r"\[AppType:\s*([^\]]+)\]\s*"
    r"\[Load:\s*([^\]]+)\]\s*"
    r"\[StartType:\s*([^\]]+)\]\s*"
    r"\[Duration:\s*(\d+)ms\]"
)


class LaunchLogcatMonitor:
    """Background thread that tails `adb logcat LaunchPerformance:I *:S`.

    Distinguishes between `found`, `adb_error` (subprocess died or non-zero
    exit), and `timeout` so the caller can report precise failure reasons.
    """

    TAG = "LaunchPerformance"

    def __init__(self, serial: Optional[str]):
        self.serial = serial
        self._proc: Optional[subprocess.Popen[str]] = None
        self._thread: Optional[threading.Thread] = None
        self._result: Dict[str, Any] = {
            "found": False,
            "duration_ms": None,
            "app_type": None,
            "load": None,
            "start_type": None,
            "adb_error": None,
        }
        self._stop = threading.Event()

    def start(self) -> bool:
        cmd: List[str] = ["adb"]
        if self.serial:
            cmd += ["-s", self.serial]
        cmd += ["logcat", "-v", "time", f"{self.TAG}:I", "*:S"]
        try:
            self._proc = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1,
            )
        except OSError as exc:
            self._result["adb_error"] = f"spawn_failed: {exc}"
            return False

        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        return True

    def _run(self) -> None:
        assert self._proc is not None and self._proc.stdout is not None
        try:
            for line in self._proc.stdout:
                if self._stop.is_set():
                    break
                match = _LAUNCH_PERF_RE.search(line)
                if not match:
                    continue
                self._result["app_type"] = match.group(1).strip()
                self._result["load"] = match.group(2).strip()
                self._result["start_type"] = match.group(3).strip()
                self._result["duration_ms"] = int(match.group(4))
                self._result["found"] = True
                break
            else:
                # Stream ended without match - adb disconnected or logcat died
                if not self._stop.is_set():
                    stderr = ""
                    if self._proc.stderr is not None:
                        try:
                            stderr = self._proc.stderr.read() or ""
                        except Exception:
                            stderr = ""
                    rc = self._proc.poll()
                    self._result["adb_error"] = (
                        f"logcat_stream_closed (rc={rc}): {stderr.strip()[:200]}"
                    )
        except Exception as exc:
            self._result["adb_error"] = f"logcat_read_exception: {exc}"

    def wait(self, timeout_sec: float) -> Dict[str, Any]:
        if self._thread is not None:
            self._thread.join(timeout=timeout_sec)
        self.stop()
        return dict(self._result)

    def stop(self) -> None:
        self._stop.set()
        if self._proc and self._proc.poll() is None:
            try:
                self._proc.terminate()
                self._proc.wait(timeout=3)
            except Exception:
                try:
                    self._proc.kill()
                except Exception:
                    pass


# -----------------------------------------------------------------------------
# Perfetto metric extraction
# -----------------------------------------------------------------------------

class PerfettoMetricExtractor:
    """Wraps `trace_processor_shell` for post-hoc trace analysis."""

    def __init__(self, binary: Path):
        self.binary = binary

    def extract_android_startup(
        self, trace_path: Path, target_package: str, timeout_sec: int = 120
    ) -> Dict[str, Any]:
        cmd = [
            str(self.binary),
            "--run-metrics",
            "android_startup",
            "--metrics-output=json",
            str(trace_path),
        ]
        try:
            proc = subprocess.run(
                cmd, capture_output=True, text=True, timeout=timeout_sec
            )
        except subprocess.TimeoutExpired:
            return {"error": f"trace_processor timeout after {timeout_sec}s"}
        if proc.returncode != 0:
            return {
                "error": f"trace_processor rc={proc.returncode}: "
                         f"{(proc.stderr or '').strip()[:300]}"
            }

        stdout = (proc.stdout or "").strip()
        if not stdout:
            return {"error": "trace_processor empty output"}
        try:
            data = json.loads(stdout)
        except json.JSONDecodeError as exc:
            return {"error": f"json decode failed: {exc}"}

        startups = (
            data.get("android_startup", {}).get("startup", [])
            if isinstance(data, dict) else []
        )
        if not startups:
            return {"error": "no startup entries in metric"}

        # Prefer an entry whose process.name matches target_package (exact match
        # or substring) but fall back to the first entry so we never drop data.
        chosen = None
        for entry in startups:
            proc_name = (entry.get("process") or {}).get("name") or entry.get("package") or ""
            if proc_name and (proc_name == target_package or target_package in proc_name):
                chosen = entry
                break
        if chosen is None:
            chosen = startups[0]

        result: Dict[str, Any] = {}
        startup_type = chosen.get("startup_type")
        if startup_type:
            result["startup_type"] = startup_type

        to_first_frame = chosen.get("to_first_frame") or {}
        dur_ns = to_first_frame.get("dur_ns")
        if dur_ns is None:
            dur_ms = to_first_frame.get("dur_ms")
            if dur_ms is not None:
                result["to_first_frame_ms"] = float(dur_ms)
        else:
            try:
                result["to_first_frame_ms"] = float(dur_ns) / 1_000_000.0
            except (TypeError, ValueError):
                pass

        proc_meta = chosen.get("process") or {}
        if proc_meta.get("name"):
            result["process_name"] = proc_meta["name"]
        return result

    def extract_custom_slices(
        self, trace_path: Path, slice_names: Tuple[str, ...], timeout_sec: int = 120
    ) -> Dict[str, Any]:
        if not slice_names:
            return {}
        quoted = ",".join(f"'{name}'" for name in slice_names)
        sql = (
            "SELECT name, AVG(dur) / 1e6 AS dur_ms "
            f"FROM slice WHERE name IN ({quoted}) GROUP BY name;"
        )
        cmd = [str(self.binary), "-Q", sql, str(trace_path)]
        try:
            proc = subprocess.run(
                cmd, capture_output=True, text=True, timeout=timeout_sec
            )
        except subprocess.TimeoutExpired:
            return {"error": f"slice query timeout after {timeout_sec}s"}
        if proc.returncode != 0:
            return {
                "error": f"slice query rc={proc.returncode}: "
                         f"{(proc.stderr or '').strip()[:200]}"
            }

        slices: Dict[str, float] = {}
        for line in (proc.stdout or "").splitlines():
            line = line.strip()
            if not line or line.startswith('"name"') or line.startswith("name"):
                continue
            parts = [p.strip().strip('"') for p in line.split(",")]
            if len(parts) < 2:
                continue
            try:
                slices[parts[0]] = float(parts[1])
            except ValueError:
                continue
        return slices


# -----------------------------------------------------------------------------
# Per-iteration capture
# -----------------------------------------------------------------------------

def run_single_launch_capture(
    adb: ADB,
    meta: LaunchModuleMeta,
    iteration: int,
    total_iterations: int,
    trace_dir: Path,
    perfetto_bin: Path,
    perfetto_config: Path,
    device_tag: str,
    *,
    wait_after_force_stop_sec: float,
    trace_start_wait_sec: float,
    logcat_timeout_sec: int,
    post_launch_grace_sec: float,
    trace_stop_timeout_sec: int,
    dry_run: bool,
) -> LaunchCaptureResult:
    result = LaunchCaptureResult(
        app_name=meta.app_name,
        stack=meta.stack,
        flavor=meta.flavor,
        iteration=iteration,
        status="failed",
        reason="unknown",
        started_at=datetime.now().isoformat(timespec="seconds"),
    )
    recorder: Optional[TraceRecorder] = None
    monitor: Optional[LaunchLogcatMonitor] = None
    wall_start = time.time()

    try:
        # [1/10] Force-stop, press home, cooldown
        log("INFO", f"[1/10] Pre-clean: force-stop {meta.package} (iter {iteration}/{total_iterations})")
        adb.force_stop(meta.package)
        adb.press_home()
        time.sleep(wait_after_force_stop_sec)

        # [2/10] Clear logcat
        log("INFO", "[2/10] Clear logcat buffer")
        adb.shell(["logcat", "-c"], timeout=10)

        # [3/10] Start logcat monitor
        log("INFO", "[3/10] Start LaunchPerformance logcat monitor")
        monitor = LaunchLogcatMonitor(serial=adb.serial)
        if not monitor.start() and not dry_run:
            result.status = "failed"
            result.reason = "logcat_spawn_failed"
            return result

        # [4/10] Start perfetto trace
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        trace_name = f"{device_tag}-{meta.module_name}-iter{iteration}-{timestamp}.ptrace"
        trace_path = trace_dir / trace_name
        recorder = TraceRecorder(
            perfetto_bin=perfetto_bin,
            perfetto_config=perfetto_config,
            serial=adb.serial,
            output_path=trace_path,
            dry_run=dry_run,
        )
        log("INFO", f"[4/10] Start Perfetto trace -> {trace_path.name}")
        ok, err = recorder.start()
        if not ok:
            result.status = "failed"
            result.reason = f"trace_start_failed: {err}"
            return result

        # [5/10] Let ftrace warm up
        if trace_start_wait_sec > 0:
            log("INFO", f"[5/10] Wait {trace_start_wait_sec:.1f}s for trace to stabilize")
            time.sleep(trace_start_wait_sec)

        # [6/10] Launch activity with -W timing
        log("INFO", f"[6/10] am start -W {meta.package}/{meta.activity}")
        am_ok, am_out = adb.start_activity(
            package=meta.package,
            activity=meta.activity,
            force_stop=False,
        )
        am_parsed = parse_am_start_output(am_out)
        result.am_status = am_parsed.get("am_status")
        result.am_launch_state = am_parsed.get("am_launch_state")
        result.am_total_time_ms = am_parsed.get("am_total_time_ms")
        result.am_wait_time_ms = am_parsed.get("am_wait_time_ms")
        result.am_this_time_ms = am_parsed.get("am_this_time_ms")
        if not am_ok:
            log("ERROR", f"  am start failed: {am_out[:200]}")
            result.status = "failed"
            result.reason = "am_start_failed"
            return result

        # [7/10] Wait for LaunchPerformance log
        log("INFO", f"[7/10] Wait for LaunchPerformance log (timeout {logcat_timeout_sec}s)")
        logcat_result = monitor.wait(logcat_timeout_sec)
        result.logcat_app_type = logcat_result.get("app_type")
        result.logcat_load_type = logcat_result.get("load")
        result.logcat_start_type = logcat_result.get("start_type")
        result.logcat_duration_ms = logcat_result.get("duration_ms")

        if logcat_result.get("found"):
            log("INFO", f"  Logcat duration: {result.logcat_duration_ms}ms "
                        f"(start_type={result.logcat_start_type})")
            if (result.logcat_load_type
                    and result.logcat_load_type.lower() != meta.flavor.lower()):
                log("WARN", f"  Load mismatch: logcat={result.logcat_load_type} "
                            f"expected={meta.flavor}")
            # Post-launch grace so the trace captures the final frame + async work
            if post_launch_grace_sec > 0:
                log("INFO", f"  Grace {post_launch_grace_sec:.1f}s for trace capture")
                time.sleep(post_launch_grace_sec)
            result.status = "success"
            result.reason = "ok"
        elif logcat_result.get("adb_error"):
            log("ERROR", f"  adb logcat error: {logcat_result['adb_error']}")
            result.status = "failed"
            result.reason = f"adb_error: {logcat_result['adb_error']}"
        else:
            log("WARN", f"  LaunchPerformance log not seen within {logcat_timeout_sec}s")
            result.status = "timeout"
            result.reason = "launch_timeout"

        # [8/10] Stop Perfetto
        log("INFO", "[8/10] Stop Perfetto trace")
        stop_ok, stop_out = recorder.stop(trace_stop_timeout_sec)
        if not dry_run and trace_path.exists():
            size = trace_path.stat().st_size
            result.trace_size_bytes = size
            if size > 0:
                result.trace_path = trace_path
                log("INFO", f"  Trace saved: {trace_path.name} ({size / 1024 / 1024:.2f} MB)")
            else:
                log("ERROR", f"  Trace file is 0 bytes: {trace_path}")
                if result.status == "success":
                    result.status = "failed"
                    result.reason = "trace_empty"
        elif dry_run:
            result.trace_path = trace_path
        else:
            log("ERROR", f"  Trace missing: {trace_path}")
            if result.status == "success":
                result.status = "failed"
                result.reason = "trace_missing"
        if not stop_ok:
            log("WARN", f"  trace stop returned non-zero: {stop_out[:200]}")

    finally:
        if monitor is not None:
            monitor.stop()
        if recorder is not None and recorder.proc is not None and recorder.proc.poll() is None:
            recorder.stop(trace_stop_timeout_sec)
        # [9/10] Cleanup
        log("INFO", "[9/10] Cleanup: force-stop + HOME")
        adb.force_stop(meta.package)
        adb.press_home()
        result.duration_wall_ms = int((time.time() - wall_start) * 1000)

    return result


# -----------------------------------------------------------------------------
# Aggregation & reporting
# -----------------------------------------------------------------------------

def _grade_from_avg(avg: Optional[float], thresholds: Dict[str, float]) -> str:
    if avg is None:
        return "-"
    if avg <= thresholds["excellent"]:
        return "A+"
    if avg <= thresholds["good"]:
        return "A"
    if avg <= thresholds["acceptable"]:
        return "B"
    if avg <= thresholds["poor"]:
        return "C"
    return "D"


def _stats(values: List[float]) -> Dict[str, Optional[float]]:
    if not values:
        return {"count": 0, "avg": None, "min": None, "max": None, "p50": None, "p95": None}
    sorted_values = sorted(values)
    n = len(sorted_values)

    def percentile(p: float) -> float:
        if n == 1:
            return float(sorted_values[0])
        k = (n - 1) * p
        lo = int(k)
        hi = min(lo + 1, n - 1)
        frac = k - lo
        return float(sorted_values[lo] * (1 - frac) + sorted_values[hi] * frac)

    return {
        "count": n,
        "avg": round(statistics.mean(sorted_values), 2),
        "min": round(sorted_values[0], 2),
        "max": round(sorted_values[-1], 2),
        "p50": round(percentile(0.50), 2),
        "p95": round(percentile(0.95), 2),
    }


def _group_by_app_flavor(
    results: List[LaunchCaptureResult],
) -> Dict[Tuple[str, str], List[LaunchCaptureResult]]:
    groups: Dict[Tuple[str, str], List[LaunchCaptureResult]] = {}
    for r in results:
        groups.setdefault((r.app_name, r.flavor), []).append(r)
    return groups


def aggregate_results(
    results: List[LaunchCaptureResult],
    thresholds: Dict[str, float],
) -> List[Dict[str, Any]]:
    groups = _group_by_app_flavor(results)

    summaries: List[Dict[str, Any]] = []
    for (app_name, flavor), group in sorted(groups.items()):
        successes = [r for r in group if r.status == "success"]
        logcat_vals = [r.logcat_duration_ms for r in successes if r.logcat_duration_ms is not None]
        am_vals = [r.am_total_time_ms for r in successes if r.am_total_time_ms is not None]
        ff_vals = [r.perfetto_to_first_frame_ms for r in successes if r.perfetto_to_first_frame_ms is not None]

        startup_type_counts: Dict[str, int] = {}
        for r in group:
            key = r.perfetto_startup_type or r.logcat_start_type or "unknown"
            startup_type_counts[key] = startup_type_counts.get(key, 0) + 1

        reason_counts: Dict[str, int] = {}
        for r in group:
            if r.status != "success":
                reason_counts[r.reason] = reason_counts.get(r.reason, 0) + 1

        slice_means: Dict[str, float] = {}
        for slice_name in CUSTOM_SLICE_NAMES:
            vs = [r.perfetto_custom_slices_ms[slice_name]
                  for r in successes
                  if slice_name in r.perfetto_custom_slices_ms]
            if vs:
                slice_means[slice_name] = round(statistics.mean(vs), 2)

        logcat_stats = _stats([float(v) for v in logcat_vals])
        summaries.append({
            "app_name": app_name,
            "flavor": flavor,
            "total_iterations": len(group),
            "successful": len(successes),
            "logcat_duration_ms": logcat_stats,
            "am_total_time_ms": _stats([float(v) for v in am_vals]),
            "perfetto_to_first_frame_ms": _stats(ff_vals),
            "startup_type_distribution": startup_type_counts,
            "failure_reasons": reason_counts,
            "custom_slice_avg_ms": slice_means,
            "grade": _grade_from_avg(logcat_stats["avg"], thresholds),
        })
    return summaries


_GETPROP_LINE_RE = re.compile(r"^\[([^\]]+)\]:\s*\[([^\]]*)\]$", re.MULTILINE)


def get_device_info(adb: ADB) -> Dict[str, str]:
    props = {
        "brand": "ro.product.brand",
        "model": "ro.product.model",
        "device": "ro.product.device",
        "android_version": "ro.build.version.release",
        "sdk_version": "ro.build.version.sdk",
        "build_id": "ro.build.id",
    }
    proc = adb.shell(["getprop"], timeout=15)
    output = combined_output(proc)
    raw: Dict[str, str] = {m.group(1): m.group(2) for m in _GETPROP_LINE_RE.finditer(output)}
    return {key: raw.get(prop) or "unknown" for key, prop in props.items()}


def _result_to_json(r: LaunchCaptureResult) -> Dict[str, Any]:
    d = asdict(r)
    if r.trace_path is not None:
        d["trace_path"] = str(r.trace_path)
    return d


def generate_json_report(
    output_path: Path,
    device_info: Dict[str, str],
    modules: List[LaunchModuleMeta],
    iterations: int,
    summaries: List[Dict[str, Any]],
    results: List[LaunchCaptureResult],
    thresholds: Dict[str, float],
    run_started_at: str,
    run_finished_at: str,
) -> None:
    payload = {
        "status": "completed",
        "run_started_at": run_started_at,
        "run_finished_at": run_finished_at,
        "device_info": device_info,
        "iterations_per_case": iterations,
        "thresholds_ms": thresholds,
        "apks": [
            {
                "app_name": m.app_name,
                "flavor": m.flavor,
                "package": m.package,
                "activity": m.activity,
                "apk_path": str(m.apk_path),
                "file_size_bytes": m.file_size_bytes,
                "file_mtime": m.file_mtime,
            }
            for m in modules
        ],
        "summaries": summaries,
        "results": [_result_to_json(r) for r in results],
    }
    output_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def _fmt(value: Any) -> str:
    if value is None:
        return "-"
    if isinstance(value, float):
        return f"{value:.1f}"
    return str(value)


def generate_markdown_report(
    output_path: Path,
    device_info: Dict[str, str],
    modules: List[LaunchModuleMeta],
    iterations: int,
    summaries: List[Dict[str, Any]],
    results: List[LaunchCaptureResult],
    thresholds: Dict[str, float],
    run_started_at: str,
    run_finished_at: str,
) -> None:
    brand = device_info.get("brand") or "unknown"
    model = device_info.get("model") or "unknown"
    android_version = device_info.get("android_version") or "unknown"
    sdk_version = device_info.get("sdk_version") or "unknown"
    build_id = device_info.get("build_id") or "unknown"

    lines: List[str] = []
    lines.append(f"# Launch Performance Report — {brand} {model}")
    lines.append("")
    lines.append(f"- Run started: {run_started_at}")
    lines.append(f"- Run finished: {run_finished_at}")
    lines.append(f"- Device: {brand} {model} "
                 f"(Android {android_version} / SDK {sdk_version} / build {build_id})")
    lines.append(f"- Iterations per case: {iterations}")
    lines.append(f"- Thresholds (ms): "
                 f"A+ ≤ {thresholds['excellent']}, A ≤ {thresholds['good']}, "
                 f"B ≤ {thresholds['acceptable']}, C ≤ {thresholds['poor']}, else D")
    lines.append("")

    lines.append("## Overall Summary")
    lines.append("")
    lines.append("| App | Flavor | Success | Logcat avg (ms) | p95 | am TotalTime avg | Perfetto FirstFrame avg | Grade |")
    lines.append("|-----|--------|---------|-----------------|-----|------------------|-------------------------|-------|")
    for s in summaries:
        lc = s["logcat_duration_ms"]
        am = s["am_total_time_ms"]
        ff = s["perfetto_to_first_frame_ms"]
        lines.append(
            f"| {s['app_name']} | {s['flavor']} | "
            f"{s['successful']}/{s['total_iterations']} | "
            f"{_fmt(lc['avg'])} | {_fmt(lc['p95'])} | "
            f"{_fmt(am['avg'])} | {_fmt(ff['avg'])} | "
            f"{s['grade']} |"
        )
    lines.append("")

    # Failure breakdown
    total_fail = sum(1 for r in results if r.status != "success")
    if total_fail > 0:
        lines.append("## Failure Breakdown")
        lines.append("")
        lines.append("| App/Flavor | Reason | Count |")
        lines.append("|------------|--------|-------|")
        for s in summaries:
            for reason, count in s["failure_reasons"].items():
                lines.append(f"| {s['app_name']}/{s['flavor']} | {reason} | {count} |")
        lines.append("")

    # Startup type distribution
    lines.append("## Startup Type Distribution (cold/warm/hot)")
    lines.append("")
    lines.append("| App | Flavor | Distribution |")
    lines.append("|-----|--------|--------------|")
    for s in summaries:
        dist = ", ".join(f"{k}: {v}" for k, v in s["startup_type_distribution"].items())
        lines.append(f"| {s['app_name']} | {s['flavor']} | {dist or '-'} |")
    lines.append("")

    # Custom slices
    lines.append(f"## Custom Trace Slices (avg ms, from Perfetto)")
    lines.append("")
    header = "| App | Flavor | " + " | ".join(CUSTOM_SLICE_NAMES) + " |"
    sep = "|-----|--------|" + "|".join(["---"] * len(CUSTOM_SLICE_NAMES)) + "|"
    lines.append(header)
    lines.append(sep)
    for s in summaries:
        cells = [_fmt(s["custom_slice_avg_ms"].get(n)) for n in CUSTOM_SLICE_NAMES]
        lines.append(f"| {s['app_name']} | {s['flavor']} | " + " | ".join(cells) + " |")
    lines.append("")

    # Per-iteration detail
    lines.append("## Per-iteration Detail")
    lines.append("")
    grouped = _group_by_app_flavor(results)
    for (app_name, flavor), rows in sorted(grouped.items()):
        lines.append(f"### {app_name} / {flavor}")
        lines.append("")
        lines.append("| Iter | Status | Logcat ms | am Total | am Wait | First Frame ms | Start Type | Trace |")
        lines.append("|------|--------|-----------|----------|---------|----------------|------------|-------|")
        for r in sorted(rows, key=lambda x: x.iteration):
            trace_cell = r.trace_path.name if r.trace_path else "-"
            start_type = r.perfetto_startup_type or r.logcat_start_type or "-"
            lines.append(
                f"| {r.iteration} | {r.status} | {_fmt(r.logcat_duration_ms)} | "
                f"{_fmt(r.am_total_time_ms)} | {_fmt(r.am_wait_time_ms)} | "
                f"{_fmt(r.perfetto_to_first_frame_ms)} | {start_type} | {trace_cell} |"
            )
        lines.append("")

    # APK meta table
    lines.append("## APK Metadata")
    lines.append("")
    lines.append("| App | Flavor | Package | APK | Size (MB) | mtime |")
    lines.append("|-----|--------|---------|-----|-----------|-------|")
    for m in modules:
        size_mb = m.file_size_bytes / 1024 / 1024
        lines.append(
            f"| {m.app_name} | {m.flavor} | `{m.package}` | `{m.apk_path.name}` | "
            f"{size_mb:.2f} | {m.file_mtime} |"
        )
    lines.append("")

    output_path.write_text("\n".join(lines), encoding="utf-8")


# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------

def build_arg_parser(repo_root: Path) -> argparse.ArgumentParser:
    default_tools = repo_root / "Tools"
    default_apk_dir = repo_root / "apk-released"
    default_trace_dir = default_tools / "trace" / "launch"
    default_results_dir = default_tools / "results"
    default_registry = default_tools / "launch_apk_registry.json"

    p = argparse.ArgumentParser(description="Auto record Perfetto for launch performance APKs")
    p.add_argument("--serial", default=None, help="ADB device serial")
    p.add_argument("--apk-dir", type=Path, default=default_apk_dir)
    p.add_argument("--trace-dir", type=Path, default=default_trace_dir)
    p.add_argument("--results-dir", type=Path, default=default_results_dir)
    p.add_argument("--perfetto-bin", type=Path, default=default_tools / "record_android_trace")
    p.add_argument("--perfetto-config", type=Path, default=default_tools / "perfetto.config")
    p.add_argument("--trace-processor", type=Path, default=default_tools / "trace_processor_shell")
    p.add_argument("--registry", type=Path, default=default_registry)
    p.add_argument("--apps", nargs="+", default=None, choices=list(DEFAULT_APPS))
    p.add_argument("--flavors", nargs="+", default=None, choices=list(DEFAULT_FLAVORS))
    p.add_argument("--iterations", type=int, default=5)
    p.add_argument("--logcat-timeout-sec", type=int, default=30)
    p.add_argument("--wait-after-force-stop-ms", type=int, default=2000)
    p.add_argument("--trace-start-wait-ms", type=int, default=500)
    p.add_argument("--post-launch-grace-ms", type=int, default=1500)
    p.add_argument("--case-interval-ms", type=int, default=2000,
                   help="Cooldown between iterations (clamped to >= 1500)")
    p.add_argument("--trace-stop-timeout-sec", type=int, default=45)
    p.add_argument("--perfetto-parse-workers", type=int, default=2)
    p.add_argument("--perfetto-parse-timeout-sec", type=int, default=120)
    p.add_argument("--skip-install", action="store_true")
    p.add_argument("--skip-perfetto-parse", action="store_true")
    p.add_argument("--dry-run", action="store_true")
    return p


def parse_trace_metrics(
    extractor: PerfettoMetricExtractor,
    results: List[LaunchCaptureResult],
    workers: int,
    timeout_sec: int,
) -> None:
    """Fill in perfetto_* fields on each success result."""
    targets = [r for r in results if r.trace_path is not None and r.trace_size_bytes and r.trace_size_bytes > 0]
    if not targets:
        return
    log("INFO", f"Parsing {len(targets)} perfetto traces (workers={workers})")

    def parse_one(r: LaunchCaptureResult) -> None:
        startup = extractor.extract_android_startup(
            r.trace_path, target_package=_expected_package(r), timeout_sec=timeout_sec
        )
        slices = extractor.extract_custom_slices(
            r.trace_path, CUSTOM_SLICE_NAMES, timeout_sec=timeout_sec
        )
        if "error" in startup:
            r.perfetto_parse_error = startup["error"]
        else:
            r.perfetto_startup_type = startup.get("startup_type")
            r.perfetto_to_first_frame_ms = startup.get("to_first_frame_ms")
        if isinstance(slices, dict) and "error" not in slices:
            r.perfetto_custom_slices_ms = {k: round(v, 2) for k, v in slices.items()}

    with ThreadPoolExecutor(max_workers=max(1, workers)) as pool:
        for fut in [pool.submit(parse_one, r) for r in targets]:
            try:
                fut.result()
            except Exception as exc:
                log("ERROR", f"trace parse raised: {exc}")


def _expected_package(r: LaunchCaptureResult) -> str:
    # Reconstruct from conventions; used only as a hint for the startup picker.
    stack = r.stack or r.app_name.removeprefix("launch-")
    return f"com.example.launch.{stack}.{r.flavor}"


def main() -> int:
    parser = build_arg_parser(REPO_ROOT)
    args = parser.parse_args()

    for required in (args.perfetto_bin, args.perfetto_config, args.registry):
        if not Path(required).exists():
            log("ERROR", f"Missing file: {required}")
            return 1

    if not args.skip_perfetto_parse and not Path(args.trace_processor).exists():
        log("ERROR", f"trace_processor_shell not found: {args.trace_processor} "
                    f"(use --skip-perfetto-parse to bypass)")
        return 1

    if not args.apk_dir.exists():
        log("ERROR", f"APK dir not found: {args.apk_dir}")
        return 1

    if args.case_interval_ms < 1500:
        log("WARN", f"--case-interval-ms={args.case_interval_ms} is too small; clamp to 1500")
        args.case_interval_ms = 1500

    args.trace_dir.mkdir(parents=True, exist_ok=True)
    args.results_dir.mkdir(parents=True, exist_ok=True)

    log("INFO", f"APK dir: {args.apk_dir}")
    log("INFO", f"Trace dir: {args.trace_dir}")
    log("INFO", f"Results dir: {args.results_dir}")
    log("INFO", f"Iterations: {args.iterations}")
    log("INFO", f"Apps filter: {args.apps or 'all'}")
    log("INFO", f"Flavors filter: {args.flavors or 'all'}")

    registry_apps = load_launch_registry(args.registry)
    modules, skipped = discover_launch_modules(
        registry_apps=registry_apps,
        apk_dir=args.apk_dir,
        repo_root=REPO_ROOT,
        only_apps=args.apps,
        only_flavors=args.flavors,
    )
    if not modules:
        log("ERROR", "No launch APKs discovered")
        for label, reason in skipped:
            log("WARN", f"  skipped {label}: {reason}")
        return 1
    for label, reason in skipped:
        log("WARN", f"skipped {label}: {reason}")

    log("INFO", f"Discovered {len(modules)} APK(s):")
    for m in modules:
        log("INFO", f"  - {m.module_name} ({m.package}) apk={m.apk_path.name} "
                   f"size={m.file_size_bytes / 1024 / 1024:.1f}MB")

    adb = ADB(serial=args.serial, dry_run=args.dry_run)
    if not args.dry_run and not adb.check_device():
        log("ERROR", "No connected adb device")
        return 1

    serial = adb.serial or adb.get_serial()
    model = adb.get_model()
    device_tag = sanitize_for_filename(f"{model}-{serial}")
    log("INFO", f"Device: model={model}, serial={serial}")
    device_info = {} if args.dry_run else get_device_info(adb)

    if not args.skip_install and not args.dry_run:
        log("INFO", "Installing launch APKs ...")
        for m in modules:
            if install_apk(adb, m):
                log("SUCCESS", f"Installed: {m.apk_path.name}")
            else:
                log("ERROR", f"Install failed: {m.apk_path.name}")

    run_started_at = datetime.now().isoformat(timespec="seconds")
    results: List[LaunchCaptureResult] = []
    case_interval_sec = args.case_interval_ms / 1000.0

    for m in modules:
        log("INFO", "=" * 72)
        log("INFO", f"Test: {m.module_name} ({m.package})")
        for i in range(1, args.iterations + 1):
            r = run_single_launch_capture(
                adb=adb,
                meta=m,
                iteration=i,
                total_iterations=args.iterations,
                trace_dir=args.trace_dir,
                perfetto_bin=args.perfetto_bin,
                perfetto_config=args.perfetto_config,
                device_tag=device_tag,
                wait_after_force_stop_sec=args.wait_after_force_stop_ms / 1000.0,
                trace_start_wait_sec=args.trace_start_wait_ms / 1000.0,
                logcat_timeout_sec=args.logcat_timeout_sec,
                post_launch_grace_sec=args.post_launch_grace_ms / 1000.0,
                trace_stop_timeout_sec=args.trace_stop_timeout_sec,
                dry_run=args.dry_run,
            )
            results.append(r)
            if r.status == "success":
                log("SUCCESS", f"  iter {i}: logcat={r.logcat_duration_ms}ms "
                               f"am={r.am_total_time_ms}ms wall={r.duration_wall_ms}ms")
            else:
                log("ERROR", f"  iter {i}: {r.status} ({r.reason})")

            if i < args.iterations:
                log("INFO", f"Case gap: sleep {case_interval_sec:.1f}s")
                time.sleep(case_interval_sec)

        log("INFO", f"Case gap (between apks): sleep {case_interval_sec:.1f}s")
        time.sleep(case_interval_sec)

    # -----------------------------------------------------------------
    # Perfetto post-processing
    # -----------------------------------------------------------------
    if not args.skip_perfetto_parse and not args.dry_run:
        extractor = PerfettoMetricExtractor(binary=args.trace_processor)
        parse_trace_metrics(
            extractor=extractor,
            results=results,
            workers=args.perfetto_parse_workers,
            timeout_sec=args.perfetto_parse_timeout_sec,
        )

    # -----------------------------------------------------------------
    # Report
    # -----------------------------------------------------------------
    run_finished_at = datetime.now().isoformat(timespec="seconds")
    thresholds = {k: float(v) for k, v in DEFAULT_THRESHOLDS_MS.items()}
    summaries = aggregate_results(results, thresholds)

    ts_tag = datetime.now().strftime("%Y%m%d-%H%M%S")
    base_name = f"launch_report_{device_tag}_{ts_tag}"
    json_path = args.results_dir / f"{base_name}.json"
    md_path = args.results_dir / f"{base_name}.md"

    generate_json_report(
        output_path=json_path,
        device_info=device_info,
        modules=modules,
        iterations=args.iterations,
        summaries=summaries,
        results=results,
        thresholds=thresholds,
        run_started_at=run_started_at,
        run_finished_at=run_finished_at,
    )
    generate_markdown_report(
        output_path=md_path,
        device_info=device_info,
        modules=modules,
        iterations=args.iterations,
        summaries=summaries,
        results=results,
        thresholds=thresholds,
        run_started_at=run_started_at,
        run_finished_at=run_finished_at,
    )

    # -----------------------------------------------------------------
    # Summary log
    # -----------------------------------------------------------------
    total = len(results)
    success = sum(1 for r in results if r.status == "success")
    timeout = sum(1 for r in results if r.status == "timeout")
    failed = total - success - timeout

    print()
    log("INFO", "=" * 72)
    log("INFO", "Launch test summary")
    log("INFO", f"  total={total}, success={success}, timeout={timeout}, failed={failed}")
    log("INFO", f"  JSON report: {json_path}")
    log("INFO", f"  MD report:   {md_path}")
    for s in summaries:
        avg = s["logcat_duration_ms"]["avg"]
        avg_str = f"{avg:.1f}ms" if avg is not None else "-"
        log("INFO", f"  {s['app_name']:<16} {s['flavor']:<8} "
                   f"{s['successful']}/{s['total_iterations']} avg={avg_str} grade={s['grade']}")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
