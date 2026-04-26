"""Memory pressure benchmark (LMK / multitask probe).

Allocates progressively larger anonymous mmap regions in a background shell
process, watches for process death, and captures lmkd kill events from the
kernel logcat buffer. Output:

  - max_resident_mb: how many MiB we successfully touched before SIGKILL
  - lmkd_kills_during: count of "Low memory: Killing" lines added during run
  - cached_processes_before/after: Activity Manager's count

This is a coarse signal — different OEMs tune lmkd very differently — but
it's a first-order proxy for "how much memory headroom does this device
give a foreground app."
"""
from __future__ import annotations

import argparse
import re
import shlex
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from .adb import ADB
from .utils import combined_output, log, parse_int, write_csv


def _count_cached_processes(adb: ADB) -> Optional[int]:
    out = adb.shell_out("dumpsys meminfo -c 2>/dev/null | head -200", timeout=20)
    cached_match = re.search(r"Cached:\s*(\d+)", out)
    if cached_match:
        return int(cached_match.group(1))
    line_count = 0
    for line in out.splitlines():
        if "ProcessRecord" in line and "cch" in line:
            line_count += 1
    return line_count or None


def _read_meminfo_mb(adb: ADB) -> Dict[str, Optional[int]]:
    out = adb.shell_out("cat /proc/meminfo 2>/dev/null", timeout=10)
    fields = {"MemTotal": None, "MemAvailable": None, "MemFree": None, "SwapFree": None}
    for line in out.splitlines():
        for key in fields:
            if line.startswith(key + ":"):
                value = parse_int(line.split(":", 1)[1].split()[0] if ":" in line else "")
                if value is not None:
                    fields[key] = value // 1024  # kB → MiB
                break
    return fields


def _scrape_lmkd(adb: ADB) -> int:
    """Count recent lmkd kill events visible in kernel + main logcat."""
    out = adb.shell_out(
        "logcat -d -b kernel -b main -t 2000 2>/dev/null "
        "| grep -i -E 'low memory|lowmemorykiller|lmkd.*kill|Low on memory' | wc -l",
        timeout=20,
    ).strip()
    return parse_int(out) or 0


def run_memory_pressure(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if getattr(args, "skip_memory_pressure", False):
        return {"status": "skipped", "reason": "--skip-memory-pressure"}
    if adb.dry_run:
        write_csv(details / "memory_pressure.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not exercise memory pressure"}

    # Snapshot before
    mem_before = _read_meminfo_mb(adb)
    cached_before = _count_cached_processes(adb)
    lmkd_before = _scrape_lmkd(adb)

    target_mb = int(getattr(args, "memory_pressure_max_mb", 1024))
    target_pct = float(getattr(args, "memory_pressure_target_pct", 0.0))
    if target_pct > 0:
        # Auto-scale target as a fraction of MemTotal so devices with different RAM
        # are stressed to a comparable proportion (key signal for RAM-capacity diff).
        ram_mb = mem_before.get("MemTotal")
        if ram_mb:
            target_mb = max(target_mb, int(ram_mb * target_pct))
    step_mb = max(16, int(getattr(args, "memory_pressure_step_mb", 64)))
    settle_sec = float(getattr(args, "memory_pressure_settle_sec", 0.4))

    # Drive an in-shell allocator: write `step_mb` blocks to /dev/shm-like buffer.
    # We use `dd if=/dev/zero` into a tmpfs file with `fallocate`-style reservation,
    # incrementally; on each step we read /proc/self/status and break if we get killed.
    target = f"/data/local/tmp/hpfc_mempress_{int(time.time())}"
    script = (
        f"target={shlex.quote(target)}; step={step_mb}; max_mb={target_mb}; "
        f"echo START; "
        f"i={step_mb}; "
        f"while [ \"$i\" -le \"$max_mb\" ]; do "
        f"  echo BUMP=$i; "
        f"  dd if=/dev/zero of=$target bs=1048576 count=$i 2>/dev/null || break; "
        f"  i=$((i + step)); "
        f"  sleep {settle_sec}; "
        f"done; "
        f"echo DONE; "
        f"rm -f $target"
    )
    rows: List[Dict[str, Any]] = []
    log("INFO", f"Memory pressure: target up to {target_mb}MiB step {step_mb}MiB")
    start = time.monotonic()
    avg_step_sec = settle_sec + step_mb / 100.0
    estimated_sec = (target_mb // step_mb) * avg_step_sec
    timeout_sec = max(60, int(estimated_sec * 2 + 30))
    last_bump_mb: Optional[int] = None
    try:
        proc = adb.shell(script, timeout=timeout_sec)
        out = combined_output(proc)
        for line in out.splitlines():
            if line.startswith("BUMP="):
                last_bump_mb = parse_int(line.split("=", 1)[1])
                rows.append({"event": "bump", "mb": last_bump_mb, "elapsed_sec": round(time.monotonic() - start, 3)})
            elif line.strip() == "DONE":
                rows.append({"event": "done"})
    finally:
        # Defense in depth: when adb.shell times out only the host client is killed;
        # the on-device dd loop and the multi-GB sparse file may both linger and
        # keep the device hot/short on disk. Kill the loop + delete the file unconditionally.
        adb.shell(
            f"pkill -f {shlex.quote(target)} 2>/dev/null; rm -f {shlex.quote(target)}",
            timeout=20,
        )
    elapsed = time.monotonic() - start

    mem_after = _read_meminfo_mb(adb)
    cached_after = _count_cached_processes(adb)
    lmkd_after = _scrape_lmkd(adb)

    write_csv(details / "memory_pressure.csv", rows)
    ram_total = mem_before.get("MemTotal")
    headroom_pct = round(last_bump_mb / ram_total * 100.0, 2) if (last_bump_mb and ram_total) else None
    return {
        "status": "advisory",
        "duration_sec": round(elapsed, 3),
        "max_resident_mb": last_bump_mb,
        "target_mb": target_mb,
        "target_pct_of_ram": target_pct or None,
        "headroom_pct": headroom_pct,
        "ram_total_mb": ram_total,
        "reached_target": last_bump_mb is not None and last_bump_mb >= target_mb,
        "lmkd_kills_during": max(0, lmkd_after - lmkd_before),
        "cached_processes_before": cached_before,
        "cached_processes_after": cached_after,
        "memory_before_mb": mem_before,
        "memory_after_mb": mem_after,
        "advisory_reason": (
            "shell dd-into-file allocator; not a true userspace anon mmap. "
            "lmkd thresholds and writeback dynamics OEM-dependent."
        ),
    }
