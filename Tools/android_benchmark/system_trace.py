from __future__ import annotations

import argparse
import os
import shlex
import time
from pathlib import Path
from typing import Any, Dict

from .adb import ADB
from .perfetto import TraceRecorder
from .utils import combined_output, log, write_csv


def run_system_trace_benchmark(serial: str, adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    if args.skip_system_trace:
        return {"status": "skipped", "reason": "--skip-system-trace"}
    trace_dir = device_dir / "system_trace"
    details = device_dir / "details"
    if adb.dry_run:
        write_csv(details / "system_trace.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not capture system trace"}

    trace_path = trace_dir / f"{serial}-mixed-{int(time.time())}.ptrace"
    recorder = TraceRecorder(
        serial=serial,
        output_path=trace_path,
        dry_run=adb.dry_run,
        duration_sec=args.system_trace_seconds + 2,
    )
    ok, message = recorder.start()
    if not ok:
        return {"status": "failed", "reason": f"trace start failed: {message}"}
    remote_file = f"/data/local/tmp/hpfc_system_trace_io_{os.getpid()}.bin"
    hash_cmd = "sha256sum"
    if "sha256sum" not in adb.shell_out("command -v sha256sum 2>/dev/null || true", timeout=10):
        hash_cmd = "md5sum"
    workload = (
        f"(dd if=/dev/zero bs=1048576 count={args.system_trace_cpu_mb} 2>/dev/null | {hash_cmd} >/dev/null 2>&1) & "
        f"dd if=/dev/zero of={shlex.quote(remote_file)} bs=1048576 count={args.system_trace_io_mb} conv=fsync 2>/dev/null; "
        f"dd if={shlex.quote(remote_file)} of=/dev/null bs=1048576 count={args.system_trace_io_mb} 2>/dev/null; "
        f"rm -f {shlex.quote(remote_file)}; wait"
    )
    log("INFO", f"System Perfetto mixed trace: cpu={args.system_trace_cpu_mb}MiB io={args.system_trace_io_mb}MiB")
    start = time.monotonic()
    try:
        proc = adb.shell(workload, timeout=max(60, args.system_trace_seconds + 60))
        remaining = args.system_trace_seconds - (time.monotonic() - start)
        if remaining > 0:
            time.sleep(remaining)
        trace_ok, stop_output = recorder.stop(timeout_sec=args.system_trace_stop_timeout_sec)
    finally:
        # If anything above raised or the shell timed out, the on-device dd may keep
        # writing the io probe file. Force-clean it and any orphan dd that's still
        # holding the path open.
        adb.shell(
            f"pkill -f {shlex.quote(remote_file)} 2>/dev/null; rm -f {shlex.quote(remote_file)}",
            timeout=20,
        )
    row = {
        "status": "success" if proc.returncode == 0 and trace_ok else "failed",
        "trace_path": str(trace_path) if trace_path.exists() else "",
        "trace_size_bytes": trace_path.stat().st_size if trace_path.exists() else "",
        "workload_returncode": proc.returncode,
        "workload_output": combined_output(proc)[:500],
        "trace_stop_output": stop_output[:500],
    }
    write_csv(details / "system_trace.csv", [row])
    return {
        **row,
        "trace_dir": str(trace_dir),
    }
