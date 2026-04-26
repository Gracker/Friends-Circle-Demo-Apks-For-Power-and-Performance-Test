from __future__ import annotations

import argparse
import json
import shlex
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

from .paths import REPO_ROOT, SCRIPT_DIR
from .utils import parse_float, run_host_command, log

def newest_file(path: Path, pattern: str) -> Optional[Path]:
    files = sorted(path.glob(pattern), key=lambda p: p.stat().st_mtime if p.exists() else 0)
    return files[-1] if files else None


def detect_root_shell(adb: "Any") -> Dict[str, Any]:
    """Best-effort root detection. Returns {available, method}.

    Distinguishes:
      - 'su 0' : adbd runs as shell, but `su 0` succeeds (Magisk / userdebug+su)
      - 'shell-uid-0' : adbd already runs as root (eng / `adb root` on userdebug)
      - None : not rooted
    """
    if getattr(adb, "dry_run", False):
        return {"available": False, "method": None}
    # Try `su 0 id` first; if it fails (no su, or su denies), it returns nonzero.
    su_out = adb.shell_out("su 0 id 2>/dev/null", timeout=10).strip()
    if "uid=0" in su_out:
        return {"available": True, "method": "su 0"}
    # Otherwise check whether the plain shell already runs as root.
    plain_out = adb.shell_out("id", timeout=10).strip()
    if "uid=0" in plain_out:
        return {"available": True, "method": "shell-uid-0"}
    return {"available": False, "method": None}


def harden_cold_launch(adb: "Any", package: Optional[str] = None) -> Dict[str, Any]:
    """If root is available, drop_caches (and optionally reset compile profile).

    Note: compile-reset only runs when `package` is provided. The launch driver
    currently doesn't pass per-package context, so most invocations just do
    drop_caches + sync.
    """
    root = detect_root_shell(adb)
    if not root["available"]:
        return {"hardened": False, "reason": "no root", "root": root}
    actions: List[str] = []
    failures: List[str] = []
    needs_su = root["method"] == "su 0"
    sync_cmd = "su 0 sync" if needs_su else "sync"
    drop_cmd = (
        "su 0 sh -c 'echo 3 > /proc/sys/vm/drop_caches'"
        if needs_su
        else "sh -c 'echo 3 > /proc/sys/vm/drop_caches'"
    )
    sync_proc = adb.shell(sync_cmd, timeout=20)
    if getattr(sync_proc, "returncode", 1) == 0:
        actions.append("sync")
    else:
        failures.append("sync")
    drop_proc = adb.shell(drop_cmd, timeout=20)
    if getattr(drop_proc, "returncode", 1) == 0:
        actions.append("drop_caches")
    else:
        failures.append("drop_caches")
    if package:
        compile_proc = adb.shell(
            f"cmd package compile --reset {shlex.quote(package)}", timeout=30
        )
        if getattr(compile_proc, "returncode", 1) == 0:
            actions.append(f"compile-reset:{package}")
        else:
            failures.append(f"compile-reset:{package}")
    return {
        "hardened": bool(actions) and not failures,
        "actions": actions,
        "failures": failures,
        "root": root,
    }


def run_child_script(cmd: Sequence[str], log_file: Path, timeout: Optional[int]) -> Dict[str, Any]:
    log("INFO", "Running child benchmark: " + " ".join(shlex.quote(x) for x in cmd))
    proc = run_host_command(cmd, timeout=timeout, log_file=log_file, cwd=REPO_ROOT)
    return {
        "status": "success" if proc.returncode == 0 else "failed",
        "returncode": proc.returncode,
        "duration_sec": round(getattr(proc, "elapsed_sec", 0.0), 3),
        "log_file": str(log_file),
    }


def run_launch_benchmark(serial: str, args: argparse.Namespace, device_dir: Path,
                          adb: Optional[Any] = None) -> Dict[str, Any]:
    if args.skip_launch:
        return {"status": "skipped", "reason": "--skip-launch"}
    script = SCRIPT_DIR / "auto_record_perfetto_launch.py"
    if not script.exists():
        return {"status": "skipped", "reason": f"script not found: {script}"}
    cold_hardening = {"hardened": False, "reason": "adb instance not provided"}
    if adb is not None and getattr(args, "harden_cold_launch", True):
        cold_hardening = harden_cold_launch(adb, package=None)
    trace_dir = device_dir / "launch" / "traces"
    results_dir = device_dir / "launch" / "results"
    cmd = [
        sys.executable,
        str(script),
        "--serial",
        serial,
        "--trace-dir",
        str(trace_dir),
        "--results-dir",
        str(results_dir),
        "--iterations",
        str(args.launch_iterations),
        "--case-interval-ms",
        str(args.launch_case_interval_ms),
    ]
    if args.launch_apps:
        cmd += ["--apps"] + args.launch_apps
    if args.launch_flavors:
        cmd += ["--flavors"] + args.launch_flavors
    if args.skip_launch_install:
        cmd.append("--skip-install")
    if args.dry_run:
        return {"status": "dry_run", "reason": "dry-run does not run child launch benchmark", "command": cmd + ["--dry-run"]}
    log_file = device_dir / "logs" / "launch.log"
    result = run_child_script(cmd, log_file=log_file, timeout=args.launch_timeout_sec)
    report_json = newest_file(results_dir, "launch_report_*.json")
    report_md = newest_file(results_dir, "launch_report_*.md")
    result["json"] = str(report_json) if report_json else None
    result["markdown"] = str(report_md) if report_md else None
    if report_json and report_json.exists():
        payload = json.loads(report_json.read_text(encoding="utf-8"))
        result["summary"] = summarize_launch_payload(payload)
    result["cold_hardening"] = cold_hardening
    # `am force-stop` already happens before each iteration in the child script,
    # which gives a real-world cold-launch baseline (the user-perceived "I tap the
    # icon after the app has been killed"). drop_caches/compile-reset are only
    # needed for "first launch ever after reboot" semantics — not the common case.
    # So treat success as active.
    has_useful_data = bool(result.get("summary", {}).get("rows"))
    if result.get("status") == "failed" and has_useful_data:
        # Same partial-success policy as scroll: mark success so per-app rows count
        # toward scoring; the failed app simply has no metric_row to contribute.
        result["status"] = "success"
        result["partial_failure_reason"] = (
            "child script exited non-zero (one or more launch apps failed to start), "
            "but per-app rows for the rest are intact. See summary.rows."
        )
    return result


def summarize_launch_payload(payload: Dict[str, Any]) -> Dict[str, Any]:
    summaries = payload.get("summaries") or payload.get("summary") or []
    rows: List[Dict[str, Any]] = []
    best_avg: Optional[float] = None
    for item in summaries:
        metric = item.get("logcat_duration_ms") or item.get("duration_ms") or {}
        avg = metric.get("avg") if isinstance(metric, dict) else None
        if avg is None:
            avg = item.get("avg_ms") or item.get("avg")
        avg_f = parse_float(avg)
        row = {
            "app": item.get("app_name") or item.get("app") or item.get("module"),
            "flavor": item.get("flavor"),
            "avg_ms": avg_f,
            "successful": item.get("successful"),
            "total": item.get("total_iterations"),
            "grade": item.get("grade"),
        }
        rows.append(row)
        if avg_f is not None:
            best_avg = avg_f if best_avg is None else min(best_avg, avg_f)
    return {
        "case_count": len(rows),
        "best_avg_ms": best_avg,
        "rows": rows,
    }


def run_scroll_benchmark(serial: str, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if args.skip_scroll:
        return {"status": "skipped", "reason": "--skip-scroll"}
    script = SCRIPT_DIR / "perfetto_release_scroll_benchmark.py"
    if not script.exists():
        return {"status": "skipped", "reason": f"script not found: {script}"}
    trace_dir = device_dir / "scroll" / "traces"
    result_dir = device_dir / "scroll" / "results"
    cmd = [
        sys.executable,
        str(script),
        "--serial",
        serial,
        "--trace-dir",
        str(trace_dir),
        "--result-dir",
        str(result_dir),
        "--trace-seconds",
        str(args.scroll_trace_seconds),
        "--case-interval-ms",
        str(args.scroll_case_interval_ms),
        "--loads",
    ] + args.scroll_loads
    if args.scroll_only_apk:
        cmd += ["--only-apk"] + args.scroll_only_apk
    if args.skip_scroll_install:
        cmd.append("--skip-install")
    if args.scroll_disable_gfxinfo:
        cmd.append("--disable-gfxinfo")
    if args.scroll_allow_main_entry:
        cmd.append("--allow-main-entry")
    if args.dry_run:
        return {"status": "dry_run", "reason": "dry-run does not run child scroll benchmark", "command": cmd + ["--dry-run"]}
    log_file = device_dir / "logs" / "scroll.log"
    result = run_child_script(cmd, log_file=log_file, timeout=args.scroll_timeout_sec)
    summary_json = newest_file(result_dir, "summary-*.json")
    final_json = newest_file(result_dir.parent, "final-summary-*.json") or newest_file(result_dir, "final-summary-*.json")
    result["summary_json"] = str(summary_json) if summary_json else None
    result["final_json"] = str(final_json) if final_json else None
    if summary_json and summary_json.exists():
        payload = json.loads(summary_json.read_text(encoding="utf-8"))
        result["summary"] = summarize_scroll_payload(payload)
    # The child script returns non-zero whenever ANY of the N scroll modules failed,
    # but the per-case data for the modules that did run is still valid. Promote to
    # success so the metrics count toward scoring; record what failed for transparency.
    has_useful_data = bool(((result.get("summary") or {}).get("valid_cases")))
    if result.get("status") == "failed" and has_useful_data:
        result["status"] = "success"
        result["partial_failure_reason"] = (
            "child script exited non-zero (one or more scroll modules failed to capture "
            "or parse), but per-module data for the rest is intact. See summary.valid_cases "
            "and summary.stuck_cases for the breakdown."
        )
    return result


def summarize_scroll_payload(payload: Dict[str, Any]) -> Dict[str, Any]:
    """Aggregate per-case gfxinfo rows.

    A "case" is one (module, load) cell. Cases with frame_count=0 (or fps=0
    and no janky frames) usually mean the swipe gesture wasn't consumed —
    e.g., WebView with heavy CPU load blocks the touch handler. Such cases
    are excluded from the averages and surfaced separately as `stuck_cases`.
    """
    rows = payload.get("comparisons", {}).get("gfxinfo_metrics", [])
    if not rows:
        rows = payload.get("results", [])
    fps_values: List[float] = []
    p95_values: List[float] = []
    p99_values: List[float] = []
    jank_values: List[float] = []
    janky_frame_count = 0
    total_frame_count = 0
    valid_cases: List[Dict[str, Any]] = []
    stuck_cases: List[Dict[str, Any]] = []

    def _first_float(row: Dict[str, Any], keys: Sequence[str]) -> Optional[float]:
        for key in keys:
            value = parse_float(row.get(key))
            if value is not None:
                return value
        return None

    for row in rows:
        fc = _first_float(row, ("frame_count", "total_frames"))
        fps = _first_float(row, ("avg_fps", "fps", "actual_fps"))
        case = {
            "module": row.get("module_name") or row.get("module"),
            "load": row.get("load"),
            "fps": fps,
            "frame_count": int(fc) if fc is not None else None,
            "p95_ms": _first_float(row, ("p95_ms", "p95_frame_ms", "frame_p95_ms")),
            "p99_ms": _first_float(row, ("p99_ms", "p99_frame_ms")),
            "janky_pct": _first_float(row, ("janky_percent", "jank_rate_pct", "jank_percent", "missed_vsync_pct")),
        }
        if (fc is None or fc == 0) or (fps is None or fps == 0):
            case["stuck_reason"] = ("0 frames captured — swipe likely not consumed "
                                     "(heavy load blocked touch handler / app crashed / SW renderer missing)")
            stuck_cases.append(case)
            continue
        valid_cases.append(case)
        if fps is not None: fps_values.append(fps)
        if case["p95_ms"] is not None: p95_values.append(case["p95_ms"])
        if case["p99_ms"] is not None: p99_values.append(case["p99_ms"])
        if case["janky_pct"] is not None: jank_values.append(case["janky_pct"])
        jf = parse_float(row.get("janky_frames"))
        if jf is not None: janky_frame_count += int(jf)
        if fc is not None: total_frame_count += int(fc)
    return {
        "case_count": len(rows),
        "valid_case_count": len(valid_cases),
        "stuck_case_count": len(stuck_cases),
        "avg_fps": round(sum(fps_values) / len(fps_values), 3) if fps_values else None,
        "p95_frame_ms": round(sum(p95_values) / len(p95_values), 3) if p95_values else None,
        "p99_frame_ms": round(sum(p99_values) / len(p99_values), 3) if p99_values else None,
        "avg_jank_pct": round(sum(jank_values) / len(jank_values), 3) if jank_values else None,
        "total_frame_count": total_frame_count or None,
        "janky_frame_count": janky_frame_count or None,
        "valid_cases": valid_cases,
        "stuck_cases": stuck_cases,
    }
