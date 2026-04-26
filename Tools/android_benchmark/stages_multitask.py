"""Multitask / app-eviction probe.

Method:
  1. Build a sequence of installed apps (system staples + our scrolling APKs).
  2. force-stop the FIRST app and time its cold launch (am start -W).
  3. Sequentially launch each subsequent app via am start -W (no force-stop).
  4. Re-launch the first app via am start -W (no force-stop) and measure;
     compare to the cold baseline.
       - revisit ≈ cold time → app was evicted from memory (LMK / cache reuse)
       - revisit << cold time → app survived in memory (warm restart)

Outputs:
  - sequence: ordered list of (pkg, launch_ms, status) for the full chain
  - first_cold_ms / first_revisit_ms / revisit_to_cold_ratio
  - first_evicted (bool): heuristically True when revisit > 0.6 * cold

This stage is RAM-capacity-sensitive: bigger RAM holds more apps in memory
before the first one gets evicted. Same SoC / OS kept constant.
"""
from __future__ import annotations

import argparse
import re
import shlex
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

from .adb import ADB
from .utils import log, parse_float, parse_int, write_csv


# Default candidate sequence — common on most Android builds. We filter to
# whatever's actually installed on the device.
DEFAULT_CANDIDATES: Tuple[str, ...] = (
    # System staples — many won't exist on every device but we filter.
    "com.android.settings", "com.android.deskclock", "com.android.calculator2",
    "com.android.pri.calculator", "com.google.android.calendar",
    "com.google.android.contacts", "com.google.android.dialer",
    "com.android.documentsui", "com.google.android.calculator",
    # Our own scrolling test APKs — predictable size, very likely installed.
    "com.example.wechatfriendforperformance",
    "com.example.wechatfriendforcompose",
    "com.example.wechatfriendforwebview",
    "com.example.wechatfriendforvideo",
    "com.example.wechatfriendforcustomscroller",
    "com.example.wechatfriendforrenderstress",
    # Launch test APKs.
    "com.example.launch.aosp.light",
    "com.example.launch.compose.light",
    "com.example.launch.gl.light",
    "com.example.launch.webview.light",
)


def _autodiscover_example_apps(adb: ADB) -> List[str]:
    """Find any com.example.* packages installed; useful when our test APKs
    are present under unexpected names."""
    out = adb.shell_out(
        "pm list packages | grep -E '^package:com\\.example\\.' | "
        "sed 's|^package:||' | sort -u",
        timeout=15,
    )
    return [line.strip() for line in out.splitlines() if line.strip()]


def _resolve_main_activity(adb: ADB, pkg: str) -> Optional[str]:
    """Return 'pkg/component' string for am start, or None if no launcher."""
    out = adb.shell_out(
        f"cmd package resolve-activity --brief {shlex.quote(pkg)} 2>/dev/null | tail -n 1",
        timeout=10,
    ).strip()
    if "/" in out and " " not in out:
        return out
    out = adb.shell_out(
        f"pm dump {shlex.quote(pkg)} 2>/dev/null "
        f"| grep -E 'android.intent.category.LAUNCHER' -B 1 "
        f"| head -1",
        timeout=15,
    ).strip()
    m = re.search(rf"({re.escape(pkg)}/[^ ]+)", out)
    return m.group(1) if m else None


def _is_installed(adb: ADB, pkg: str) -> bool:
    out = adb.shell_out(f"pm path {shlex.quote(pkg)}", timeout=10).strip()
    return out.startswith("package:")


def _am_start_w(adb: ADB, component: str, timeout: int = 20) -> Dict[str, Any]:
    """Launch via `am start -W` and parse TotalTime."""
    out = adb.shell_out(f"am start -W -n {shlex.quote(component)}", timeout=timeout)
    total_time = None
    wait_time = None
    started = None
    for line in out.splitlines():
        m = re.match(r"\s*TotalTime:\s*(\d+)", line)
        if m: total_time = int(m.group(1))
        m = re.match(r"\s*WaitTime:\s*(\d+)", line)
        if m: wait_time = int(m.group(1))
        m = re.match(r"\s*Status:\s*(\w+)", line)
        if m: started = m.group(1)
    return {
        "component": component,
        "total_time_ms": total_time,
        "wait_time_ms": wait_time,
        "status": started,
        "raw_tail": out[-300:],
    }


def _list_running_processes(adb: ADB) -> int:
    """Count of processes from `dumpsys meminfo` Cached: section."""
    out = adb.shell_out("dumpsys meminfo -c 2>/dev/null | head -200", timeout=15)
    count = 0
    for line in out.splitlines():
        if "ProcessRecord" in line and "cch" in line.lower():
            count += 1
    return count


def run_multitask_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if getattr(args, "skip_multitask", False):
        return {"status": "skipped", "reason": "--skip-multitask"}
    if adb.dry_run:
        write_csv(details / "multitask.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not exercise multitask"}

    extra = list(getattr(args, "multitask_apps", []) or [])
    discovered = _autodiscover_example_apps(adb)
    # Prefer com.example.* (our own predictable APKs) as the first app since
    # vendor-customized Settings/etc may not return TotalTime via am start -W.
    our_first = [p for p in discovered if p.startswith("com.example.wechatfriendfor")]
    candidates = list(extra) + our_first + list(DEFAULT_CANDIDATES) + list(discovered)
    seen, ordered = set(), []
    for c in candidates:
        if c not in seen:
            seen.add(c); ordered.append(c)

    # Resolve installed + has launcher
    plan: List[Tuple[str, str]] = []
    for pkg in ordered:
        if not _is_installed(adb, pkg):
            continue
        comp = _resolve_main_activity(adb, pkg)
        if comp:
            plan.append((pkg, comp))
        if len(plan) >= int(getattr(args, "multitask_max_apps", 8)):
            break

    if len(plan) < 3:
        return {
            "status": "skipped",
            "reason": f"only found {len(plan)} launchable apps; need ≥3 for multitask probe",
            "plan": plan,
        }

    log("INFO", f"Multitask: chain of {len(plan)} apps starting with {plan[0][0]}")
    rows: List[Dict[str, Any]] = []
    settle = float(getattr(args, "multitask_settle_sec", 1.5))
    cached_before: Optional[int] = None
    cached_after: Optional[int] = None
    cold: Dict[str, Any] = {}
    revisit: Dict[str, Any] = {}
    first_pkg = plan[0][0]
    try:
        first_pkg, first_comp = plan[0]
        adb.shell(f"am force-stop {shlex.quote(first_pkg)}", timeout=10)
        time.sleep(0.5)
        cached_before = _list_running_processes(adb)
        cold = _am_start_w(adb, first_comp)
        rows.append({"step": 1, "kind": "first_cold", "pkg": first_pkg, **cold,
                     "cached_processes_after": _list_running_processes(adb)})
        time.sleep(settle)

        for i, (pkg, comp) in enumerate(plan[1:], start=2):
            adb.shell(f"am force-stop {shlex.quote(pkg)}", timeout=10)
            time.sleep(0.3)
            out = _am_start_w(adb, comp)
            rows.append({"step": i, "kind": "chain", "pkg": pkg, **out,
                         "cached_processes_after": _list_running_processes(adb)})
            time.sleep(settle)

        revisit = _am_start_w(adb, first_comp)
        cached_after = _list_running_processes(adb)
        rows.append({"step": len(plan) + 1, "kind": "first_revisit", "pkg": first_pkg, **revisit,
                     "cached_processes_after": cached_after})
    finally:
        # Always tear down everything we launched, even if a step blew up midway —
        # otherwise the chain (8+ apps incl. our test APKs) keeps draining battery.
        for pkg, _ in plan:
            adb.shell(f"am force-stop {shlex.quote(pkg)}", timeout=10)

    write_csv(details / "multitask.csv", rows)

    cold_ms = cold.get("total_time_ms")
    revisit_ms = revisit.get("total_time_ms")
    ratio = round(revisit_ms / cold_ms, 3) if (cold_ms and revisit_ms) else None
    # Heuristic: if revisit takes > 60% of cold, the app was likely evicted (cold-ish restart).
    # If revisit < 20% of cold, the activity was still warm in RAM.
    evicted = (ratio is not None and ratio >= 0.6)
    return {
        "status": "advisory",
        "apps_in_chain": len(plan),
        "first_pkg": first_pkg,
        "first_cold_ms": cold_ms,
        "first_revisit_ms": revisit_ms,
        "revisit_to_cold_ratio": ratio,
        "first_evicted": evicted,
        "cached_processes_before": cached_before,
        "cached_processes_after": cached_after,
        "rows": rows,
        "advisory_reason": (
            "heuristic: revisit_ms / cold_ms >= 0.6 → first app was likely evicted. "
            "Real-world depends on app size (our chain is mixed system + scrolling test APKs)."
        ),
    }
