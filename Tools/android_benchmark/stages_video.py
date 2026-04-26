"""Video decode/encode probe (advisory).

Real video decode benchmarking on Android needs MediaCodec via Java/JNI; we
can't do that from a shell binary. As an advisory signal we run two short
tasks and time them on the wall clock:

  - `screenrecord --time-limit N` (uses HW encoder)
  - `cmd media check-codecs` to dump available codecs

These give a qualitative picture of codec availability and encode-side
latency, not a real fps number. Status is always advisory.
"""
from __future__ import annotations

import argparse
import re
import shlex
import time
from pathlib import Path
from typing import Any, Dict, List

from .adb import ADB
from .utils import combined_output, log, write_csv


def list_codecs(adb: ADB) -> Dict[str, Any]:
    out = adb.shell_out("dumpsys media.player 2>/dev/null | head -200", timeout=20)
    omx = adb.shell_out("dumpsys media.codec 2>/dev/null | head -200", timeout=20)
    return {"player_dump_kib": len(out) / 1024.0, "codec_dump_kib": len(omx) / 1024.0,
            "player_tail": out[:800], "codec_tail": omx[:800]}


def screenrecord_probe(adb: ADB, seconds: int, bit_rate: int) -> Dict[str, Any]:
    target = f"/data/local/tmp/hpfc_screenrec_{int(time.time())}.mp4"
    cmd = (
        f"screenrecord --time-limit {seconds} --bit-rate {bit_rate} "
        f"--size 720x1280 {shlex.quote(target)}"
    )
    log("INFO", f"Video screenrecord probe: {seconds}s @ {bit_rate}bps")
    start = time.monotonic()
    proc = adb.shell(cmd, timeout=max(60, seconds * 4))
    elapsed = time.monotonic() - start
    size_out = adb.shell_out(f"stat -c %s {shlex.quote(target)} 2>/dev/null || wc -c < {shlex.quote(target)} 2>/dev/null",
                             timeout=10).strip()
    try:
        size_bytes = int(size_out.splitlines()[-1])
    except (ValueError, IndexError):
        size_bytes = None
    adb.shell(f"rm -f {shlex.quote(target)}", timeout=10)
    return {
        "status": "success" if proc.returncode == 0 and (size_bytes or 0) > 0 else "failed",
        "duration_sec": round(elapsed, 3),
        "encoder_overhead_sec": round(elapsed - seconds, 3),
        "output_bytes": size_bytes,
        "estimated_mb_per_sec": round((size_bytes or 0) / 1024.0 / 1024.0 / max(seconds, 1), 3),
    }


def run_video_benchmark(adb: ADB, args: argparse.Namespace, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if getattr(args, "skip_video", False):
        return {"status": "skipped", "reason": "--skip-video"}
    if adb.dry_run:
        write_csv(details / "video_probe.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not probe video"}
    seconds = int(getattr(args, "video_seconds", 4))
    bit_rate = int(getattr(args, "video_bit_rate", 8_000_000))
    sr = screenrecord_probe(adb, seconds, bit_rate)
    codecs = list_codecs(adb)
    rows: List[Dict[str, Any]] = [
        {"test": "screenrecord_encoder", **sr},
        {"test": "codec_dump_size_kib", "kib": codecs["codec_dump_kib"]},
    ]
    write_csv(details / "video_probe.csv", rows)
    return {
        "status": "advisory" if sr.get("status") == "success" else "failed",
        "advisory_reason": ("Wall-clock encode probe via screenrecord; not a real-decoder fps. "
                            "Use MediaCodec test APK for true codec benchmarks."),
        "screenrecord": sr,
        "codec_summary": codecs,
    }
