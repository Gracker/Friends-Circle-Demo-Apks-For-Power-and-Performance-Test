from __future__ import annotations

import argparse
import os
import re
import shlex
import subprocess
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

from .adb import ADB
from .collectors import collect_cpu_topology, collect_current_sysfs, collect_thermal_zones, parse_battery_dump
from .stats import median_metric, numeric_summary, numeric_values
from .utils import combined_output, log, parse_float, parse_int, sanitize_for_filename, write_csv

def parse_ping(text: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {"raw": text.strip()}
    loss = re.search(r"(\d+(?:\.\d+)?)%\s*packet loss", text)
    if loss:
        result["packet_loss_pct"] = float(loss.group(1))
    packets = re.search(r"(\d+)\s+packets transmitted,\s+(\d+)\s+(?:packets )?received", text)
    if packets:
        result["transmitted"] = int(packets.group(1))
        result["received"] = int(packets.group(2))
    stats = re.search(
        r"(?:rtt|round-trip).*?=\s*([\d.]+)/([\d.]+)/([\d.]+)(?:/([\d.]+))?\s*ms",
        text,
        re.IGNORECASE,
    )
    if stats:
        result["min_ms"] = float(stats.group(1))
        result["avg_ms"] = float(stats.group(2))
        result["max_ms"] = float(stats.group(3))
        if stats.group(4) is not None:
            result["jitter_ms"] = float(stats.group(4))
    return result


def find_hash_command(adb: ADB) -> Optional[str]:
    out = adb.shell_out(
        "if command -v sha256sum >/dev/null 2>&1; then echo sha256sum; "
        "elif command -v md5sum >/dev/null 2>&1; then echo md5sum; "
        "else echo ''; fi",
        timeout=10,
    ).strip()
    if out in ("sha256sum", "md5sum"):
        return out
    return None


def get_cpu_count(adb: ADB, fallback: int = 1) -> int:
    out = adb.shell_out("ls -d /sys/devices/system/cpu/cpu[0-9]* 2>/dev/null | wc -l", timeout=10)
    count = parse_int(out)
    return max(1, count or fallback)


def timed_shell(adb: ADB, command: str, timeout: int) -> Tuple[subprocess.CompletedProcess[str], float]:
    start = time.monotonic()
    proc = adb.shell(command, timeout=timeout)
    elapsed = time.monotonic() - start
    return proc, elapsed


def run_cpu_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if adb.dry_run:
        write_csv(details / "cpu_compute.csv", [{"status": "dry_run", "reason": "dry-run does not execute CPU workload"}])
        return {"status": "dry_run", "reason": "dry-run does not execute CPU workload", "rows": []}
    hash_cmd = find_hash_command(adb)
    if not hash_cmd:
        return {
            "status": "skipped",
            "reason": "sha256sum/md5sum not available on device shell",
            "rows": [],
        }

    cpu_count = get_cpu_count(adb)
    max_workers = args.cpu_max_workers if args.cpu_max_workers > 0 else min(cpu_count, 8)
    worker_counts = sorted({1, min(2, max_workers), min(4, max_workers), max_workers})
    worker_counts = [x for x in worker_counts if x >= 1]

    rows: List[Dict[str, Any]] = []
    for iteration in range(1, int(args.benchmark_iterations) + 1):
        for workers in worker_counts:
            per_worker_mb = args.cpu_mb if workers == 1 else max(16, args.cpu_mb // 2)
            total_mb = per_worker_mb * workers
            script = (
                f"i=0; while [ \"$i\" -lt {workers} ]; do "
                f"(dd if=/dev/zero bs=1048576 count={per_worker_mb} 2>/dev/null | {hash_cmd} >/dev/null 2>&1) & "
                "i=$((i + 1)); "
                "done; wait"
            )
            timeout = max(60, int(total_mb / 2) + 30)
            log("INFO", f"CPU hash iteration={iteration}: workers={workers}, total={total_mb}MiB")
            proc, elapsed = timed_shell(adb, script, timeout=timeout)
            status = "success" if proc.returncode == 0 else "failed"
            rows.append(
                {
                    "iteration": iteration,
                    "workers": workers,
                    "hash": hash_cmd,
                    "per_worker_mib": per_worker_mb,
                    "total_mib": total_mb,
                    "duration_sec": round(elapsed, 4),
                    "throughput_mib_s": round(total_mb / elapsed, 3) if elapsed > 0 and status == "success" else None,
                    "status": status,
                    "output": combined_output(proc)[:500],
                }
            )
    write_csv(details / "cpu_compute.csv", rows)
    good_rows = [r for r in rows if r.get("status") == "success" and r.get("throughput_mib_s")]
    summaries = []
    for workers in worker_counts:
        worker_rows = [r for r in good_rows if r.get("workers") == workers]
        summaries.append(
            {
                "workers": workers,
                "throughput_mib_s": numeric_summary(numeric_values(worker_rows, "throughput_mib_s")),
            }
        )
    single_rows = [r for r in good_rows if r["workers"] == 1]
    best_summary = max(
        summaries,
        key=lambda r: parse_float(r["throughput_mib_s"].get("median")) or 0.0,
        default=None,
    )
    return {
        "status": "advisory" if good_rows else "failed",
        "advisory_reason": (
            "shell 'dd /dev/zero | sha256sum' on ARMv8 uses the hardware crypto "
            "extension; this measures HW crypto throughput + kernel memcpy, not "
            "general CPU compute. Use native.cpu_int_* / cpu_fp_* for CPU score."
        ),
        "method": "dd /dev/zero piped through device sha256sum/md5sum",
        "iterations": int(args.benchmark_iterations),
        "rows": rows,
        "summaries": summaries,
        "crypto_single_mib_s": median_metric(single_rows, "throughput_mib_s"),
        "crypto_best_multi_mib_s": best_summary["throughput_mib_s"].get("median") if best_summary else None,
        "best_workers": best_summary.get("workers") if best_summary else None,
    }


def run_memory_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if adb.dry_run:
        row = {"method": "dd_zero_to_null", "status": "dry_run", "reason": "dry-run does not execute memory workload"}
        write_csv(device_dir / "details" / "memory_bw.csv", [row])
        return {"status": "dry_run", "reason": "dry-run does not execute memory workload", "row": row}
    total_mb = args.memory_mb
    rows: List[Dict[str, Any]] = []
    for iteration in range(1, int(args.benchmark_iterations) + 1):
        command = f"dd if=/dev/zero of=/dev/null bs=1048576 count={total_mb} 2>/dev/null"
        log("INFO", f"Memory stream proxy iteration={iteration}: {total_mb}MiB /dev/zero -> /dev/null")
        proc, elapsed = timed_shell(adb, command, timeout=max(60, int(total_mb / 4) + 30))
        rows.append(
            {
                "iteration": iteration,
                "method": "dd_zero_to_null",
                "total_mib": total_mb,
                "duration_sec": round(elapsed, 4),
                "throughput_mib_s": round(total_mb / elapsed, 3) if elapsed > 0 and proc.returncode == 0 else None,
                "status": "success" if proc.returncode == 0 else "failed",
                "output": combined_output(proc)[:500],
            }
        )
    write_csv(device_dir / "details" / "memory_bw.csv", rows)
    good_rows = [r for r in rows if r.get("status") == "success"]
    throughput_summary = numeric_summary(numeric_values(good_rows, "throughput_mib_s"))
    return {
        "status": "advisory" if good_rows else "failed",
        "advisory_reason": (
            "'dd /dev/zero -> /dev/null' is a kernel syscall loop; it never allocates "
            "a userspace buffer and may not touch DRAM. Use native.memory_triad_mib_s "
            "for real memory bandwidth."
        ),
        "method": "kernel syscall loop (not STREAM Triad)",
        "iterations": int(args.benchmark_iterations),
        "kernel_loop_mib_s": throughput_summary.get("median"),
        "throughput_summary": throughput_summary,
        "rows": rows,
    }


def run_storage_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if adb.dry_run:
        row = {"test": "storage", "status": "dry_run", "reason": "dry-run does not execute storage workload"}
        write_csv(details / "storage_io.csv", [row])
        return {"status": "dry_run", "reason": "dry-run does not execute storage workload", "rows": [row]}
    remote_dir = f"/data/local/tmp/hpfc_bench_{int(time.time())}_{os.getpid()}"
    remote_file = f"{remote_dir}/storage.bin"
    size_mb = args.storage_mb
    rows: List[Dict[str, Any]] = []
    adb.shell(f"mkdir -p {shlex.quote(remote_dir)}", timeout=20)

    try:
        for iteration in range(1, int(args.benchmark_iterations) + 1):
            write_cmd = (
                f"(dd if=/dev/zero of={shlex.quote(remote_file)} bs=1048576 count={size_mb} conv=fsync 2>/dev/null) "
                f"|| (rm -f {shlex.quote(remote_file)}; "
                f"dd if=/dev/zero of={shlex.quote(remote_file)} bs=1048576 count={size_mb} 2>/dev/null; sync)"
            )
            log("INFO", f"Storage sequential write iteration={iteration}: {size_mb}MiB")
            proc, elapsed = timed_shell(adb, write_cmd, timeout=max(120, size_mb * 4))
            rows.append(
                {
                    "iteration": iteration,
                    "test": "sequential_write",
                    "block_kib": 1024,
                    "size_mib": size_mb,
                    "ops": "",
                    "duration_sec": round(elapsed, 4),
                    "throughput_mib_s": round(size_mb / elapsed, 3) if elapsed > 0 and proc.returncode == 0 else None,
                    "iops": "",
                    "status": "success" if proc.returncode == 0 else "failed",
                    "output": combined_output(proc)[:500],
                }
            )

            read_cmd = f"dd if={shlex.quote(remote_file)} of=/dev/null bs=1048576 count={size_mb} 2>/dev/null"
            log("INFO", f"Storage sequential read iteration={iteration}: {size_mb}MiB (page-cache likely hot)")
            proc, elapsed = timed_shell(adb, read_cmd, timeout=max(120, size_mb * 4))
            rows.append(
                {
                    "iteration": iteration,
                    "test": "sequential_read",
                    "block_kib": 1024,
                    "size_mib": size_mb,
                    "ops": "",
                    "duration_sec": round(elapsed, 4),
                    "throughput_mib_s": round(size_mb / elapsed, 3) if elapsed > 0 and proc.returncode == 0 else None,
                    "iops": "",
                    "status": "success" if proc.returncode == 0 else "failed",
                    "cache_warning": "buffered_read: value reflects page cache hit rate, not raw storage",
                    "output": combined_output(proc)[:500],
                }
            )

            if args.storage_random_ops > 0:
                blocks = max(1, size_mb * 256)
                random_read = (
                    f"file={shlex.quote(remote_file)}; ops={args.storage_random_ops}; blocks={blocks}; "
                    "seed=12345; i=0; "
                    'while [ "$i" -lt "$ops" ]; do '
                    "seed=$(((seed * 1103515245 + 12345) % 2147483647)); "
                    "block=$((seed % blocks)); "
                    'dd if="$file" of=/dev/null bs=4096 skip="$block" count=1 2>/dev/null; '
                    "i=$((i + 1)); "
                    "done"
                )
                log("INFO", f"Storage random read iteration={iteration}: {args.storage_random_ops} ops (shell-fork bound)")
                proc, elapsed = timed_shell(adb, random_read, timeout=max(120, args.storage_random_ops // 2 + 60))
                rows.append(
                    {
                        "iteration": iteration,
                        "test": "random_read",
                        "block_kib": 4,
                        "size_mib": size_mb,
                        "ops": args.storage_random_ops,
                        "duration_sec": round(elapsed, 4),
                        "throughput_mib_s": round((args.storage_random_ops * 4 / 1024) / elapsed, 3)
                        if elapsed > 0 and proc.returncode == 0
                        else None,
                        "iops": round(args.storage_random_ops / elapsed, 2) if elapsed > 0 and proc.returncode == 0 else None,
                        "status": "advisory" if proc.returncode == 0 else "failed",
                        "advisory_reason": "shell while+dd: IOPS bounded by process-fork overhead, not storage",
                        "output": combined_output(proc)[:500],
                    }
                )

                random_write = (
                    f"file={shlex.quote(remote_file)}; ops={args.storage_random_ops}; blocks={blocks}; "
                    "seed=54321; i=0; "
                    'while [ "$i" -lt "$ops" ]; do '
                    "seed=$(((seed * 1103515245 + 12345) % 2147483647)); "
                    "block=$((seed % blocks)); "
                    'dd if=/dev/zero of="$file" bs=4096 seek="$block" count=1 conv=notrunc 2>/dev/null; '
                    "i=$((i + 1)); "
                    "done; sync"
                )
                log("INFO", f"Storage random write iteration={iteration}: {args.storage_random_ops} ops (shell-fork bound)")
                proc, elapsed = timed_shell(adb, random_write, timeout=max(120, args.storage_random_ops + 60))
                rows.append(
                    {
                        "iteration": iteration,
                        "test": "random_write",
                        "block_kib": 4,
                        "size_mib": size_mb,
                        "ops": args.storage_random_ops,
                        "duration_sec": round(elapsed, 4),
                        "throughput_mib_s": round((args.storage_random_ops * 4 / 1024) / elapsed, 3)
                        if elapsed > 0 and proc.returncode == 0
                        else None,
                        "iops": round(args.storage_random_ops / elapsed, 2) if elapsed > 0 and proc.returncode == 0 else None,
                        "status": "advisory" if proc.returncode == 0 else "failed",
                        "advisory_reason": "shell while+dd: IOPS bounded by process-fork overhead, not storage",
                        "output": combined_output(proc)[:500],
                    }
                )
    finally:
        if not args.keep_device_temp:
            adb.shell(f"rm -rf {shlex.quote(remote_dir)}", timeout=30)

    write_csv(details / "storage_io.csv", rows)
    all_ok_rows = [row for row in rows if row.get("status") in ("success", "advisory")]
    summaries = {
        test: {
            "throughput_mib_s": numeric_summary(numeric_values([r for r in all_ok_rows if r.get("test") == test], "throughput_mib_s")),
            "iops": numeric_summary(numeric_values([r for r in all_ok_rows if r.get("test") == test], "iops")),
        }
        for test in ("sequential_write", "sequential_read", "random_read", "random_write")
    }
    return {
        "status": "success" if any(r.get("status") == "success" for r in rows) else "failed",
        "remote_dir": remote_dir if args.keep_device_temp else None,
        "iterations": int(args.benchmark_iterations),
        "rows": rows,
        "summaries": summaries,
        "sequential_write_mib_s": summaries["sequential_write"]["throughput_mib_s"].get("median"),
        "buffered_read_mib_s": summaries["sequential_read"]["throughput_mib_s"].get("median"),
        "buffered_read_advisory": "page cache hot; not raw storage throughput",
        "random_read_iops_advisory": summaries["random_read"]["iops"].get("median"),
        "random_write_iops_advisory": summaries["random_write"]["iops"].get("median"),
        "random_iops_advisory_reason": "shell while+dd is bounded by fork overhead",
    }


def min_freq_ratio(cpu_rows: Sequence[Dict[str, Any]]) -> Optional[float]:
    ratios: List[float] = []
    for row in cpu_rows:
        cur = parse_float(row.get("scaling_cur_khz"))
        max_freq = parse_float(row.get("max_khz"))
        if cur is not None and max_freq and max_freq > 0:
            ratios.append(cur / max_freq)
    return min(ratios) if ratios else None


def run_thermal_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if adb.dry_run:
        write_csv(device_dir / "details" / "thermal_curve.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not execute thermal workload"}
    if args.thermal_duration_sec <= 0:
        return {"status": "skipped", "reason": "thermal duration is 0"}
    hash_cmd = find_hash_command(adb)
    if not hash_cmd:
        return {"status": "skipped", "reason": "sha256sum/md5sum not available on device shell"}

    cpu_count = get_cpu_count(adb)
    workers = args.thermal_workers if args.thermal_workers > 0 else min(cpu_count, 8)
    details = device_dir / "details"
    stress_local = details / "hpfc_cpu_stress.sh"
    stress_local.parent.mkdir(parents=True, exist_ok=True)
    stress_local.write_text(
        """#!/system/bin/sh
workers="$1"
stop_file="$2"
hash_cmd="$3"
i=0
while [ "$i" -lt "$workers" ]; do
  (
    while [ ! -f "$stop_file" ]; do
      dd if=/dev/zero bs=1048576 count=16 2>/dev/null | "$hash_cmd" >/dev/null 2>&1
    done
  ) &
  i=$((i + 1))
done
wait
""",
        encoding="utf-8",
    )
    remote_script = f"/data/local/tmp/hpfc_cpu_stress_{os.getpid()}.sh"
    remote_stop = f"/data/local/tmp/hpfc_cpu_stress_stop_{os.getpid()}"
    adb.push(stress_local, remote_script, timeout=30)
    adb.shell(f"chmod 755 {shlex.quote(remote_script)}; rm -f {shlex.quote(remote_stop)}", timeout=20)
    start_cmd = (
        f"sh -c 'sh {shlex.quote(remote_script)} {workers} "
        f"{shlex.quote(remote_stop)} {shlex.quote(hash_cmd)} >/dev/null 2>&1 & echo $!'"
    )
    pid = adb.shell_out(start_cmd, timeout=20).strip().splitlines()[-1:] or [""]
    pid_text = pid[0]
    log("INFO", f"Thermal stress: workers={workers}, duration={args.thermal_duration_sec}s, pid={pid_text}")

    rows: List[Dict[str, Any]] = []
    start = time.monotonic()
    try:
        while True:
            elapsed = time.monotonic() - start
            zones = collect_thermal_zones(adb)
            cpu_rows = collect_cpu_topology(adb)
            temps = [v for v in zones.values() if v is not None]
            ratio = min_freq_ratio(cpu_rows)
            row: Dict[str, Any] = {
                "elapsed_sec": round(elapsed, 3),
                "max_temp_c": round(max(temps), 2) if temps else "",
                "min_freq_ratio": round(ratio, 4) if ratio is not None else "",
            }
            for key, value in zones.items():
                row[f"thermal_{sanitize_for_filename(key)}_c"] = "" if value is None else round(value, 2)
            rows.append(row)
            if elapsed >= args.thermal_duration_sec:
                break
            time.sleep(max(1.0, args.sample_interval_sec))
    finally:
        adb.shell(
            f"touch {shlex.quote(remote_stop)}; sleep 1; "
            f"kill {shlex.quote(pid_text)} 2>/dev/null || true; "
            f"rm -f {shlex.quote(remote_stop)} {shlex.quote(remote_script)}",
            timeout=20,
        )

    write_csv(details / "thermal_curve.csv", rows)
    max_temps = [parse_float(row.get("max_temp_c")) for row in rows if parse_float(row.get("max_temp_c")) is not None]
    ratios = [
        parse_float(row.get("min_freq_ratio"))
        for row in rows
        if parse_float(row.get("min_freq_ratio")) is not None
    ]
    first_low_ratio_sec = None
    for row in rows:
        ratio = parse_float(row.get("min_freq_ratio"))
        if ratio is not None and ratio < args.throttle_ratio_threshold:
            first_low_ratio_sec = row.get("elapsed_sec")
            break
    return {
        "status": "advisory" if rows else "failed",
        "advisory_reason": (
            "stressor is dd|sha256sum; on ARMv8 sha256 uses crypto extension so "
            "big cluster may not saturate. Treat thermal delta / min_freq_ratio as "
            "indicative, not authoritative."
        ),
        "stress_method": "shell dd|sha256sum (hw-accelerated)",
        "workers": workers,
        "duration_sec": args.thermal_duration_sec,
        "sample_interval_sec": args.sample_interval_sec,
        "max_temp_c": round(max(max_temps), 2) if max_temps else None,
        "start_temp_c": round(max_temps[0], 2) if max_temps else None,
        "delta_temp_c": round(max(max_temps) - max_temps[0], 2) if len(max_temps) >= 2 else None,
        "min_freq_ratio": round(min(ratios), 4) if ratios else None,
        "first_low_ratio_sec": first_low_ratio_sec,
        "csv": str(details / "thermal_curve.csv"),
    }


def collect_battery_sample(adb: ADB) -> Dict[str, Any]:
    sample = parse_battery_dump(adb.shell_out("dumpsys battery", timeout=20))
    sample.update(collect_current_sysfs(adb))
    return sample


def run_battery_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if adb.dry_run:
        write_csv(device_dir / "details" / "battery_drain.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not sample battery"}
    if args.battery_duration_sec <= 0:
        return {"status": "skipped", "reason": "battery duration is 0"}
    first_sample = collect_battery_sample(adb)
    charging = bool(
        first_sample.get("ac_powered")
        or first_sample.get("usb_powered")
        or first_sample.get("wireless_powered")
    )
    if charging and not getattr(args, "allow_charging_battery_stage", False):
        return {
            "status": "advisory",
            "advisory_reason": "device is charging; drain measurement is not meaningful",
            "duration_sec": 0,
            "csv": None,
        }
    rows: List[Dict[str, Any]] = []
    if args.battery_wake_screen:
        adb.shell("input keyevent KEYCODE_WAKEUP; input keyevent KEYCODE_MENU", timeout=10)
    start = time.monotonic()
    log("INFO", f"Battery sampling: duration={args.battery_duration_sec}s")
    while True:
        elapsed = time.monotonic() - start
        sample = collect_battery_sample(adb)
        row = {"elapsed_sec": round(elapsed, 3)}
        row.update(sample)
        rows.append(row)
        if elapsed >= args.battery_duration_sec:
            break
        time.sleep(max(1.0, args.sample_interval_sec))
    write_csv(device_dir / "details" / "battery_drain.csv", rows)
    first = rows[0] if rows else {}
    last = rows[-1] if rows else {}
    level_start = parse_float(first.get("level_pct"))
    level_end = parse_float(last.get("level_pct"))
    drain_pct: Optional[float] = None
    if level_start is not None and level_end is not None:
        drain_pct = round(level_start - level_end, 3)

    currents = [parse_float(r.get("current_now_ua")) for r in rows]
    currents = [c for c in currents if c is not None]
    voltages = [parse_float(r.get("voltage_now_uv")) for r in rows]
    voltages = [v for v in voltages if v is not None]
    avg_power_mw: Optional[float] = None
    if currents and voltages:
        avg_current_ua = sum(currents) / len(currents)
        avg_voltage_uv = sum(voltages) / len(voltages)
        avg_power_mw = round(abs(avg_current_ua) * abs(avg_voltage_uv) / 1e9, 3)

    return {
        "status": "success" if rows else "failed",
        "duration_sec": args.battery_duration_sec,
        "level_start_pct": level_start,
        "level_end_pct": level_end,
        "drain_pct": drain_pct,
        "avg_power_mw_estimate": avg_power_mw,
        "avg_power_note": "sign of current_now is OEM-dependent; |I|*|V| used",
        "samples": len(rows),
        "csv": str(device_dir / "details" / "battery_drain.csv"),
    }


def run_network_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if adb.dry_run:
        row = {"host": args.ping_host, "count": args.ping_count, "status": "dry_run"}
        write_csv(device_dir / "details" / "network.csv", [row])
        return {"status": "dry_run", "reason": "dry-run does not execute ping", "ping": row}
    command = f"ping -c {args.ping_count} -i {args.ping_interval_sec} {shlex.quote(args.ping_host)}"
    log("INFO", f"Network ping: host={args.ping_host}, count={args.ping_count}")
    proc, elapsed = timed_shell(adb, command, timeout=max(30, args.ping_count * 3))
    raw_out = combined_output(proc)
    parsed = parse_ping(raw_out)
    network_unreachable = "Network is unreachable" in raw_out or "unknown host" in raw_out.lower()
    if network_unreachable:
        row_status = "advisory"
        advisory_reason = "device has no usable network (Wi-Fi off / not connected); ping cannot run"
    else:
        row_status = "success" if proc.returncode == 0 else "failed"
        advisory_reason = None
    row = {
        "host": args.ping_host,
        "count": args.ping_count,
        "duration_sec": round(elapsed, 4),
        "status": row_status,
        "avg_ms": parsed.get("avg_ms"),
        "min_ms": parsed.get("min_ms"),
        "max_ms": parsed.get("max_ms"),
        "jitter_ms": parsed.get("jitter_ms"),
        "packet_loss_pct": parsed.get("packet_loss_pct"),
        "raw": parsed.get("raw", "")[:1000],
    }
    write_csv(device_dir / "details" / "network.csv", [row])
    return {
        "status": row_status,
        "advisory_reason": advisory_reason,
        "ping": row,
    }


def maybe_install_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if not args.install_benchmark_apk:
        return {"status": "skipped", "reason": "no install benchmark APK selected"}
    apk = Path(args.install_benchmark_apk)
    if not apk.exists():
        return {"status": "skipped", "reason": f"APK not found: {apk}"}
    if adb.dry_run:
        row = {"apk": str(apk), "status": "dry_run", "reason": "dry-run does not install APK"}
        write_csv(device_dir / "details" / "install_speed.csv", [row])
        return row
    log("INFO", f"Install benchmark: {apk.name}")
    start = time.monotonic()
    proc = adb.run(["install", "-r", "-d", str(apk)], timeout=args.install_timeout_sec)
    elapsed = time.monotonic() - start
    row = {
        "apk": str(apk),
        "size_mib": round(apk.stat().st_size / 1024 / 1024, 3),
        "duration_sec": round(elapsed, 4),
        "status": "success" if proc.returncode == 0 else "failed",
        "output": combined_output(proc)[:1000],
    }
    write_csv(device_dir / "details" / "install_speed.csv", [row])
    return row
