#!/usr/bin/env python3
"""Release scrolling benchmark with Perfetto + gfxinfo.

Workflow per case:
1. Install release APK (optional).
2. Launch app and enter the target load page.
3. Warm up with upward swipes (default: 1, excluded from metrics).
4. Reset `dumpsys gfxinfo` counters.
5. Start Perfetto trace.
6. For a fixed window (default: 20s), run repeated cycle: 5 up swipes + 4 down swipes.
7. Stop Perfetto trace and collect `dumpsys gfxinfo ... framestats`.
8. Parse Perfetto (consumer perspective: sf/app) and emit metrics JSON.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import re
import shlex
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from Tools.auto_record_perfetto_scrolling import (  # noqa: E402
    ADB,
    DEFAULT_EXCLUDED_MODULES,
    ModuleMeta,
    TraceRecorder,
    combined_output,
    detect_manual_permission_reasons,
    detect_motion_after_swipe,
    enter_load_page,
    fallback_swipe_route,
    find_package_viewport,
    find_scrollable_swipe_route,
    install_apk,
    module_supports_activity_type,
    module_supports_load,
    parse_application_id,
    parse_launcher_activity,
    parse_manifest_activities,
    sanitize_for_filename,
    viewport_fallback_swipe_route,
)

RELEASE_DEFAULT_EXCLUDED_MODULES = set(DEFAULT_EXCLUDED_MODULES)
RELEASE_DEFAULT_EXCLUDED_MODULES.add("scrolling-aosp-mixedrender")


LOAD_CHOICES = [
    "minimal",
    "build_light",
    "build_medium",
    "build_heavy",
    "postframe_light",
    "postframe_medium",
    "postframe_heavy",
    "mixed_light",
    "mixed_medium",
    "mixed_heavy",
    "light",
    "medium",
    "heavy",
    "light_between_frames",
    "medium_between_frames",
    "heavy_between_frames",
    "light_mixed",
    "medium_mixed",
    "heavy_mixed",
    "long_frame",
]

DEFAULT_LOADS = ["minimal", "heavy", "heavy_between_frames", "heavy_mixed"]

KNOWN_RELEASE_FLUTTER_APKS = {
    "friends_circle_v19sv_release.apk": {
        "module_name": "friends_circle_v19sv",
        "package_name": "com.example.friendscircle.v19",
        "launcher_activity": "com.example.friendscircle.v19.MainActivity",
        "supported_loads": ("minimal", "build_light", "build_medium", "build_heavy"),
        "load_entry_key": "load",
    },
    "friends_circle_v19tv_release.apk": {
        "module_name": "friends_circle_v19tv",
        "package_name": "com.example.friendscircle.v19.textureview",
        "launcher_activity": "com.example.friendscircle.v19.textureview.MainActivity",
        "supported_loads": ("minimal", "build_light", "build_medium", "build_heavy"),
        "load_entry_key": "load",
    },
}

# Pre-launch warm-up targets: some Flutter apps (Flutter-Engine-version variants)
# are installed out-of-band on the device. Running them once before the formal
# scroll sweep ensures they're resident and JIT-warm. Previously this matrix
# lived in run_perfetto_release_scroll.sh, which made it diverge from the
# Python test registry. Kept in one place now.
FLUTTER_PRELAUNCH_VARIANTS: Dict[str, Dict[str, str]] = {
    "v19": {
        "package": "com.example.friendscircle.v19",
        "activity": ".MainActivity",
        "load_extra_value": "build_heavy",
    },
    "v27": {
        "package": "com.example.friendscircle.v27",
        "activity": ".MainActivity",
        "load_extra_value": "build_heavy",
    },
    "v29": {
        "package": "com.example.friendscircle.v29",
        "activity": ".MainActivity",
        "load_extra_value": "build_heavy",
    },
}


@dataclass
class CaptureOutcome:
    module_name: str
    package_name: str
    load: str
    status: str
    reason: str
    trace_path: Optional[Path]
    metrics_path: Optional[Path]


def _safe_float(value: Any) -> Optional[float]:
    try:
        if value is None:
            return None
        return float(value)
    except (TypeError, ValueError):
        return None


def _format_ratio_as_percent(value: Any, digits: int = 2) -> Any:
    numeric = _safe_float(value)
    if numeric is None:
        return value

    text = f"{numeric * 100:.{digits}f}".rstrip("0").rstrip(".")
    return f"{text}%"


def _resolve_existing_result_file(path_str: str, summary_file: Optional[Path], subdir: str) -> Optional[Path]:
    raw = str(path_str or "").strip()
    if not raw:
        return None

    original = Path(raw)
    if original.exists():
        return original

    if summary_file is None:
        return original

    fallback = summary_file.parent / subdir / original.name
    if fallback.exists():
        return fallback

    return original


def extract_thread_summary_rows(results: Sequence[CaptureOutcome]) -> List[Dict[str, Any]]:
    rows: List[Dict[str, Any]] = []

    for item in results:
        if item.status != "success" or not item.metrics_path or not item.metrics_path.exists():
            continue

        try:
            payload = json.loads(item.metrics_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue

        perfetto = payload.get("perfetto", {})
        if not isinstance(perfetto, dict):
            continue
        thread_metrics = perfetto.get("thread_metrics", {})
        if not isinstance(thread_metrics, dict):
            continue

        app_result = {}
        results_obj = perfetto.get("results", {})
        if isinstance(results_obj, dict):
            app = results_obj.get("app", {})
            if isinstance(app, dict):
                app_result = app

        main = thread_metrics.get("main", {})
        render = thread_metrics.get("render", {})
        if not isinstance(main, dict):
            main = {}
        if not isinstance(render, dict):
            render = {}

        app_fps_window_norm = app_result.get("fps_window_norm")
        if app_fps_window_norm is None:
            app_fps_window_norm = app_result.get("fps_20s_norm")
        app_fps = app_result.get("fps", app_result.get("fps_effective"))
        app_drop_rate = app_result.get("drop_rate")

        rows.append(
            {
                "module_name": item.module_name,
                "load": item.load,
                "metrics_path": str(item.metrics_path),
                "app_fps": app_fps,
                "app_fps_main": app_fps,
                "app_fps_window_norm": app_fps_window_norm,
                "app_fps_20s_norm": app_fps_window_norm,
                "app_fps_effective": app_result.get("fps_effective"),
                "app_drop_rate": app_drop_rate,
                "app_drop_rate_percent": _format_ratio_as_percent(app_drop_rate),
                "app_dropped_frames_vsync_no_consume": app_result.get("dropped_frames_vsync_no_consume"),
                "cluster_tier_count": thread_metrics.get("cluster_tier_count"),
                "cluster_cpu_count": thread_metrics.get("cluster_cpu_count"),
                "main": main,
                "render": render,
            }
        )

    return rows


def extract_gfxinfo_summary_rows(results: Sequence[CaptureOutcome]) -> List[Dict[str, Any]]:
    rows: List[Dict[str, Any]] = []

    for item in results:
        if item.status != "success" or not item.metrics_path or not item.metrics_path.exists():
            continue

        try:
            payload = json.loads(item.metrics_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue

        gfxinfo = payload.get("gfxinfo", {})
        if not isinstance(gfxinfo, dict):
            gfxinfo = {}
        parsed = gfxinfo.get("parsed", {})
        if not isinstance(parsed, dict):
            parsed = {}

        rows.append(
            {
                "module_name": item.module_name,
                "load": item.load,
                "metrics_path": str(item.metrics_path),
                "enabled": bool(gfxinfo.get("enabled", False)),
                "avg_fps": parsed.get("avg_fps"),
                "janky_percent": parsed.get("janky_percent"),
                "janky_frames": parsed.get("janky_frames"),
                "frame_count": parsed.get("frame_count"),
                "sampled_frames": parsed.get("sampled_frames"),
                "p90_ms": parsed.get("p90_ms"),
                "p95_ms": parsed.get("p95_ms"),
                "p99_ms": parsed.get("p99_ms"),
                "selected_sampled_source": parsed.get("selected_sampled_source"),
                "fallback_used": parsed.get("fallback_used"),
            }
        )

    return rows


def summarize_gfxinfo_rows(rows: Sequence[Dict[str, Any]]) -> Dict[str, Any]:
    avg_fps_values = [_safe_float(r.get("avg_fps")) for r in rows]
    janky_percent_values = [_safe_float(r.get("janky_percent")) for r in rows]
    p95_values = [_safe_float(r.get("p95_ms")) for r in rows]
    p99_values = [_safe_float(r.get("p99_ms")) for r in rows]
    frame_count_values = [_safe_float(r.get("frame_count")) for r in rows]

    def _compact(values: Sequence[Optional[float]]) -> List[float]:
        return [v for v in values if v is not None]

    avg_fps_values = _compact(avg_fps_values)
    janky_percent_values = _compact(janky_percent_values)
    p95_values = _compact(p95_values)
    p99_values = _compact(p99_values)
    frame_count_values = _compact(frame_count_values)

    def _mean_or_none(values: Sequence[float]) -> Optional[float]:
        if not values:
            return None
        return round(sum(values) / len(values), 4)

    return {
        "case_count": len(rows),
        "enabled_case_count": sum(1 for r in rows if bool(r.get("enabled"))),
        "avg_of_avg_fps": _mean_or_none(avg_fps_values),
        "avg_janky_percent": _mean_or_none(janky_percent_values),
        "avg_p95_ms": _mean_or_none(p95_values),
        "avg_p99_ms": _mean_or_none(p99_values),
        "avg_frame_count": _mean_or_none(frame_count_values),
    }


def capture_outcomes_from_summary(summary: Dict[str, Any], summary_file: Optional[Path] = None) -> List[CaptureOutcome]:
    outcomes: List[CaptureOutcome] = []
    for item in summary.get("results", []):
        if not isinstance(item, dict):
            continue

        trace_raw = str(item.get("trace_path", "")).strip()
        metrics_raw = str(item.get("metrics_path", "")).strip()
        trace_path = _resolve_existing_result_file(trace_raw, summary_file, "trace")
        metrics_path = _resolve_existing_result_file(metrics_raw, summary_file, "metrics")
        outcomes.append(
            CaptureOutcome(
                module_name=str(item.get("module_name", "")),
                package_name=str(item.get("package_name", "")),
                load=str(item.get("load", "")),
                status=str(item.get("status", "")),
                reason=str(item.get("reason", "")),
                trace_path=trace_path,
                metrics_path=metrics_path,
            )
        )
    return outcomes


def enrich_summary_with_postprocess(summary: Dict[str, Any], outcomes: Sequence[CaptureOutcome]) -> Dict[str, Any]:
    comparisons = summary.get("comparisons", {})
    if not isinstance(comparisons, dict):
        comparisons = {}

    thread_summary_rows = extract_thread_summary_rows(outcomes)
    gfxinfo_summary_rows = extract_gfxinfo_summary_rows(outcomes)
    gfxinfo_overview = summarize_gfxinfo_rows(gfxinfo_summary_rows)

    comparisons["thread_metrics"] = thread_summary_rows
    comparisons["gfxinfo_metrics"] = gfxinfo_summary_rows
    comparisons["gfxinfo_overview"] = gfxinfo_overview
    summary["comparisons"] = comparisons
    return summary


def write_final_run_report(
    summary: Dict[str, Any],
    summary_file: Path,
    comparison_csv: Optional[Path],
) -> Path:
    run_root = summary_file.parent.parent
    run_root.mkdir(parents=True, exist_ok=True)

    results = summary.get("results", [])
    success = sum(1 for r in results if isinstance(r, dict) and r.get("status") == "success")
    failed = sum(1 for r in results if isinstance(r, dict) and r.get("status") == "failed")
    skipped = sum(1 for r in results if isinstance(r, dict) and r.get("status") == "skipped")

    timestamp = str(summary.get("timestamp", datetime.now().strftime("%Y%m%d-%H%M%S")))
    final_payload = {
        "timestamp": timestamp,
        "device": summary.get("device", {}),
        "config": summary.get("config", {}),
        "totals": {
            "total": len(results) if isinstance(results, list) else 0,
            "success": success,
            "failed": failed,
            "skipped": skipped,
        },
        "artifacts": {
            "summary_json": str(summary_file),
            "comparison_csv": str(comparison_csv) if comparison_csv else "",
            "run_root": str(run_root),
            "result_dir": str(summary_file.parent),
            "trace_dir": str(run_root / "trace"),
            "log_dir": str(run_root / "logs"),
        },
        "comparisons": summary.get("comparisons", {}),
        "results": results,
    }

    final_path = run_root / f"final-summary-{timestamp}.json"
    final_path.write_text(json.dumps(final_payload, indent=2), encoding="utf-8")
    return final_path


def postprocess_existing_summary(summary_file: Path) -> Tuple[Path, Optional[Path], Path]:
    payload = json.loads(summary_file.read_text(encoding="utf-8"))
    outcomes = capture_outcomes_from_summary(payload, summary_file)
    payload = enrich_summary_with_postprocess(payload, outcomes)
    summary_file.write_text(json.dumps(payload, indent=2), encoding="utf-8")

    comparison_csv: Optional[Path] = None
    try:
        comparison_csv = export_default_comparison_csv(payload, summary_file)
    except OSError as exc:
        log("WARN", f"Failed to write comparison CSV: {exc}")

    final_report = write_final_run_report(payload, summary_file, comparison_csv)
    return summary_file, comparison_csv, final_report


# CSV field schema: ordered list of (header_cn, extractor). Extractor receives
# a context dict and returns the cell value. Adding/removing a metric = edit
# this list in one place; ordering of the CSV columns is the list order.
#
# Context dict fields:
#   result            summary["results"] entry
#   metrics_path      resolved Path to per-case metrics.json
#   app               payload["perfetto"]["results"]["app"]
#   main / render     payload["perfetto"]["thread_metrics"][kind]
#   gfx               payload["gfxinfo"]["parsed"]
CSV_FIELD_DEFINITIONS: List[Tuple[str, Any]] = [
    ("模块",                   lambda c: c["result"].get("module_name", "")),
    ("负载",                   lambda c: c["result"].get("load", "")),
    ("FPS(主指标)",            lambda c: c["app"].get("fps", c["app"].get("fps_effective", ""))),
    ("掉帧率",                 lambda c: _format_ratio_as_percent(c["app"].get("drop_rate", ""))),
    ("VSync无消费掉帧",        lambda c: c["app"].get("dropped_frames_vsync_no_consume", "")),
    ("BufferTX跳变抛弃掉帧",   lambda c: c["app"].get("dropped_frames_buffertx_jump", "")),
    ("展示帧(分子)",           lambda c: c["app"].get("presented_frames", "")),
    ("VSync机会帧(分母)",      lambda c: c["app"].get("sf_vsync_ticks", "")),
    ("消费帧(原始)",           lambda c: c["app"].get("buffertx_consumed_frames", "")),
    ("BufferTX抛弃帧(跳变)",   lambda c: c["app"].get("buffertx_discarded_frames", "")),
    ("滑动区间时长(秒)",       lambda c: c["app"].get("effective_window_sec", "")),
    ("FPS来源",                lambda c: c["app"].get("fps_source", "")),
    ("窗口归一FPS(参考)",      lambda c: c["app"].get("fps_window_norm", c["app"].get("fps_20s_norm"))),
    ("有效区间FPS(参考)",      lambda c: c["app"].get("fps_effective", "")),
    ("BufferTX大跳变次数",     lambda c: c["app"].get("buffertx_large_negative_jump_events", "")),
    ("BufferTX大跳变帧数",     lambda c: c["app"].get("buffertx_large_negative_jump_frames", "")),
    ("BufferTX单次最大消费帧", lambda c: c["app"].get("buffertx_max_single_dequeue_frames", "")),
    ("时间线展示帧(参考)",     lambda c: c["app"].get("timeline_presented_frames", "")),
    ("时间线总帧(参考)",       lambda c: c["app"].get("timeline_total_frames", "")),
    ("主线程运行时间ms",       lambda c: c["main"].get("running_time_ms", "")),
    ("主线程可运行时间ms",     lambda c: c["main"].get("runnable_time_ms", "")),
    ("渲染线程运行时间ms",     lambda c: c["render"].get("running_time_ms", "")),
    ("渲染线程可运行时间ms",   lambda c: c["render"].get("runnable_time_ms", "")),
    ("gfxinfo平均FPS",         lambda c: c["gfx"].get("avg_fps", "")),
    ("gfxinfo卡顿率",          lambda c: c["gfx"].get("janky_percent", "")),
    ("gfxinfo卡顿帧数",        lambda c: c["gfx"].get("janky_frames", "")),
    ("gfxinfo总帧数",          lambda c: c["gfx"].get("frame_count", "")),
    ("gfxinfo采样帧数",        lambda c: c["gfx"].get("sampled_frames", "")),
    ("gfxinfoP90(ms)",         lambda c: c["gfx"].get("p90_ms", "")),
    ("gfxinfoP95(ms)",         lambda c: c["gfx"].get("p95_ms", "")),
    ("gfxinfoP99(ms)",         lambda c: c["gfx"].get("p99_ms", "")),
    ("gfxinfo样本来源",        lambda c: c["gfx"].get("selected_sampled_source", "")),
    ("gfxinfo回退解析",        lambda c: c["gfx"].get("fallback_used", "")),
    ("指标文件",               lambda c: str(c["metrics_path"])),
]


def _as_dict(value: Any) -> Dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _build_csv_row_context(result: Dict[str, Any], metrics_path: Path) -> Optional[Dict[str, Any]]:
    try:
        payload = json.loads(metrics_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    perfetto = _as_dict(_as_dict(payload).get("perfetto"))
    thread_metrics = _as_dict(perfetto.get("thread_metrics"))
    gfxinfo = _as_dict(_as_dict(payload).get("gfxinfo"))
    return {
        "result": result,
        "metrics_path": metrics_path,
        "app": _as_dict(_as_dict(perfetto.get("results")).get("app")),
        "main": _as_dict(thread_metrics.get("main")),
        "render": _as_dict(thread_metrics.get("render")),
        "gfx": _as_dict(gfxinfo.get("parsed")),
    }


def export_default_comparison_csv(summary: Dict[str, Any], summary_file: Path) -> Path:
    timestamp = str(summary.get("timestamp", "latest"))
    device = summary.get("device", {})
    model = str(device.get("model", "unknown-device")) if isinstance(device, dict) else "unknown-device"
    serial = str(device.get("serial", "unknown-serial")) if isinstance(device, dict) else "unknown-serial"
    device_name = sanitize_for_filename(model)
    if serial and serial != "unknown-serial":
        device_name = sanitize_for_filename(f"{device_name}-{serial}")
    out_path = summary_file.parent / f"perfetto-fps-thread-comparison-{device_name}-{timestamp}.csv"

    fields = [header for header, _ in CSV_FIELD_DEFINITIONS]
    rows: List[Dict[str, Any]] = []
    for result in summary.get("results", []):
        metrics_path_str = str(result.get("metrics_path", "")).strip()
        metrics_path = _resolve_existing_result_file(metrics_path_str, summary_file, "metrics")
        if metrics_path is None or not metrics_path.exists():
            continue
        ctx = _build_csv_row_context(result, metrics_path)
        if ctx is None:
            continue
        rows.append({header: extractor(ctx) for header, extractor in CSV_FIELD_DEFINITIONS})

    with out_path.open("w", newline="", encoding="utf-8-sig") as fp:
        writer = csv.DictWriter(fp, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)

    return out_path


def now_ts() -> str:
    return datetime.now().strftime("%H:%M:%S")


def log(level: str, msg: str) -> None:
    print(f"[{now_ts()}] [{level}] {msg}")


def quote_sql(value: str) -> str:
    return value.replace("'", "''")


def percentile(values: List[float], p: float) -> float:
    if not values:
        return 0.0
    sorted_vals = sorted(values)
    rank = max(0, min(len(sorted_vals) - 1, int(math.ceil(p * len(sorted_vals))) - 1))
    return sorted_vals[rank]


def discover_release_modules(
    repo_root: Path,
    apk_dir: Path,
    only_tokens: Sequence[str],
    include_manual_permission_modules: bool,
) -> List[ModuleMeta]:
    def _manual_release_meta(apk_path: Path) -> Optional[ModuleMeta]:
        config = KNOWN_RELEASE_FLUTTER_APKS.get(apk_path.name)
        if not config:
            return None

        return ModuleMeta(
            module_name=config["module_name"],
            apk_path=apk_path,
            module_dir=repo_root / "Tools" / "apks",
            package_name=config["package_name"],
            launcher_activity=config["launcher_activity"],
            manifest_activities=[config["launcher_activity"]],
            supports_activity_type=False,
            supported_loads=config["supported_loads"],
            load_entry_key=config["load_entry_key"],
        )

    search_dirs = [apk_dir]
    legacy_apk_dir = repo_root / "Tools" / "apks"
    if legacy_apk_dir.is_dir() and legacy_apk_dir.resolve() != apk_dir.resolve():
        search_dirs.append(legacy_apk_dir)

    seen_names = set()
    apks: List[Path] = []
    for directory in search_dirs:
        if not directory.is_dir():
            continue
        patterns = (
            "scrolling-*-release-*.apk",
            "friends_circle_v19sv_release.apk",
            "friends_circle_v19tv_release.apk",
        )
        for pattern in patterns:
            for apk_path in sorted(directory.glob(pattern)):
                if apk_path.name in seen_names:
                    continue
                seen_names.add(apk_path.name)
                apks.append(apk_path)

    only = [x.strip() for x in only_tokens if x.strip()]
    modules: List[ModuleMeta] = []
    for apk_path in apks:
        module_name = None
        config = KNOWN_RELEASE_FLUTTER_APKS.get(apk_path.name)
        if config:
            module_name = config["module_name"]
        else:
            module_name = apk_path.name.split("-release-")[0]

        if only and not any(
            token == module_name or token == apk_path.name or token in module_name or token in apk_path.name
            for token in only
        ):
            continue

        manual_meta = _manual_release_meta(apk_path)
        if manual_meta:
            modules.append(manual_meta)
            continue

        if module_name in RELEASE_DEFAULT_EXCLUDED_MODULES:
            log("WARN", f"Skip {apk_path.name}: excluded by default policy ({module_name})")
            continue

        module_dir = repo_root / module_name
        if not module_dir.exists():
            log("WARN", f"Skip {apk_path.name}: module dir not found")
            continue

        manual_permission_reasons = detect_manual_permission_reasons(module_dir)
        if manual_permission_reasons and not include_manual_permission_modules:
            reason_text = ", ".join(manual_permission_reasons)
            log(
                "WARN",
                f"Skip {apk_path.name}: requires manual permission confirmation ({reason_text})",
            )
            continue

        package_name = parse_application_id(module_dir)
        launcher_activity = parse_launcher_activity(module_dir)
        if not package_name or not launcher_activity:
            log("WARN", f"Skip {apk_path.name}: cannot parse package/activity")
            continue

        manifest_activities = parse_manifest_activities(module_dir)
        modules.append(
            ModuleMeta(
                module_name=module_name,
                apk_path=apk_path,
                module_dir=module_dir,
                package_name=package_name,
                launcher_activity=launcher_activity,
                manifest_activities=manifest_activities,
                supports_activity_type=module_supports_activity_type(module_dir),
            )
        )
    return modules


_REFRESH_RATE_PATTERNS = (
    re.compile(r"peakRefreshRate\s*=\s*([0-9]+(?:\.[0-9]+)?)"),
    re.compile(r"mRefreshRate\s*=\s*([0-9]+(?:\.[0-9]+)?)"),
    re.compile(r"refreshRate\s*=\s*([0-9]+(?:\.[0-9]+)?)\s*Hz"),
)


def detect_device_refresh_rate(adb: ADB, fallback: int = 120) -> Tuple[int, str]:
    """Return (refresh_rate_hz, source) for the target device.

    Tries `dumpsys display` first, picking the highest reported refresh rate
    (peakRefreshRate reflects the device's physical capability, which is what
    we want for janky% budget: a 120Hz device rendering at 60Hz still has an
    8.33ms budget if we care about "is any frame slower than the max rate".)
    Falls back to the provided value if detection fails.
    """
    try:
        proc = adb.shell(["dumpsys", "display"], timeout=15)
    except Exception as exc:
        return fallback, f"fallback ({exc})"
    output = combined_output(proc)
    if not output:
        return fallback, "fallback (empty dumpsys display)"

    candidates: List[float] = []
    for pattern in _REFRESH_RATE_PATTERNS:
        for match in pattern.finditer(output):
            try:
                candidates.append(float(match.group(1)))
            except ValueError:
                continue
    if not candidates:
        return fallback, "fallback (no refresh rate in dumpsys)"

    peak = max(candidates)
    detected = int(round(peak))
    if detected <= 0:
        return fallback, f"fallback (invalid peak={peak})"
    return detected, f"dumpsys display peak={peak}"


def prelaunch_flutter_variants(
    adb: ADB,
    variants: List[str],
    settle_sec: float = 1.0,
) -> None:
    """Warm up Flutter engine variants before the formal benchmark sweep.

    Expands 'all' to every entry in FLUTTER_PRELAUNCH_VARIANTS. No-op if the
    list is empty. Failures are logged but non-fatal: the target apps may not
    be installed on every device, and a missing warm-up is better than an
    aborted benchmark.
    """
    if not variants:
        return
    expanded: List[str] = []
    for v in variants:
        if v == "all":
            expanded.extend(FLUTTER_PRELAUNCH_VARIANTS.keys())
        else:
            expanded.append(v)
    # Preserve declaration order, deduplicate.
    seen: set = set()
    ordered: List[str] = []
    for v in expanded:
        if v not in seen:
            ordered.append(v)
            seen.add(v)

    for variant in ordered:
        cfg = FLUTTER_PRELAUNCH_VARIANTS.get(variant)
        if not cfg:
            log("WARN", f"Flutter prelaunch: unknown variant {variant}")
            continue
        pkg, act, extra = cfg["package"], cfg["activity"], cfg["load_extra_value"]
        log("INFO", f"Flutter prelaunch: {pkg}/{act} load={extra}")
        ok, out = adb.start_activity(
            package=pkg,
            activity=act,
            string_extras={"load": extra},
        )
        if not ok:
            log("WARN", f"Flutter prelaunch failed for {variant}: {out[:200]}")
        if settle_sec > 0:
            time.sleep(settle_sec)


def reset_gfxinfo(adb: ADB, package_name: str) -> Tuple[bool, str]:
    proc = adb.shell(["dumpsys", "gfxinfo", package_name, "reset"], timeout=30)
    return proc.returncode == 0, combined_output(proc)


def collect_gfxinfo(adb: ADB, package_name: str) -> Tuple[bool, str]:
    proc = adb.shell(["dumpsys", "gfxinfo", package_name, "framestats"], timeout=60)
    return proc.returncode == 0, combined_output(proc)


def parse_gfxinfo_output(gfxinfo_output: str, target_fps: int = 120) -> Dict[str, Any]:
    result: Dict[str, Any] = {
        "total_frames": 0,
        "frame_count": 0,
        "frame_count_source": "none",
        "uptime_ms": 0,
        "stats_since_ns": 0,
        "stats_window_ms": 0.0,
        "summary_total_frames": 0,
        "sampled_frames": 0,
        "profiledata_sampled_frames": 0,
        "histogram_sampled_frames": 0,
        "selected_sampled_frames": 0,
        "selected_sampled_source": "none",
        "janky_frames": 0,
        "janky_percent": 0.0,
        "legacy_janky_frames": 0,
        "legacy_janky_percent": 0.0,
        "avg_fps": 0.0,
        "p90_ms": 0.0,
        "p95_ms": 0.0,
        "p99_ms": 0.0,
        "frame_budget_ms": round(1000.0 / target_fps, 4),
        "timing_source": "none",
        "fallback_used": False,
    }

    lines = gfxinfo_output.splitlines()
    in_profile = False
    header_map: Optional[Dict[str, int]] = None
    frame_durations_ms: List[float] = []
    histogram_durations_ms: List[float] = []
    summary_percentiles: Dict[str, float] = {}
    histogram_seen = False

    for raw_line in lines:
        line = raw_line.strip()
        if not line:
            continue

        if line.startswith("Uptime:"):
            match = re.search(r"Uptime:\s*(\d+)\s+Realtime:\s*(\d+)", line)
            if match:
                result["uptime_ms"] = int(match.group(1))
            continue

        if line.startswith("Stats since:") and result["stats_since_ns"] <= 0:
            match = re.search(r"Stats since:\s*(\d+)ns", line)
            if match:
                result["stats_since_ns"] = int(match.group(1))
            continue

        if "Total frames rendered:" in line:
            try:
                total_frames = int(line.split(":", 1)[1].strip())
                result["total_frames"] = total_frames
                result["summary_total_frames"] = total_frames
            except (IndexError, ValueError):
                pass
            continue

        if "Janky frames:" in line and result["janky_frames"] == 0:
            try:
                left, right = line.split(":", 1)[1].split("(", 1)
                result["janky_frames"] = int(left.strip())
                result["janky_percent"] = float(right.strip().rstrip(")").rstrip("%"))
            except (IndexError, ValueError):
                pass
            continue

        if "Janky frames (legacy):" in line:
            try:
                left, right = line.split(":", 1)[1].split("(", 1)
                result["legacy_janky_frames"] = int(left.strip())
                result["legacy_janky_percent"] = float(right.strip().rstrip(")").rstrip("%"))
            except (IndexError, ValueError):
                pass
            continue

        if line.startswith(("90th percentile:", "95th percentile:", "99th percentile:")):
            match = re.match(r"(90th|95th|99th) percentile:\s*([0-9.]+)ms", line)
            if match:
                summary_percentiles[match.group(1)] = float(match.group(2))
            continue

        if line.startswith("HISTOGRAM:"):
            if histogram_seen:
                continue
            histogram_seen = True
            for ms_text, count_text in re.findall(r"([0-9]+)ms=([0-9]+)", line):
                count = int(count_text)
                if count <= 0:
                    continue
                duration = float(ms_text)
                histogram_durations_ms.extend([duration] * count)
            continue

        if line.startswith("---PROFILEDATA---"):
            in_profile = not in_profile
            header_map = None
            continue

        if not in_profile:
            continue

        if line.startswith("Flags,"):
            headers = [x.strip() for x in line.split(",") if x.strip()]
            header_map = {name: idx for idx, name in enumerate(headers)}
            continue

        if header_map is None or not line[0].isdigit():
            continue

        parts = [x.strip() for x in line.split(",")]
        start_col = "IntendedVsync" if "IntendedVsync" in header_map else "Vsync"
        if "Flags" not in header_map or start_col not in header_map:
            continue

        idx_flags = header_map["Flags"]
        idx_start = header_map[start_col]

        if idx_flags >= len(parts) or idx_start >= len(parts):
            continue

        try:
            flags = int(parts[idx_flags])
            frame_start = int(parts[idx_start])
        except ValueError:
            continue

        if flags != 0:
            continue
        if frame_start <= 0:
            continue

        completion_candidates = [
            "FrameCompleted",
            "SwapBuffersCompleted",
            "GpuCompleted",
            "SwapBuffers",
            "DisplayPresentTime",
        ]
        completion_ts: Optional[int] = None
        for key in completion_candidates:
            idx = header_map.get(key)
            if idx is None or idx >= len(parts):
                continue
            raw_value = parts[idx]
            if not raw_value:
                continue
            try:
                ts = int(raw_value)
            except ValueError:
                continue
            if ts > frame_start:
                completion_ts = ts
                break

        if completion_ts is None:
            continue

        duration_ms = (completion_ts - frame_start) / 1_000_000.0
        if 0 < duration_ms <= 1000:
            frame_durations_ms.append(duration_ms)

    result["profiledata_sampled_frames"] = len(frame_durations_ms)
    result["histogram_sampled_frames"] = len(histogram_durations_ms)

    # PROFILEDATA is the fine-grained source (per-frame timestamps); histogram
    # is coarse. Accept PROFILEDATA as long as (a) it has at least 10 samples
    # AND (b) it covers >= 50% of the summary total — previous 30 / 60%
    # threshold was over-strict and unnecessarily forced short runs back to
    # histogram fallback.
    use_profiledata = False
    if frame_durations_ms:
        total_frames = result["summary_total_frames"]
        min_required = 10
        if total_frames > 0:
            min_required = max(min_required, int(total_frames * 0.5))
        use_profiledata = len(frame_durations_ms) >= min_required

    if use_profiledata:
        result["selected_sampled_frames"] = len(frame_durations_ms)
        result["selected_sampled_source"] = "profiledata"
        budget_ms = result["frame_budget_ms"]
        janky = sum(1 for d in frame_durations_ms if d > budget_ms)
        result["janky_frames"] = janky
        result["janky_percent"] = round((janky / len(frame_durations_ms)) * 100.0, 4)
        avg_frame_time = sum(frame_durations_ms) / len(frame_durations_ms)
        fps = 1000.0 / avg_frame_time if avg_frame_time > 0 else 0.0
        result["avg_fps"] = round(min(float(target_fps), fps), 3)
        result["p90_ms"] = round(percentile(frame_durations_ms, 0.90), 4)
        result["p95_ms"] = round(percentile(frame_durations_ms, 0.95), 4)
        result["p99_ms"] = round(percentile(frame_durations_ms, 0.99), 4)
        result["timing_source"] = "profiledata"

        if result["summary_total_frames"] <= 0:
            result["summary_total_frames"] = len(frame_durations_ms)
    elif histogram_durations_ms:
        result["selected_sampled_frames"] = len(histogram_durations_ms)
        result["selected_sampled_source"] = "histogram"
        budget_ms = result["frame_budget_ms"]
        janky = sum(1 for d in histogram_durations_ms if d > budget_ms)
        result["janky_frames"] = janky
        result["janky_percent"] = round((janky / len(histogram_durations_ms)) * 100.0, 4)
        avg_frame_time = sum(histogram_durations_ms) / len(histogram_durations_ms)
        fps = 1000.0 / avg_frame_time if avg_frame_time > 0 else 0.0
        result["avg_fps"] = round(min(float(target_fps), fps), 3)
        result["p90_ms"] = round(summary_percentiles.get("90th", percentile(histogram_durations_ms, 0.90)), 4)
        result["p95_ms"] = round(summary_percentiles.get("95th", percentile(histogram_durations_ms, 0.95)), 4)
        result["p99_ms"] = round(summary_percentiles.get("99th", percentile(histogram_durations_ms, 0.99)), 4)
        result["timing_source"] = "histogram"
        result["fallback_used"] = True

        if result["summary_total_frames"] <= 0:
            result["summary_total_frames"] = len(histogram_durations_ms)
    else:
        if "90th" in summary_percentiles:
            result["p90_ms"] = round(summary_percentiles["90th"], 4)
        if "95th" in summary_percentiles:
            result["p95_ms"] = round(summary_percentiles["95th"], 4)
        if "99th" in summary_percentiles:
            result["p99_ms"] = round(summary_percentiles["99th"], 4)
        result["selected_sampled_source"] = "summary_only"
        result["timing_source"] = "summary_only"
        result["fallback_used"] = True

    result["sampled_frames"] = result["selected_sampled_frames"]
    result["total_frames"] = result["summary_total_frames"]
    if result["uptime_ms"] > 0 and result["stats_since_ns"] > 0:
        window_ms = result["uptime_ms"] - (result["stats_since_ns"] / 1_000_000.0)
        if window_ms > 0:
            result["stats_window_ms"] = round(window_ms, 3)

    if result["summary_total_frames"] > 0:
        result["frame_count"] = result["summary_total_frames"]
        result["frame_count_source"] = "summary_total_frames"
    elif result["profiledata_sampled_frames"] > 0:
        result["frame_count"] = result["profiledata_sampled_frames"]
        result["frame_count_source"] = "profiledata_sampled_frames"
    elif result["histogram_sampled_frames"] > 0:
        result["frame_count"] = result["histogram_sampled_frames"]
        result["frame_count_source"] = "histogram_sampled_frames"

    return result


def install_release_apk_with_recovery(adb: ADB, meta: ModuleMeta) -> bool:
    if install_apk(adb, meta):
        return True

    log("WARN", f"Install failed, uninstall old app then retry: {meta.package_name}")
    uninstall_proc = adb.run(["uninstall", meta.package_name], timeout=60)
    uninstall_out = combined_output(uninstall_proc)
    if uninstall_out and "Unknown package" not in uninstall_out:
        log("WARN", f"Uninstall output: {uninstall_out}")

    retry_proc = adb.run(["install", "-r", str(meta.apk_path)], timeout=240)
    retry_out = combined_output(retry_proc)
    if retry_proc.returncode != 0 and retry_out:
        log("ERROR", f"Install retry failed: {retry_out}")
    return retry_proc.returncode == 0 and ("Success" in retry_out or retry_out == "")


def resolve_swipe_route(
    adb: ADB,
    meta: ModuleMeta,
    screen_size: Tuple[int, int],
    swipe_x_ratio: float,
    swipe_start_y_ratio: float,
    swipe_end_y_ratio: float,
) -> Tuple[Tuple[int, int, int], str]:
    if not adb.dry_run:
        xml_text = adb.dump_uixml()
        if xml_text:
            found = find_scrollable_swipe_route(xml_text, meta.package_name)
            if found:
                x, y_start, y_end, klass = found
                return (x, y_start, y_end), f"scrollable:{klass}"

            viewport = find_package_viewport(xml_text, meta.package_name)
            if viewport:
                return (
                    viewport_fallback_swipe_route(
                        viewport,
                        swipe_x_ratio,
                        swipe_start_y_ratio,
                        swipe_end_y_ratio,
                    ),
                    "package_viewport_fallback",
                )

    return (
        fallback_swipe_route(screen_size, swipe_x_ratio, swipe_start_y_ratio, swipe_end_y_ratio),
        "ratio_fallback",
    )


def force_centered_swipe_route(
    route: Tuple[int, int, int],
    screen_size: Tuple[int, int],
) -> Tuple[int, int, int]:
    x, _, _ = route
    width, height = screen_size

    center_y = height // 2
    top_guard = int(height * 0.16)
    bottom_guard = height - top_guard
    min_span = int(height * 0.38)
    target_span = int(height * 0.50)

    max_span = max(0, bottom_guard - top_guard)
    span = min(max_span, max(min_span, target_span))
    half = span // 2

    y_start = center_y + half
    y_end = center_y - half
    y_start = min(bottom_guard, y_start)
    y_end = max(top_guard, y_end)

    min_x = int(width * 0.20)
    max_x = int(width * 0.80)
    x = max(min_x, min(max_x, x))

    if y_start <= y_end:
        y_start = min(bottom_guard, center_y + int(height * 0.18))
        y_end = max(top_guard, center_y - int(height * 0.18))

    return x, y_start, y_end


def run_warmup_up_swipes(
    adb: ADB,
    route: Tuple[int, int, int],
    warmup_up_swipes: int,
    swipe_duration_ms: int,
    swipe_interval_sec: float,
) -> None:
    if warmup_up_swipes <= 0:
        return

    x, y_start, y_end = route
    for i in range(warmup_up_swipes):
        log("INFO", f"Warmup swipe {i + 1}/{warmup_up_swipes} (up)")
        proc = adb.swipe(x, y_start, x, y_end, swipe_duration_ms)
        if proc.returncode != 0:
            log("WARN", f"Warmup swipe failed: {combined_output(proc)}")
        if i < warmup_up_swipes - 1 and swipe_interval_sec > 0:
            time.sleep(swipe_interval_sec)


def run_formal_scroll_loop(
    adb: ADB,
    route: Tuple[int, int, int],
    trace_seconds: int,
    up_block_size: int,
    down_block_size: int,
    swipe_duration_ms: int,
    swipe_interval_sec: float,
    motion_check_enabled: bool,
) -> Dict[str, Any]:
    x, y_start, y_end = route
    total_swipes = 0
    up_swipes = 0
    down_swipes = 0
    motion_detected = False
    prev_hash = adb.screenshot_hash() if motion_check_enabled else None
    cycle_size = up_block_size + down_block_size

    t0 = time.monotonic()
    deadline = t0 + trace_seconds

    while time.monotonic() < deadline:
        step = total_swipes % cycle_size
        is_up = step < up_block_size
        if is_up:
            proc = adb.swipe(x, y_start, x, y_end, swipe_duration_ms)
            up_swipes += 1
            direction = "up"
        else:
            proc = adb.swipe(x, y_end, x, y_start, swipe_duration_ms)
            down_swipes += 1
            direction = "down"

        total_swipes += 1
        if proc.returncode != 0:
            log("WARN", f"Swipe failed ({direction}): {combined_output(proc)}")

        if motion_check_enabled:
            time.sleep(0.3)
            moved, prev_hash = detect_motion_after_swipe(adb, prev_hash)
            if moved:
                motion_detected = True

        remaining = deadline - time.monotonic()
        if remaining <= 0:
            break
        if swipe_interval_sec > 0:
            time.sleep(min(swipe_interval_sec, remaining))

    elapsed = time.monotonic() - t0
    return {
        "requested_window_sec": trace_seconds,
        "actual_window_sec": round(elapsed, 6),
        "scroll_pattern": f"up{up_block_size}_down{down_block_size}",
        "total_swipes": total_swipes,
        "up_swipes": up_swipes,
        "down_swipes": down_swipes,
        "motion_detected": motion_detected,
    }


def render_template(template_text: str, mapping: Dict[str, str]) -> str:
    output = template_text
    for key, value in mapping.items():
        output = output.replace(f"__{key}__", value)
    return output


def parse_trace_processor_payload(stdout_text: str) -> Dict[str, Any]:
    lines = [line.strip() for line in stdout_text.splitlines() if line.strip()]
    if len(lines) < 2:
        raise ValueError("trace_processor stdout has no data row")

    payload_line = lines[-1]
    if payload_line == "NULL":
        return {}

    if payload_line.startswith('"') and payload_line.endswith('"') and len(payload_line) >= 2:
        payload_line = payload_line[1:-1]

    if not payload_line:
        return {}

    return json.loads(payload_line)


class TraceProcessorError(RuntimeError):
    """Trace processor invocation failed. `reason_code` is a short tag:
    trace_parse_timeout / trace_parse_failed / trace_missing / trace_empty."""

    def __init__(self, reason_code: str, message: str):
        super().__init__(message)
        self.reason_code = reason_code


def run_trace_processor_query(
    trace_processor_bin: Path,
    trace_path: Path,
    rendered_sql: str,
    rendered_sql_path: Path,
    timeout_sec: int = 300,
) -> Dict[str, Any]:
    rendered_sql_path.parent.mkdir(parents=True, exist_ok=True)
    rendered_sql_path.write_text(rendered_sql, encoding="utf-8")

    if not trace_path.exists():
        raise TraceProcessorError("trace_missing", f"trace file not found: {trace_path}")
    if trace_path.stat().st_size == 0:
        raise TraceProcessorError("trace_empty", f"trace file is 0 bytes: {trace_path}")

    cmd = [str(trace_processor_bin), "-q", str(rendered_sql_path), str(trace_path)]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout_sec)
    except subprocess.TimeoutExpired as exc:
        raise TraceProcessorError(
            "trace_parse_timeout",
            f"trace_processor timed out after {timeout_sec}s: "
            + " ".join(shlex.quote(x) for x in cmd)
            + f" | stderr={(exc.stderr or '')[:200] if exc.stderr else ''}",
        ) from exc

    if proc.returncode != 0:
        stderr = (proc.stderr or "").strip()
        stdout = (proc.stdout or "").strip()
        raise TraceProcessorError(
            "trace_parse_failed",
            "trace_processor query failed: "
            + " ".join(shlex.quote(x) for x in cmd)
            + f" | stdout={stdout[:200]} | stderr={stderr[:300]}",
        )

    return parse_trace_processor_payload(proc.stdout)


def parse_perfetto_metrics(
    trace_path: Path,
    trace_processor_bin: Path,
    sql_template_dir: Path,
    sql_output_dir: Path,
    case_id: str,
    package_name: str,
    layer_glob: str,
    trace_seconds: float,
    consumer_mode: str,
    trailing_grace_ns: int = 0,
) -> Dict[str, Any]:
    template_files = {
        "common": sql_template_dir / "00_common.sql",
        "consumer_app": sql_template_dir / "10_consumer_app.sql",
        "consumer_sf": sql_template_dir / "11_consumer_sf.sql",
        "metrics": sql_template_dir / "20_metrics.sql",
        "layers": sql_template_dir / "21_layer_breakdown.sql",
        "thread_sched": sql_template_dir / "30_thread_sched.sql",
    }
    for name, file_path in template_files.items():
        if not file_path.exists():
            raise FileNotFoundError(f"SQL template missing ({name}): {file_path}")

    template_texts = {key: path.read_text(encoding="utf-8") for key, path in template_files.items()}
    mapping_base = {
        "PACKAGE_NAME": quote_sql(package_name),
        "LAYER_GLOB": quote_sql(layer_glob),
        "TRACE_SECONDS": str(trace_seconds),
        "TRAILING_GRACE_NS": str(int(max(0, trailing_grace_ns))),
    }

    consumers: List[str]
    if consumer_mode == "both":
        consumers = ["app", "sf"]
    else:
        consumers = [consumer_mode]

    results: Dict[str, Any] = {}
    for consumer in consumers:
        if consumer == "app":
            consumer_text = template_texts["consumer_app"]
            consumer_table = "hpfc_consumer_frames_app"
        else:
            consumer_text = template_texts["consumer_sf"]
            consumer_table = "hpfc_consumer_frames_sf"

        metrics_sql = render_template(
            template_texts["common"] + "\n" + consumer_text + "\n" + template_texts["metrics"],
            {
                **mapping_base,
                "CONSUMER_TABLE": consumer_table,
            },
        )
        layers_sql = render_template(
            template_texts["common"] + "\n" + consumer_text + "\n" + template_texts["layers"],
            {
                **mapping_base,
                "CONSUMER_TABLE": consumer_table,
            },
        )

        metrics_sql_path = sql_output_dir / f"{case_id}-{consumer}-metrics.sql"
        layers_sql_path = sql_output_dir / f"{case_id}-{consumer}-layers.sql"
        metrics_payload = run_trace_processor_query(
            trace_processor_bin=trace_processor_bin,
            trace_path=trace_path,
            rendered_sql=metrics_sql,
            rendered_sql_path=metrics_sql_path,
        )
        layers_payload = run_trace_processor_query(
            trace_processor_bin=trace_processor_bin,
            trace_path=trace_path,
            rendered_sql=layers_sql,
            rendered_sql_path=layers_sql_path,
        )
        if isinstance(layers_payload, list):
            metrics_payload["layer_breakdown"] = layers_payload
        else:
            metrics_payload["layer_breakdown"] = []

        results[consumer] = metrics_payload

    thread_sched_sql = render_template(
        template_texts["common"] + "\n" + template_texts["thread_sched"],
        mapping_base,
    )
    thread_sched_sql_path = sql_output_dir / f"{case_id}-thread-sched.sql"
    thread_sched_payload = run_trace_processor_query(
        trace_processor_bin=trace_processor_bin,
        trace_path=trace_path,
        rendered_sql=thread_sched_sql,
        rendered_sql_path=thread_sched_sql_path,
    )

    return {
        "window_sec": trace_seconds,
        "results": results,
        "thread_metrics": thread_sched_payload,
    }


def build_arg_parser(repo_root: Path) -> argparse.ArgumentParser:
    tools_dir = repo_root / "Tools"
    parser = argparse.ArgumentParser(
        description="Release scrolling benchmark: Perfetto + gfxinfo + consumer-perspective metrics"
    )
    parser.add_argument("--serial", default=None, help="ADB device serial")
    parser.add_argument("--apk-dir", type=Path, default=repo_root / "apk-released")
    parser.add_argument("--trace-dir", type=Path, default=tools_dir / "trace-release")
    parser.add_argument(
        "--result-dir",
        type=Path,
        default=tools_dir / "results" / "perfetto-scroll",
    )
    parser.add_argument(
        "--perfetto-bin",
        type=Path,
        default=tools_dir / "record_android_trace",
    )
    parser.add_argument(
        "--perfetto-config",
        type=Path,
        default=tools_dir / "perfetto.config",
    )
    parser.add_argument(
        "--trace-processor-bin",
        type=Path,
        default=tools_dir / "trace_processor_shell",
    )
    parser.add_argument(
        "--sql-template-dir",
        type=Path,
        default=tools_dir / "sql" / "perfetto_scroll",
    )
    parser.add_argument(
        "--loads",
        nargs="+",
        default=DEFAULT_LOADS,
        choices=LOAD_CHOICES,
        help="Loads to run for each module",
    )
    parser.add_argument(
        "--only-apk",
        nargs="+",
        default=[],
        help="Filter apk/module by name token (e.g. scrolling-aosp-performance)",
    )
    parser.add_argument("--trace-seconds", type=int, default=20)
    parser.add_argument("--warmup-up-swipes", type=int, default=1)
    parser.add_argument(
        "--block-size",
        type=int,
        default=5,
        help="Up swipes per formal cycle",
    )
    parser.add_argument("--down-block-size", type=int, default=4)
    parser.add_argument("--swipe-duration-ms", type=int, default=130)
    parser.add_argument("--swipe-interval-ms", type=int, default=1200)
    parser.add_argument("--trace-start-wait-ms", type=int, default=0)
    parser.add_argument("--post-scroll-wait-ms", type=int, default=0)
    parser.add_argument("--swipe-x-ratio", type=float, default=0.50)
    parser.add_argument("--swipe-start-y-ratio", type=float, default=0.82)
    parser.add_argument("--swipe-end-y-ratio", type=float, default=0.28)
    parser.add_argument("--wait-after-launch-ms", type=int, default=3500)
    parser.add_argument("--post-warmup-wait-ms", type=int, default=3000)
    parser.add_argument("--wait-after-entry-ms", type=int, default=800)
    parser.add_argument("--trace-stop-timeout-sec", type=int, default=45)
    parser.add_argument("--case-interval-ms", type=int, default=1000)
    parser.add_argument("--layer-glob", default="*")
    parser.add_argument("--consumer-mode", choices=["sf", "app", "both"], default="both")
    parser.add_argument(
        "--gfx-target-fps",
        type=int,
        default=None,
        help="Frame budget target in FPS. Defaults to device peak refresh "
             "rate detected via `dumpsys display`; falls back to 120.",
    )
    parser.add_argument("--skip-install", action="store_true")
    parser.add_argument("--strict-loads", action="store_true")
    parser.add_argument(
        "--include-manual-permission-modules",
        action="store_true",
        help="Include modules that may require manual runtime permission confirmation",
    )
    parser.add_argument(
        "--enable-gfxinfo",
        action="store_true",
        default=True,
        help="Collect and parse dumpsys gfxinfo framestats",
    )
    parser.add_argument(
        "--disable-gfxinfo",
        dest="enable_gfxinfo",
        action="store_false",
        help="Disable dumpsys gfxinfo collection",
    )
    parser.add_argument("--motion-check", action="store_true", default=False)
    parser.add_argument("--allow-main-entry", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument(
        "--prelaunch-flutter-variant",
        nargs="+",
        default=[],
        choices=sorted(FLUTTER_PRELAUNCH_VARIANTS.keys()) + ["all"],
        help="Flutter engine variants to warm up before the formal sweep "
             "(am start with load=build_heavy). 'all' expands to every known variant.",
    )
    parser.add_argument(
        "--postprocess-summary",
        type=Path,
        default=None,
        help="Post-process an existing summary JSON and emit final report under run root",
    )
    return parser


def run_case(
    adb: ADB,
    meta: ModuleMeta,
    load: str,
    args: argparse.Namespace,
    device_tag: str,
    metrics_dir: Path,
    gfxinfo_dir: Path,
    sql_rendered_dir: Path,
) -> CaptureOutcome:
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    case_id = f"{device_tag}-{meta.module_name}-{load}-{timestamp}"
    trace_path = args.trace_dir / f"{case_id}.ptrace"
    metrics_path = metrics_dir / f"{case_id}.metrics.json"
    gfxinfo_raw_path = gfxinfo_dir / f"{case_id}.framestats.txt"
    recorder: Optional[TraceRecorder] = None

    artifacts: Dict[str, Any] = {
        "trace_file": str(trace_path),
        "gfxinfo_raw_file": str(gfxinfo_raw_path),
        "sql_dir": str(sql_rendered_dir),
    }

    try:
        # Defensive mkdirs: external cleanups or interrupted previous runs can remove these folders.
        metrics_dir.mkdir(parents=True, exist_ok=True)
        gfxinfo_dir.mkdir(parents=True, exist_ok=True)
        sql_rendered_dir.mkdir(parents=True, exist_ok=True)

        log("INFO", f"[1/10] Pre-clean: force-stop {meta.package_name}")
        adb.force_stop(meta.package_name)

        log("INFO", f"[2/10] Enter target page: {meta.module_name}/{load}")
        entered, entry_method = enter_load_page(
            adb=adb,
            meta=meta,
            load=load,
            wait_after_entry_sec=args.wait_after_entry_ms / 1000.0,
            allow_main_entry=args.allow_main_entry,
        )
        if not entered:
            return CaptureOutcome(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason="direct_entry_failed",
                trace_path=None,
                metrics_path=None,
            )

        if args.wait_after_launch_ms > 0:
            log("INFO", f"[3/10] Stabilize page: wait {args.wait_after_launch_ms}ms")
            time.sleep(args.wait_after_launch_ms / 1000.0)

        screen_size = adb.get_screen_size()
        route, route_desc = resolve_swipe_route(
            adb=adb,
            meta=meta,
            screen_size=screen_size,
            swipe_x_ratio=args.swipe_x_ratio,
            swipe_start_y_ratio=args.swipe_start_y_ratio,
            swipe_end_y_ratio=args.swipe_end_y_ratio,
        )
        route = force_centered_swipe_route(route, screen_size)
        route_desc = f"{route_desc}|centered_forceful"
        x, y_start, y_end = route
        log(
            "INFO",
            f"[4/10] Swipe route ({route_desc}): x={x}, y:{y_start}->{y_end}, "
            f"distance={y_start - y_end}px",
        )

        log("INFO", f"[5/10] Warmup up swipe x{args.warmup_up_swipes}")
        run_warmup_up_swipes(
            adb=adb,
            route=route,
            warmup_up_swipes=args.warmup_up_swipes,
            swipe_duration_ms=args.swipe_duration_ms,
            swipe_interval_sec=args.swipe_interval_ms / 1000.0,
        )

        if args.post_warmup_wait_ms > 0:
            log("INFO", f"Warmup settle: wait {args.post_warmup_wait_ms}ms before formal run")
            time.sleep(args.post_warmup_wait_ms / 1000.0)

        log("INFO", f"[6/10] Start trace: {trace_path}")
        recorder = TraceRecorder(
            perfetto_bin=args.perfetto_bin,
            perfetto_config=args.perfetto_config,
            serial=adb.serial,
            output_path=trace_path,
            dry_run=args.dry_run,
        )
        ok_start, start_err = recorder.start()
        if not ok_start:
            return CaptureOutcome(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason=f"trace_start_failed: {start_err}",
                trace_path=None,
                metrics_path=None,
            )

        if args.trace_start_wait_ms > 0:
            time.sleep(args.trace_start_wait_ms / 1000.0)

        # Gfxinfo reset happens AFTER trace_start_wait and IMMEDIATELY before
        # the formal scroll loop; collection happens IMMEDIATELY after the
        # loop, before post_scroll_wait and trace stop. This tightens the
        # gfxinfo window to match the perfetto formal-scroll window (P1 fix).
        gfx_output = ""
        if args.enable_gfxinfo:
            log("INFO", "[7/10] Reset gfxinfo counters (aligned to formal scroll)")
            gfx_ok, gfx_msg = reset_gfxinfo(adb, meta.package_name)
            if not gfx_ok:
                log("WARN", f"gfxinfo reset failed: {gfx_msg}")

        log(
            "INFO",
            f"[8/10] Formal scroll loop (up={args.block_size}, down={args.down_block_size}) "
            f"for {args.trace_seconds}s",
        )
        loop_stats = run_formal_scroll_loop(
            adb=adb,
            route=route,
            trace_seconds=args.trace_seconds,
            up_block_size=args.block_size,
            down_block_size=args.down_block_size,
            swipe_duration_ms=args.swipe_duration_ms,
            swipe_interval_sec=args.swipe_interval_ms / 1000.0,
            motion_check_enabled=args.motion_check,
        )

        if args.enable_gfxinfo:
            gfx_ok, gfx_output = collect_gfxinfo(adb, meta.package_name)
            gfxinfo_raw_path.parent.mkdir(parents=True, exist_ok=True)
            gfxinfo_raw_path.write_text(gfx_output, encoding="utf-8")
            if not gfx_ok:
                log("WARN", "gfxinfo collection command returned non-zero status")

        if args.post_scroll_wait_ms > 0:
            time.sleep(args.post_scroll_wait_ms / 1000.0)

        log("INFO", "[9/10] Stop trace")
        ok_stop, stop_out = recorder.stop(args.trace_stop_timeout_sec)

        trace_ok = args.dry_run or (trace_path.exists() and trace_path.stat().st_size > 0)
        if not ok_stop:
            log("WARN", f"trace stop output: {stop_out}")
        if not trace_ok:
            return CaptureOutcome(
                module_name=meta.module_name,
                package_name=meta.package_name,
                load=load,
                status="failed",
                reason=f"trace_stop_or_file_failed: {stop_out}",
                trace_path=trace_path if trace_path.exists() else None,
                metrics_path=None,
            )

        log("INFO", "[10/10] Parse perfetto trace")

        metrics_window_sec = float(loop_stats.get("actual_window_sec", args.trace_seconds))
        if metrics_window_sec <= 0:
            metrics_window_sec = float(args.trace_seconds)
        metrics_window_sec = min(metrics_window_sec, float(args.trace_seconds))

        # Grace = everything that happens between the last formal-scroll frame
        # and the trace-stop write (post_scroll_wait + a buffer for trace-stop
        # latency). Subtracted from MAX(ts) so the perfetto window anchors on
        # the last formal-scroll frame instead of stray post-scroll frames.
        trailing_grace_ms = max(0, args.post_scroll_wait_ms) + 500
        trailing_grace_ns = trailing_grace_ms * 1_000_000

        perfetto_metrics: Dict[str, Any] = {
            "window_sec": metrics_window_sec,
            "results": {},
        }
        if not args.dry_run:
            perfetto_metrics = parse_perfetto_metrics(
                trace_path=trace_path,
                trace_processor_bin=args.trace_processor_bin,
                sql_template_dir=args.sql_template_dir,
                sql_output_dir=sql_rendered_dir,
                case_id=case_id,
                package_name=meta.package_name,
                layer_glob=args.layer_glob,
                trace_seconds=metrics_window_sec,
                consumer_mode=args.consumer_mode,
                trailing_grace_ns=trailing_grace_ns,
            )

        gfx_metrics = {
            "enabled": args.enable_gfxinfo,
            "parsed": parse_gfxinfo_output(gfx_output, target_fps=args.gfx_target_fps)
            if args.enable_gfxinfo and gfx_output
            else {},
        }

        payload: Dict[str, Any] = {
            "run_id": case_id,
            "meta": {
                "timestamp": timestamp,
                "device_model": adb.get_model(),
                "serial": adb.serial or adb.get_serial(),
                "package": meta.package_name,
                "module": meta.module_name,
                "load": load,
                "entry_method": entry_method,
            },
            "config": {
                "trace_seconds": args.trace_seconds,
                "metrics_window_sec": metrics_window_sec,
                "warmup_up_swipes": args.warmup_up_swipes,
                "formal_swipe_mode": "up_down_cycle",
                "up_block_size": args.block_size,
                "down_block_size": args.down_block_size,
                "consumer_mode": args.consumer_mode,
                "layer_glob": args.layer_glob,
                "swipe_duration_ms": args.swipe_duration_ms,
                "swipe_interval_ms": args.swipe_interval_ms,
                "post_warmup_wait_ms": args.post_warmup_wait_ms,
                "motion_check": args.motion_check,
            },
            "artifacts": artifacts,
            "scroll_stats": loop_stats,
            "perfetto": perfetto_metrics,
            "gfxinfo": gfx_metrics,
        }
        metrics_path.parent.mkdir(parents=True, exist_ok=True)
        metrics_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")

        return CaptureOutcome(
            module_name=meta.module_name,
            package_name=meta.package_name,
            load=load,
            status="success",
            reason="ok",
            trace_path=trace_path,
            metrics_path=metrics_path,
        )
    except TraceProcessorError as exc:
        return CaptureOutcome(
            module_name=meta.module_name,
            package_name=meta.package_name,
            load=load,
            status="failed",
            reason=f"{exc.reason_code}: {exc}",
            trace_path=trace_path if trace_path.exists() else None,
            metrics_path=metrics_path if metrics_path.exists() else None,
        )
    except Exception as exc:  # noqa: BLE001
        return CaptureOutcome(
            module_name=meta.module_name,
            package_name=meta.package_name,
            load=load,
            status="failed",
            reason=f"exception: {exc}",
            trace_path=trace_path if trace_path.exists() else None,
            metrics_path=metrics_path if metrics_path.exists() else None,
        )
    finally:
        if recorder and recorder.proc and recorder.proc.poll() is None:
            recorder.stop(args.trace_stop_timeout_sec)
        adb.force_stop(meta.package_name)
        adb.press_home()


def main() -> int:
    script_path = Path(__file__).resolve()
    repo_root = script_path.parent.parent
    parser = build_arg_parser(repo_root)
    args = parser.parse_args()

    if args.postprocess_summary is not None:
        summary_file = args.postprocess_summary
        if not summary_file.exists():
            log("ERROR", f"Summary file not found: {summary_file}")
            return 1
        try:
            updated_summary, comparison_csv, final_report = postprocess_existing_summary(summary_file)
        except (OSError, json.JSONDecodeError, ValueError) as exc:
            log("ERROR", f"Postprocess failed: {exc}")
            return 1
        log("INFO", f"Postprocessed summary: {updated_summary}")
        if comparison_csv:
            log("INFO", f"Comparison CSV: {comparison_csv}")
        log("INFO", f"Final report: {final_report}")
        return 0

    for required in (
        args.perfetto_bin,
        args.perfetto_config,
        args.trace_processor_bin,
        args.sql_template_dir,
    ):
        if not required.exists():
            log("ERROR", f"Missing required path: {required}")
            return 1

    if not args.apk_dir.exists():
        log("ERROR", f"APK dir not found: {args.apk_dir}")
        return 1

    if args.trace_seconds <= 0:
        log("ERROR", "--trace-seconds must be > 0")
        return 1
    if args.block_size <= 0:
        log("ERROR", "--block-size must be > 0")
        return 1
    if args.down_block_size <= 0:
        log("ERROR", "--down-block-size must be > 0")
        return 1
    if args.swipe_interval_ms < 0:
        log("ERROR", "--swipe-interval-ms must be >= 0")
        return 1
    if args.case_interval_ms < 0:
        log("ERROR", "--case-interval-ms must be >= 0")
        return 1
    if args.post_warmup_wait_ms < 0:
        log("ERROR", "--post-warmup-wait-ms must be >= 0")
        return 1

    args.trace_dir.mkdir(parents=True, exist_ok=True)
    args.result_dir.mkdir(parents=True, exist_ok=True)
    metrics_dir = args.result_dir / "metrics"
    gfxinfo_dir = args.result_dir / "gfxinfo_raw"
    sql_rendered_dir = args.result_dir / "sql_rendered"
    metrics_dir.mkdir(parents=True, exist_ok=True)
    gfxinfo_dir.mkdir(parents=True, exist_ok=True)
    sql_rendered_dir.mkdir(parents=True, exist_ok=True)

    adb = ADB(serial=args.serial, dry_run=args.dry_run)
    if not args.dry_run and not adb.check_device():
        log("ERROR", "No connected adb device")
        return 1

    serial = adb.serial or adb.get_serial()
    model = adb.get_model()
    device_tag = sanitize_for_filename(f"{model}-{serial}")

    log("INFO", f"Device: model={model}, serial={serial}")
    log("INFO", f"Release APK dir: {args.apk_dir}")
    log("INFO", f"Trace dir: {args.trace_dir}")
    log("INFO", f"Result dir: {args.result_dir}")
    log("INFO", f"Loads: {', '.join(args.loads)}")

    if args.gfx_target_fps is None:
        detected_fps, source = detect_device_refresh_rate(adb, fallback=120)
        args.gfx_target_fps = detected_fps
        log("INFO", f"gfx-target-fps (auto): {detected_fps} from {source}")
    else:
        log("INFO", f"gfx-target-fps (user): {args.gfx_target_fps}")

    if args.prelaunch_flutter_variant and not args.dry_run:
        prelaunch_flutter_variants(adb, args.prelaunch_flutter_variant)

    modules = discover_release_modules(
        repo_root=repo_root,
        apk_dir=args.apk_dir,
        only_tokens=args.only_apk,
        include_manual_permission_modules=args.include_manual_permission_modules,
    )
    if not modules:
        log("ERROR", "No matching scrolling release APKs found")
        return 1

    log("INFO", f"Discovered {len(modules)} scrolling release APK(s)")
    for meta in modules:
        if meta.supported_loads:
            log(
                "INFO",
                f"  - {meta.module_name} load_plan={','.join(meta.supported_loads)}",
            )
        log(
            "INFO",
            f"  - {meta.module_name} ({meta.package_name}) "
            f"launcher={meta.launcher_activity} "
            f"activity_type={'yes' if meta.supports_activity_type else 'no'}",
        )

    install_ok_map: Dict[str, bool] = {}
    if not args.skip_install:
        log("INFO", "Installing release APKs ...")
        for meta in modules:
            ok = install_release_apk_with_recovery(adb, meta)
            install_ok_map[meta.module_name] = ok
            if ok:
                log("SUCCESS", f"Installed: {meta.apk_path.name}")
            else:
                log("ERROR", f"Install failed: {meta.apk_path.name}")
    else:
        for meta in modules:
            install_ok_map[meta.module_name] = True

    results: List[CaptureOutcome] = []
    module_text_cache: Dict[str, str] = {}
    for meta in modules:
        if not install_ok_map.get(meta.module_name, False):
            module_loads = list(meta.supported_loads) if meta.supported_loads else list(args.loads)
            for load in module_loads:
                results.append(
                    CaptureOutcome(
                        module_name=meta.module_name,
                        package_name=meta.package_name,
                        load=load,
                        status="failed",
                        reason="install_failed",
                        trace_path=None,
                        metrics_path=None,
                    )
                )
            continue

        module_loads = list(meta.supported_loads) if meta.supported_loads else list(args.loads)
        for load in module_loads:
            if not module_supports_load(meta, load, module_text_cache):
                results.append(
                    CaptureOutcome(
                        module_name=meta.module_name,
                        package_name=meta.package_name,
                        load=load,
                        status="skipped",
                        reason="module_does_not_support_load",
                        trace_path=None,
                        metrics_path=None,
                    )
                )
                log("WARN", f"Skipped: {meta.module_name}/{load} (module_does_not_support_load)")
                if args.case_interval_ms > 0:
                    time.sleep(args.case_interval_ms / 1000.0)
                continue

            log("INFO", f"Capture: {meta.module_name}/{load}")
            outcome = run_case(
                adb=adb,
                meta=meta,
                load=load,
                args=args,
                device_tag=device_tag,
                metrics_dir=metrics_dir,
                gfxinfo_dir=gfxinfo_dir,
                sql_rendered_dir=sql_rendered_dir,
            )
            results.append(outcome)

            if outcome.status == "success":
                log(
                    "SUCCESS",
                    f"Saved: trace={outcome.trace_path}, metrics={outcome.metrics_path}",
                )
            else:
                log("ERROR", f"Failed: {meta.module_name}/{load} ({outcome.reason})")

            if args.case_interval_ms > 0:
                log("INFO", f"Case gap: sleep {args.case_interval_ms}ms")
                time.sleep(args.case_interval_ms / 1000.0)

    summary = {
        "timestamp": datetime.now().strftime("%Y%m%d-%H%M%S"),
        "device": {
            "model": model,
            "serial": serial,
        },
        "config": {
            "loads": args.loads,
            "trace_seconds": args.trace_seconds,
            "warmup_up_swipes": args.warmup_up_swipes,
            "formal_swipe_mode": "up_down_cycle",
            "up_block_size": args.block_size,
            "down_block_size": args.down_block_size,
            "consumer_mode": args.consumer_mode,
            "layer_glob": args.layer_glob,
        },
        "results": [
            {
                "module_name": r.module_name,
                "package_name": r.package_name,
                "load": r.load,
                "status": r.status,
                "reason": r.reason,
                "trace_path": str(r.trace_path) if r.trace_path else "",
                "metrics_path": str(r.metrics_path) if r.metrics_path else "",
            }
            for r in results
        ],
        "comparisons": {},
    }
    summary = enrich_summary_with_postprocess(summary, results)
    thread_summary_rows = summary["comparisons"].get("thread_metrics", [])
    gfxinfo_summary_rows = summary["comparisons"].get("gfxinfo_metrics", [])

    summary_file = args.result_dir / f"summary-{summary['timestamp']}.json"
    summary_file.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    csv_file: Optional[Path] = None
    try:
        csv_file = export_default_comparison_csv(summary, summary_file)
    except OSError as exc:
        log("WARN", f"Failed to write comparison CSV: {exc}")

    final_report_file = write_final_run_report(summary, summary_file, csv_file)

    success = sum(1 for r in results if r.status == "success")
    skipped = sum(1 for r in results if r.status == "skipped")
    failed = sum(1 for r in results if r.status == "failed")
    total = len(results)

    print()
    log("INFO", "=" * 72)
    log("INFO", "Summary")
    log("INFO", "=" * 72)
    log("INFO", f"Total: {total}, Success: {success}, Skipped: {skipped}, Failed: {failed}")
    log("INFO", f"Thread comparison rows: {len(thread_summary_rows)}")
    log("INFO", f"Gfxinfo comparison rows: {len(gfxinfo_summary_rows)}")
    log("INFO", f"Summary JSON: {summary_file}")
    if csv_file:
        log("INFO", f"Comparison CSV: {csv_file}")
    log("INFO", f"Final report: {final_report_file}")
    for r in results:
        trace_str = str(r.trace_path) if r.trace_path else "-"
        metrics_str = str(r.metrics_path) if r.metrics_path else "-"
        log(
            "INFO",
            f"{r.status.upper():7} {r.module_name:34} {r.load:10} trace={trace_str} metrics={metrics_str}",
        )

    if failed > 0:
        return 1
    if args.strict_loads and skipped > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
