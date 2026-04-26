from __future__ import annotations

import html
import json
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

from .collectors import ANIMATION_SCALE_KEYS
from .utils import parse_float, write_csv

STAGE_TO_DIMENSION = [
    ("cpu", "CPU HW crypto / memcpy (advisory)", "shell dd|sha256sum — HW accelerated on ARMv8"),
    ("memory", "Kernel syscall loop (advisory)", "shell dd /dev/zero -> /dev/null"),
    ("native", "Native CPU + STREAM-like memory + pointer-chase latency", "self-built ARM64 benchmark with affinity"),
    ("geekbench_mix", "Geekbench-style mix (AES + SHA256 + FFT + matmul + n-body + sort)", "self-built ARM64 multi-kernel"),
    ("ux_data", "UX Data Processing (regex + JSON tokenize + sort)", "self-built ARM64"),
    ("ux_image", "UX Image Processing (blur + bilinear resize + sepia)", "self-built ARM64 RGBA pipeline"),
    ("storage", "Storage sequential + buffered read + shell random 4K (advisory)", "multi-iteration dd, fsync at end; random IOPS shell-fork bound"),
    ("storage_random_native", "True random 4K IOPS via pread64/pwrite64 (+/- O_DIRECT)", "self-built ARM64 storage helper"),
    ("sustained", "Sustained perf curve (N rounds, throttle drift)", "self-built ARM64; emits per-round timings"),
    ("per_cluster", "Per-cluster (big.LITTLE) int+fp throughput", "self-built ARM64 with sched_setaffinity"),
    ("latency_curve", "Memory hierarchy latency curve (L1d→DRAM)", "self-built ARM64 pointer-chase across working sets"),
    ("memory_pressure", "Memory pressure / LMK probe (advisory)", "progressive dd-allocate + lmkd log scrape"),
    ("multitask", "Multitask app-eviction probe (advisory)", "chained am start -W; revisit/cold ratio heuristic"),
    ("thermal", "Thermal curve under HW-accelerated hash stress (advisory)", "dd|sha256sum stress + thermal zone sampling"),
    ("battery", "Short-term battery drain + avg power estimate", "dumpsys battery + sysfs current_now/voltage_now sampling"),
    ("network", "Device ICMP ping (advisory)", "ping to --ping-host only; no DNS/HTTP yet"),
    ("install", "APK install wall time", "adb install elapsed"),
    ("system_trace", "Generic Perfetto mixed CPU/I/O trace", "dd + hash workload under full ftrace capture"),
    ("interaction", "Tap-to-onDraw click response (advisory)", "custom APK logcat timestamps; onDraw != visible"),
    ("launch", "App launch via Perfetto launch runner", "existing auto_record_perfetto_launch.py; cold-hardened on root"),
    ("scroll", "Scroll gfxinfo/Perfetto frame analysis", "existing perfetto_release_scroll_benchmark.py"),
    ("gpu", "GPU bench via glmark2-es2 (external binary)", "user-supplied /data/local/tmp/glmark2-es2"),
    ("ai", "TFLite benchmark_model (CPU/XNNPACK/GPU/NNAPI delegates)", "user-supplied /data/local/tmp/benchmark_model + .tflite model"),
    ("video", "Encoder wall-clock probe via screenrecord (advisory)", "screenrecord + media codec dump"),
]


def coverage_matrix(device_results: Optional[Sequence[Dict[str, Any]]] = None) -> List[Dict[str, Any]]:
    rows: List[Dict[str, Any]] = []
    stages_seen: Dict[str, Dict[str, int]] = {}
    if device_results:
        for device in device_results:
            for stage, bench in (device.get("benchmarks", {}) or {}).items():
                status = (bench or {}).get("status", "unknown")
                stages_seen.setdefault(stage, {}).setdefault(status, 0)
                stages_seen[stage][status] += 1
    for stage_key, dimension, method in STAGE_TO_DIMENSION:
        stats = stages_seen.get(stage_key)
        if stats:
            status_summary = ", ".join(f"{s}={n}" for s, n in sorted(stats.items()))
        else:
            status_summary = "not-run"
        rows.append(
            {
                "dimension": dimension,
                "stage": stage_key,
                "status": status_summary,
                "method": method,
            }
        )
    rows.append({"dimension": "GPU compute/render synthetic", "stage": "", "status": "external hook", "method": "OpenCL-Benchmark/glmark2 Android binary"})
    rows.append({"dimension": "Touch latency (end-to-end hardware)", "stage": "", "status": "out of scope", "method": "WALT / high-speed camera"})
    rows.append({"dimension": "Audio round-trip latency", "stage": "", "status": "out of scope", "method": "audiolat / OboeTester"})
    rows.append({"dimension": "System boot time", "stage": "", "status": "out of scope", "method": "requires reboot orchestration"})
    rows.append({"dimension": "Long-run battery Historian A/B", "stage": "", "status": "out of scope", "method": "batterystats + Historian (skipped)"})
    return rows


def _stage_status(b: Dict[str, Any], stage: str) -> str:
    value = (b.get(stage) or {}).get("status")
    return str(value) if value else "unknown"


def _advisory_reason(b: Dict[str, Any], stage: str) -> Optional[str]:
    return (b.get(stage) or {}).get("advisory_reason")


def _cpu_scaling_efficiency(native_summary: Dict[str, Any], cpu_count: Optional[int],
                             single_key: str, best_key: str) -> Optional[float]:
    """efficiency % = best_multi / (cpu_count × single)."""
    if not cpu_count:
        return None
    single = parse_float(native_summary.get(single_key))
    best = parse_float(native_summary.get(best_key))
    if not single or not best:
        return None
    ideal = single * cpu_count
    if ideal <= 0:
        return None
    return round(best / ideal * 100.0, 2)


def flatten_device_metrics(device_result: Dict[str, Any]) -> List[Dict[str, Any]]:
    b = device_result.get("benchmarks", {})
    info = device_result.get("device_info", {}) or {}
    cpu_count = parse_float(info.get("cpu_count"))
    cpu_count = int(cpu_count) if cpu_count else None
    native_summary = (b.get("native") or {}).get("summary") or {}
    int_scale = _cpu_scaling_efficiency(native_summary, cpu_count, "cpu_int_single_mops", "cpu_int_best_mops")
    fp_scale = _cpu_scaling_efficiency(native_summary, cpu_count, "cpu_fp_single_mflops", "cpu_fp_best_mflops")

    def rs(stage: str, **overrides: Any) -> Dict[str, Any]:
        status = _stage_status(b, stage)
        result = {
            "stage_status": status,
            "stage_advisory_reason": _advisory_reason(b, stage),
        }
        result.update(overrides)
        return result

    rows = [
        # Native CPU — the ONLY trustworthy CPU metrics
        metric_row("cpu", "Native integer single-thread", "native.cpu_int_single_mops", "Mops/s", "higher",
                   b.get("native", {}).get("summary", {}).get("cpu_int_single_mops"), **rs("native")),
        metric_row("cpu", "Native integer best multi", "native.cpu_int_best_mops", "Mops/s", "higher",
                   b.get("native", {}).get("summary", {}).get("cpu_int_best_mops"), **rs("native")),
        metric_row("cpu", "Native FP single-thread", "native.cpu_fp_single_mflops", "MFLOPS", "higher",
                   b.get("native", {}).get("summary", {}).get("cpu_fp_single_mflops"), **rs("native")),
        metric_row("cpu", "Native FP best multi", "native.cpu_fp_best_mflops", "MFLOPS", "higher",
                   b.get("native", {}).get("summary", {}).get("cpu_fp_best_mflops"), **rs("native")),
        metric_row("cpu", "Int multi-thread scaling efficiency", "native.cpu_int_scaling_pct", "%", "higher",
                   int_scale, **rs("native")),
        metric_row("cpu", "FP multi-thread scaling efficiency", "native.cpu_fp_scaling_pct", "%", "higher",
                   fp_scale, **rs("native")),
        # Per-cluster (big.LITTLE)
        metric_row("cpu", "Big-cluster int single", "cpu.big_int_mops", "Mops/s", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("big_int_mops"), **rs("per_cluster")),
        metric_row("cpu", "Big-cluster fp single", "cpu.big_fp_mflops", "MFLOPS", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("big_fp_mflops"), **rs("per_cluster")),
        metric_row("cpu", "Little-cluster int single", "cpu.little_int_mops", "Mops/s", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("little_int_mops"), **rs("per_cluster")),
        metric_row("cpu", "Little-cluster fp single", "cpu.little_fp_mflops", "MFLOPS", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("little_fp_mflops"), **rs("per_cluster")),
        metric_row("cpu", "Big/Little int ratio", "cpu.int_big_over_little", "x", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("int_big_over_little"), **rs("per_cluster")),
        metric_row("cpu", "Big/Little fp ratio", "cpu.fp_big_over_little", "x", "higher",
                   (b.get("per_cluster") or {}).get("summary", {}).get("fp_big_over_little"), **rs("per_cluster")),
        # Memory hierarchy curve
        metric_row("memory", "L1d latency", "memory.l1_ns", "ns", "lower",
                   (b.get("latency_curve") or {}).get("summary", {}).get("l1_ns"), **rs("latency_curve")),
        metric_row("memory", "L2 latency", "memory.l2_ns", "ns", "lower",
                   (b.get("latency_curve") or {}).get("summary", {}).get("l2_ns"), **rs("latency_curve")),
        metric_row("memory", "L3 latency", "memory.l3_ns", "ns", "lower",
                   (b.get("latency_curve") or {}).get("summary", {}).get("l3_ns"), **rs("latency_curve")),
        metric_row("memory", "DRAM latency", "memory.dram_ns", "ns", "lower",
                   (b.get("latency_curve") or {}).get("summary", {}).get("dram_ns"), **rs("latency_curve")),
        # Native memory — the ONLY trustworthy DRAM metrics
        metric_row("memory", "Native STREAM copy", "native.memory_copy_mib_s", "MiB/s", "higher",
                   b.get("native", {}).get("summary", {}).get("memory_copy_mib_s"), **rs("native")),
        metric_row("memory", "Native STREAM scale", "native.memory_scale_mib_s", "MiB/s", "higher",
                   b.get("native", {}).get("summary", {}).get("memory_scale_mib_s"), **rs("native")),
        metric_row("memory", "Native STREAM add", "native.memory_add_mib_s", "MiB/s", "higher",
                   b.get("native", {}).get("summary", {}).get("memory_add_mib_s"), **rs("native")),
        metric_row("memory", "Native STREAM triad", "native.memory_triad_mib_s", "MiB/s", "higher",
                   b.get("native", {}).get("summary", {}).get("memory_triad_mib_s"), **rs("native")),
        metric_row("memory", "Native memory latency", "native.memory_latency_ns", "ns", "lower",
                   b.get("native", {}).get("summary", {}).get("memory_latency_ns"), **rs("native")),
        # Shell CPU hash / memory kernel loop — demoted to `crypto` / `baseline` advisory categories
        metric_row("crypto", "HW crypto single", "crypto.hash_single_mib_s", "MiB/s", "higher",
                   b.get("cpu", {}).get("crypto_single_mib_s"), **rs("cpu")),
        metric_row("crypto", "HW crypto best multi", "crypto.hash_best_multi_mib_s", "MiB/s", "higher",
                   b.get("cpu", {}).get("crypto_best_multi_mib_s"), **rs("cpu")),
        metric_row("baseline", "Kernel syscall loop", "baseline.kernel_loop_mib_s", "MiB/s", "higher",
                   b.get("memory", {}).get("kernel_loop_mib_s"), **rs("memory")),
        # Storage — sequential write is real; buffered_read and random are advisory
        metric_row("storage", "Storage sequential write", "storage.seq_write_mib_s", "MiB/s", "higher",
                   b.get("storage", {}).get("sequential_write_mib_s"), **rs("storage")),
        metric_row("storage", "Storage buffered read (advisory)", "storage.buffered_read_mib_s", "MiB/s", "higher",
                   b.get("storage", {}).get("buffered_read_mib_s"),
                   **rs("storage"), advisory="buffered_read: page cache dominated"),
        metric_row("storage", "Storage random read IOPS (advisory)", "storage.random_read_iops", "IOPS", "higher",
                   b.get("storage", {}).get("random_read_iops_advisory"),
                   **rs("storage"), advisory="shell-fork bound"),
        metric_row("storage", "Storage random write IOPS (advisory)", "storage.random_write_iops", "IOPS", "higher",
                   b.get("storage", {}).get("random_write_iops_advisory"),
                   **rs("storage"), advisory="shell-fork bound"),
        # Thermal — all advisory due to HW-accelerated stressor
        metric_row("thermal", "Thermal max temp", "thermal.max_temp_c", "C", "lower",
                   b.get("thermal", {}).get("max_temp_c"), **rs("thermal")),
        metric_row("thermal", "Thermal delta", "thermal.delta_temp_c", "C", "lower",
                   b.get("thermal", {}).get("delta_temp_c"), **rs("thermal")),
        metric_row("thermal", "Min freq ratio under load", "thermal.min_freq_ratio", "ratio", "higher",
                   b.get("thermal", {}).get("min_freq_ratio"), **rs("thermal")),
        # Battery — fixed direction (drain_pct lower-is-better)
        metric_row("battery", "Battery drain over sample window", "battery.drain_pct", "pct", "lower",
                   b.get("battery", {}).get("drain_pct"), **rs("battery")),
        metric_row("battery", "Avg power (|I|*|V|)", "battery.avg_power_mw", "mW", "lower",
                   b.get("battery", {}).get("avg_power_mw_estimate"), **rs("battery")),
        # Network
        metric_row("network", "Ping average", "network.ping_avg_ms", "ms", "lower",
                   b.get("network", {}).get("ping", {}).get("avg_ms"), **rs("network")),
        metric_row("network", "Ping jitter", "network.ping_jitter_ms", "ms", "lower",
                   b.get("network", {}).get("ping", {}).get("jitter_ms"), **rs("network")),
        metric_row("network", "Ping packet loss", "network.ping_loss_pct", "pct", "lower",
                   b.get("network", {}).get("ping", {}).get("packet_loss_pct"), **rs("network")),
        # Interaction — advisory (onDraw != visible)
        metric_row("interaction", "Tap dispatch latency", "interaction.dispatch_median_ms", "ms", "lower",
                   b.get("interaction", {}).get("summary", {}).get("dispatch_latency_ms", {}).get("median"),
                   **rs("interaction")),
        metric_row("interaction", "Tap to onDraw (advisory)", "interaction.on_draw_median_ms", "ms", "lower",
                   b.get("interaction", {}).get("summary", {}).get("on_draw_median_ms"),
                   **rs("interaction"), advisory="onDraw is draw-command-start, not visible"),
        metric_row("interaction", "Tap to frame callback (advisory)", "interaction.frame_median_ms", "ms", "lower",
                   b.get("interaction", {}).get("summary", {}).get("frame_median_ms"),
                   **rs("interaction"), advisory="FrameCallback fires before sync/draw"),
        # Launch — overall + per-app (am force-stop = real-world cold launch)
        metric_row("launch", "Best launch avg", "launch.best_avg_ms", "ms", "lower",
                   b.get("launch", {}).get("summary", {}).get("best_avg_ms"),
                   **rs("launch")),
    ]
    # Append per-app launch rows (each app comparable apples-to-apples across devices)
    launch_rows = ((b.get("launch") or {}).get("summary") or {}).get("rows") or []
    for app_row in launch_rows:
        app = app_row.get("app")
        flavor = app_row.get("flavor") or "default"
        avg = app_row.get("avg_ms")
        if avg is None:
            continue
        rows.append(metric_row(
            "launch", f"启动 {app} ({flavor})", f"launch.{app}.{flavor}_avg_ms",
            "ms", "lower", avg, **rs("launch")
        ))
    rows += [
        # Scroll
        metric_row("scroll", "Scroll avg FPS", "scroll.avg_fps", "fps", "higher",
                   b.get("scroll", {}).get("summary", {}).get("avg_fps"), **rs("scroll")),
        metric_row("scroll", "Scroll P95 frame", "scroll.p95_frame_ms", "ms", "lower",
                   b.get("scroll", {}).get("summary", {}).get("p95_frame_ms"), **rs("scroll")),
        metric_row("scroll", "Scroll P99 frame", "scroll.p99_frame_ms", "ms", "lower",
                   b.get("scroll", {}).get("summary", {}).get("p99_frame_ms"), **rs("scroll")),
        metric_row("scroll", "Scroll avg jank", "scroll.avg_jank_pct", "pct", "lower",
                   b.get("scroll", {}).get("summary", {}).get("avg_jank_pct"), **rs("scroll")),
        # Install
        metric_row("install", "APK install speed", "install.duration_sec", "sec", "lower",
                   b.get("install", {}).get("duration_sec"), **rs("install")),
        # Geekbench-style mix → counted under cpu category but separate metrics
        metric_row("cpu", "AES round throughput", "gb.aes_round_mib_s", "MiB/s", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("aes_round_mib_s"), **rs("geekbench_mix")),
        metric_row("cpu", "SHA-256 throughput", "gb.sha256_mib_s", "MiB/s", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("sha256_mib_s"), **rs("geekbench_mix")),
        metric_row("cpu", "FFT MFLOPS", "gb.fft_mflops", "MFLOPS", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("fft_mflops"), **rs("geekbench_mix")),
        metric_row("cpu", "Matrix mult MFLOPS", "gb.matmul_mflops", "MFLOPS", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("matmul_mflops"), **rs("geekbench_mix")),
        metric_row("cpu", "N-body MFLOPS", "gb.nbody_mflops", "MFLOPS", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("nbody_mflops"), **rs("geekbench_mix")),
        metric_row("cpu", "Sort throughput", "gb.sort_mops", "Mops/s", "higher",
                   (b.get("geekbench_mix", {}).get("summary") or {}).get("sort_mops"), **rs("geekbench_mix")),
        # AnTuTu UX — Data
        metric_row("ux_data", "Regex scan MiB/s", "ux.regex_mib_per_s", "MiB/s", "higher",
                   (b.get("ux_data", {}).get("summary") or {}).get("regex_mib_per_s"), **rs("ux_data")),
        metric_row("ux_data", "JSON tokenize MiB/s", "ux.json_tokenize_mib_per_s", "MiB/s", "higher",
                   (b.get("ux_data", {}).get("summary") or {}).get("json_tokenize_mib_per_s"), **rs("ux_data")),
        metric_row("ux_data", "Data sort Mops/s", "ux.data_sort_mops", "Mops/s", "higher",
                   (b.get("ux_data", {}).get("summary") or {}).get("data_sort_mops"), **rs("ux_data")),
        # AnTuTu UX — Image
        metric_row("ux_image", "Image processing Mpix/s", "ux.image_mpix_out_per_s", "Mpix/s", "higher",
                   (b.get("ux_image", {}).get("summary") or {}).get("image_mpix_out_per_s"), **rs("ux_image")),
        # True random IOPS (active when O_DIRECT or fsync per op succeeded)
        metric_row("storage", "Random read IOPS (native)", "storage.random_read_iops_native", "IOPS", "higher",
                   (b.get("storage_random_native", {}).get("summary") or {}).get("random_read_iops"),
                   **rs("storage_random_native")),
        metric_row("storage", "Random write IOPS (native)", "storage.random_write_iops_native", "IOPS", "higher",
                   (b.get("storage_random_native", {}).get("summary") or {}).get("random_write_iops"),
                   **rs("storage_random_native")),
        # Sustained performance curve
        metric_row("sustained", "Stability ratio", "sustained.stability_ratio", "ratio", "higher",
                   (b.get("sustained", {}).get("summary") or {}).get("stability_ratio"), **rs("sustained")),
        metric_row("sustained", "Drift %", "sustained.drift_pct", "%", "lower",
                   (b.get("sustained", {}).get("summary") or {}).get("drift_pct"), **rs("sustained")),
        # Memory pressure / LMK
        metric_row("memory_pressure", "Max resident before LMK", "memory_pressure.max_resident_mb", "MiB", "higher",
                   b.get("memory_pressure", {}).get("max_resident_mb"), **rs("memory_pressure")),
        metric_row("memory_pressure", "RAM headroom %", "memory_pressure.headroom_pct", "%", "higher",
                   b.get("memory_pressure", {}).get("headroom_pct"), **rs("memory_pressure")),
        metric_row("memory_pressure", "lmkd kills during", "memory_pressure.lmkd_kills_during", "kills", "lower",
                   b.get("memory_pressure", {}).get("lmkd_kills_during"), **rs("memory_pressure")),
        # Multitask
        metric_row("memory_pressure", "Multitask: revisit/cold ratio", "multitask.revisit_to_cold_ratio", "x", "lower",
                   (b.get("multitask") or {}).get("revisit_to_cold_ratio"), **rs("multitask")),
        metric_row("memory_pressure", "Multitask apps held", "multitask.apps_in_chain", "count", "higher",
                   (b.get("multitask") or {}).get("apps_in_chain"), **rs("multitask")),
        # GPU
        metric_row("gpu", "glmark2 score", "gpu.glmark2_score", "score", "higher",
                   b.get("gpu", {}).get("glmark2_score"), **rs("gpu")),
        # AI / NPU
        metric_row("ai", "Best inference latency", "ai.best_inference_avg_us", "µs", "lower",
                   (b.get("ai", {}).get("summary") or {}).get("best_inference_avg_us"), **rs("ai")),
        metric_row("ai", "AI accelerator speedup", "ai.accel_speedup", "x", "higher",
                   (b.get("ai", {}).get("summary") or {}).get("accel_speedup"), **rs("ai")),
        # Video probe (advisory)
        metric_row("video", "Screenrecord encoder MB/s", "video.encoder_mb_per_s", "MB/s", "higher",
                   (b.get("video", {}).get("screenrecord") or {}).get("estimated_mb_per_sec"), **rs("video")),
    ]
    return [row for row in rows if row["value"] is not None]


def metric_row(
    category: str,
    name: str,
    key: str,
    unit: str,
    direction: str,
    value: Any,
    stage_status: str = "unknown",
    stage_advisory_reason: Optional[str] = None,
    advisory: Optional[str] = None,
) -> Dict[str, Any]:
    parsed = parse_float(value)
    is_metric_advisory = advisory is not None or "advisory" in (stage_status or "").lower()
    status = "advisory" if is_metric_advisory else "active"
    reasons: List[str] = []
    if advisory:
        reasons.append(advisory)
    if stage_advisory_reason:
        reasons.append(stage_advisory_reason)
    # A metric is only safe to score when (1) the stage actually succeeded — failed/skipped/unknown
    # stages may still emit partial values that shouldn't influence a normalized comparison —
    # and (2) the metric itself isn't advisory.
    valid_for_score = (
        status == "active"
        and stage_status == "success"
        and parsed is not None
    )
    return {
        "category": category,
        "name": name,
        "key": key,
        "unit": unit,
        "direction": direction,
        "value": parsed,
        "status": status,
        "stage_status": stage_status,
        "advisory_reason": "; ".join(reasons) if reasons else None,
        "valid_for_score": valid_for_score,
    }


def _safe_delta_pct(best: Optional[float], worst: Optional[float], direction: str) -> Optional[float]:
    if best is None or worst is None:
        return None
    # Prefer |worst| as the denom (larger), fall back to |best| when worst is 0 to avoid div-by-zero.
    denom = worst if worst not in (0, 0.0) else best
    if denom in (0, 0.0):
        return None
    numerator = (worst - best) if direction == "lower" else (best - worst)
    return numerator / abs(denom) * 100.0


def compare_devices(device_results: Sequence[Dict[str, Any]]) -> List[Dict[str, Any]]:
    by_device: Dict[str, Dict[str, Dict[str, Any]]] = {}
    meta_by_key: Dict[str, Dict[str, Any]] = {}
    for device in device_results:
        label = device["device_label"]
        by_device[label] = {}
        for row in device.get("metrics", []):
            by_device[label][row["key"]] = row
            meta_by_key[row["key"]] = row

    comparisons: List[Dict[str, Any]] = []
    labels = [device["device_label"] for device in device_results]
    for key, meta in sorted(meta_by_key.items()):
        values: Dict[str, Optional[float]] = {}
        for label in labels:
            row = by_device.get(label, {}).get(key)
            values[label] = parse_float(row.get("value")) if row else None
        present = {label: value for label, value in values.items() if value is not None}
        if not present:
            continue
        if meta["direction"] == "lower":
            winner = min(present, key=lambda x: present[x] if present[x] is not None else float("inf"))
        else:
            winner = max(present, key=lambda x: present[x] if present[x] is not None else float("-inf"))
        best = present[winner]
        worst = max(present.values()) if meta["direction"] == "lower" else min(present.values())
        delta_pct = _safe_delta_pct(best, worst, meta["direction"])
        winner_display = winner if len(present) > 1 else ""
        row = {
            "category": meta["category"],
            "metric": meta["name"],
            "key": key,
            "unit": meta["unit"],
            "direction": meta["direction"],
            "status": meta.get("status", "active"),
            "advisory_reason": meta.get("advisory_reason"),
            "winner": winner_display,
            "winner_delta_pct": round(delta_pct, 2) if delta_pct is not None and winner_display else "",
        }
        for label in labels:
            row[label] = values[label] if values[label] is not None else ""
        comparisons.append(row)
    return comparisons


SCORE_WEIGHTS = {
    # AnTuTu-style buckets, biased toward UI perf since this project's raison d'être is UI.
    # CPU bucket (native int/fp + Geekbench-style mix)
    "cpu": 22.0,
    # GPU
    "gpu": 12.0,
    # Memory bucket (native STREAM + memory pressure)
    "memory": 10.0,
    "memory_pressure": 4.0,
    # Storage (native true IOPS subsumes shell IOPS in score; sequential write still active)
    "storage": 8.0,
    # AnTuTu UX: data + image (responsiveness lives in interaction/scroll/launch below)
    "ux_data": 5.0,
    "ux_image": 5.0,
    # UI perf — the headline of this project
    "scroll": 13.0,
    "launch": 8.0,
    "interaction": 6.0,
    # Sustained throttling
    "sustained": 4.0,
    # AI / NPU
    "ai": 6.0,
    # Thermal advisory (low weight, advisory metrics not in score anyway)
    "thermal": 3.0,
    # Battery / network / install — small but present
    "battery": 2.0,
    "network": 1.0,
    "install": 1.0,
    # video is advisory-only; not in scoring (advisory metric_rows are skipped)
    # "crypto" and "baseline" are advisory-only and excluded from scoring.
}


def compute_scores(device_results: Sequence[Dict[str, Any]]) -> Dict[str, Any]:
    labels = [device["device_label"] for device in device_results]
    if len(labels) <= 1:
        # Single-device runs cannot produce a meaningful relative score.
        # Emit a stable per-device shape so downstream consumers don't have to special-case this.
        return {
            label: {
                "total": None,
                "categories": {},
                "absolute_mode": False,
                "note": "single-device run: cross-device normalization not meaningful; see metrics table.",
            }
            for label in labels
        }

    metric_values: Dict[str, Dict[str, Any]] = {}
    for device in device_results:
        for row in device.get("metrics", []):
            if not row.get("valid_for_score", False):
                continue
            metric_values.setdefault(row["key"], {"meta": row, "values": {}})
            metric_values[row["key"]]["values"][device["device_label"]] = row["value"]

    category_scores: Dict[str, Dict[str, List[float]]] = {label: {} for label in labels}
    for key, payload in metric_values.items():
        meta = payload["meta"]
        if meta["category"] not in SCORE_WEIGHTS:
            continue
        values = {label: parse_float(value) for label, value in payload["values"].items()}
        present = {label: value for label, value in values.items() if value is not None}
        if len(present) < 2:
            continue
        if meta["direction"] == "lower":
            best = min(v for v in present.values() if v is not None)
            normalized = {
                label: max(0.0, min(100.0, (best / value) * 100.0)) if value and value > 0 else 0.0
                for label, value in present.items()
            }
        else:
            best = max(v for v in present.values() if v is not None)
            normalized = {
                label: max(0.0, min(100.0, (value / best) * 100.0)) if best and best > 0 else 0.0
                for label, value in present.items()
            }
        for label, score in normalized.items():
            category_scores[label].setdefault(meta["category"], []).append(score)

    full_weight_total = sum(SCORE_WEIGHTS.values())
    result: Dict[str, Any] = {}
    for label in labels:
        categories: Dict[str, float] = {}
        weighted_total = 0.0
        for category, weight in SCORE_WEIGHTS.items():
            scores = category_scores[label].get(category)
            if scores:
                cat_score = sum(scores) / len(scores)
                categories[category] = round(cat_score, 2)
                weighted_total += cat_score * weight
            else:
                categories[category] = None  # absent category gets 0 contribution but is flagged
        result[label] = {
            "total": round(weighted_total / full_weight_total, 2) if full_weight_total else None,
            "categories": categories,
            "absolute_mode": False,
            "weight_denominator": full_weight_total,
            "note": "normalized per-metric; best device = 100 in that metric.",
        }
    return result


def write_hardware_baseline_csv(run_dir: Path, device_results: Sequence[Dict[str, Any]]) -> None:
    rows: List[Dict[str, Any]] = []
    keys = [
        "model",
        "manufacturer",
        "brand",
        "device",
        "android_release",
        "sdk",
        "soc_model",
        "abi",
        "cpu_count",
        "memory_total_mb",
        "memory_available_mb",
        "screen_size",
        "density_dpi",
        "peak_refresh_rate",
        "min_refresh_rate",
        "gpu_renderer",
        "selinux_mode",
        "foreground_package",
        "build_fingerprint",
    ]
    nested_fields = [
        ("hwui", "hwui_renderer"),
        ("hwui", "hwui_use_vulkan"),
        ("hwui", "skia_pipeline"),
        ("storage", "data_fs"),
        ("gpu_freq", "gpu_cur_freq_raw"),
        ("gpu_freq", "gpu_governor"),
        ("wifi_link", "rssi_dbm"),
        ("wifi_link", "link_mbps"),
    ]
    for device in device_results:
        info = device.get("device_info", {})
        row = {"device_label": device["device_label"], "serial": info.get("serial")}
        for key in keys:
            row[key] = info.get(key)
        for parent, child in nested_fields:
            container = info.get(parent) or {}
            if isinstance(container, dict):
                if child == "data_fs" and parent == "storage":
                    row["data_fs"] = (container.get("data_fs") or {}).get("fstype")
                else:
                    row[f"{parent}.{child}"] = container.get(child)
        scales = info.get("animation_scales") or {}
        for scale_key in ANIMATION_SCALE_KEYS:
            row[f"anim.{scale_key}"] = scales.get(scale_key)
        mm = info.get("mm", {}) or {}
        zram = mm.get("zram") or {}
        row["zram_enabled"] = zram.get("enabled")
        row["zram_comp_algorithm"] = zram.get("comp_algorithm")
        row["mglru_enabled"] = mm.get("mglru_enabled")
        rows.append(row)
    write_csv(run_dir / "details" / "hardware_baseline.csv", rows)


def write_summary_metrics_csv(device_dir: Path, metrics: Sequence[Dict[str, Any]]) -> None:
    write_csv(device_dir / "details" / "summary_metrics.csv", list(metrics))


def pct_diff(value: Optional[float], reference: Optional[float], direction: str = "higher") -> Optional[float]:
    """Return percentage delta of `value` vs `reference` interpreted in `direction`.

    For direction='higher': positive means `value` is `direction`-better than reference.
    For direction='lower': positive means `value` is lower than reference (better).
    """
    if value is None or reference is None or reference in (0, 0.0):
        return None
    if direction == "lower":
        return round((reference - value) / abs(reference) * 100.0, 2)
    return round((value - reference) / abs(reference) * 100.0, 2)


def format_ascii_bar(value: Optional[float], peak: Optional[float], width: int = 20) -> str:
    if value is None or peak is None or peak <= 0:
        return " " * width
    fill = max(0, min(width, int(round(value / peak * width))))
    return "█" * fill + "·" * (width - fill)


_DEVICE_INFO_KEYS_OF_INTEREST = (
    "model", "manufacturer", "brand", "soc_model", "abi", "android_release", "sdk",
    "cpu_count", "memory_total_mb", "screen_size", "density_dpi",
    "peak_refresh_rate", "min_refresh_rate", "selinux_mode",
    "foreground_package", "build_fingerprint",
)


def _flatten_for_diff(device_result: Dict[str, Any]) -> Dict[str, Any]:
    info = device_result.get("device_info", {}) or {}
    env_applied = (device_result.get("env", {}) or {}).get("applied", {}) or {}
    battery_gate = device_result.get("battery_gate", {}) or {}
    flat: Dict[str, Any] = {}
    for key in _DEVICE_INFO_KEYS_OF_INTEREST:
        flat[f"device.{key}"] = info.get(key)
    hwui = info.get("hwui") or {}
    for k, v in hwui.items():
        flat[f"hwui.{k}"] = v
    storage = (info.get("storage") or {}).get("data_fs") or {}
    for k in ("fstype", "noatime", "discard"):
        flat[f"data_fs.{k}"] = storage.get(k)
    mm = info.get("mm") or {}
    zram = mm.get("zram") or {}
    flat["mm.zram_enabled"] = zram.get("enabled")
    flat["mm.zram_comp_algorithm"] = zram.get("comp_algorithm")
    flat["mm.mglru_enabled"] = mm.get("mglru_enabled")
    vm_tuning = mm.get("vm_tuning") or {}
    for k in ("swappiness", "dirty_ratio", "dirty_background_ratio"):
        flat[f"vm.{k}"] = vm_tuning.get(k)
    gpu_freq = info.get("gpu_freq") or {}
    flat["gpu.governor"] = gpu_freq.get("gpu_governor")
    wifi = info.get("wifi_link") or {}
    flat["wifi.rssi_dbm"] = wifi.get("rssi_dbm")
    flat["wifi.link_mbps"] = wifi.get("link_mbps")
    for k, v in env_applied.items():
        flat[f"env.{k}"] = v
    flat["battery.charging"] = battery_gate.get("charging")
    flat["battery.level_pct"] = battery_gate.get("level_pct")
    return flat


def compute_config_diffs(device_results: Sequence[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Return rows for every key whose value differs across devices.

    Each row is `{field, <label1>, <label2>, ...}` so the table prints cleanly.
    Same-everywhere keys are dropped.
    """
    if len(device_results) < 2:
        return []
    flats = {dr["device_label"]: _flatten_for_diff(dr) for dr in device_results}
    all_keys: List[str] = []
    seen = set()
    for flat in flats.values():
        for k in flat:
            if k not in seen:
                seen.add(k)
                all_keys.append(k)
    rows: List[Dict[str, Any]] = []
    for key in all_keys:
        values = {label: flat.get(key) for label, flat in flats.items()}
        unique = {repr(v) for v in values.values()}
        if len(unique) <= 1:
            continue
        row: Dict[str, Any] = {"field": key}
        for label, value in values.items():
            row[label] = "" if value is None else value
        rows.append(row)
    return rows


def compute_category_aggregates(device_results: Sequence[Dict[str, Any]]) -> Dict[str, Dict[str, Any]]:
    """For each category, compute per-device aggregate (geomean of normalized active metrics).

    Returns:
      {category: {
          'metrics': [metric_key, ...],
          'devices': {label: {'normalized': float (0..100), 'absolute_geomean': float, 'count': int}},
       }}
    """
    labels = [d["device_label"] for d in device_results]
    by_metric: Dict[str, Dict[str, Any]] = {}
    for d in device_results:
        for row in d.get("metrics", []):
            if not row.get("valid_for_score", False):
                continue
            cat = row["category"]
            if cat not in SCORE_WEIGHTS:
                continue
            entry = by_metric.setdefault(row["key"], {"meta": row, "values": {}, "category": cat})
            entry["values"][d["device_label"]] = parse_float(row["value"])

    cat_to_metrics: Dict[str, List[str]] = {}
    for key, payload in by_metric.items():
        cat_to_metrics.setdefault(payload["category"], []).append(key)

    result: Dict[str, Dict[str, Any]] = {}
    for cat, keys in cat_to_metrics.items():
        per_device: Dict[str, Dict[str, Any]] = {label: {"normalized_pcts": [], "ratios": [], "count": 0}
                                                  for label in labels}
        for key in keys:
            payload = by_metric[key]
            meta = payload["meta"]
            values = payload["values"]
            present = {l: v for l, v in values.items() if v is not None and v > 0}
            if len(present) < 2:
                continue
            if meta["direction"] == "lower":
                best = min(present.values())
                for label, value in present.items():
                    per_device[label]["ratios"].append(best / value)
                    per_device[label]["normalized_pcts"].append((best / value) * 100.0)
            else:
                best = max(present.values())
                for label, value in present.items():
                    per_device[label]["ratios"].append(value / best)
                    per_device[label]["normalized_pcts"].append((value / best) * 100.0)
            for label in present:
                per_device[label]["count"] += 1
        cat_summary: Dict[str, Any] = {"metrics": sorted(keys), "devices": {}}
        for label, agg in per_device.items():
            ratios = agg["ratios"]
            if ratios:
                geomean_ratio = 1.0
                for r in ratios:
                    geomean_ratio *= r
                geomean_ratio = geomean_ratio ** (1.0 / len(ratios))
                cat_summary["devices"][label] = {
                    "normalized": round(geomean_ratio * 100.0, 2),
                    "metrics_counted": agg["count"],
                }
            else:
                cat_summary["devices"][label] = {"normalized": None, "metrics_counted": 0}
        result[cat] = cat_summary
    return result


def summarize_run_health(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Extract per-device health context for the Run Health block."""
    rows: List[Dict[str, Any]] = []
    for device in payload.get("devices", []):
        info = device.get("device_info", {}) or {}
        env = (device.get("env", {}) or {}).get("applied") or {}
        benchmarks = device.get("benchmarks", {}) or {}
        failed = sorted(k for k, v in benchmarks.items() if (v or {}).get("status") == "failed")
        skipped = sorted(k for k, v in benchmarks.items() if (v or {}).get("status") == "skipped")
        advisory = sorted(k for k, v in benchmarks.items() if (v or {}).get("status") == "advisory")
        battery = info.get("battery", {}) or {}
        charging = bool(
            battery.get("ac_powered")
            or battery.get("usb_powered")
            or battery.get("wireless_powered")
        )
        rows.append(
            {
                "device": device.get("device_label"),
                "charging_at_start": "yes" if charging else "no",
                "battery_level_pct": battery.get("level_pct"),
                "foreground_at_start": info.get("foreground_package"),
                "animation_scale_applied": env.get("animation_scale"),
                "stayon_applied": env.get("stay_on_while_plugged_in"),
                "failed_stages": ",".join(failed) if failed else "",
                "skipped_stages": ",".join(skipped) if skipped else "",
                "advisory_stages": ",".join(advisory) if advisory else "",
                "selinux_mode": info.get("selinux_mode"),
                "hwui_renderer": (info.get("hwui") or {}).get("hwui_renderer"),
                "data_fs": ((info.get("storage") or {}).get("data_fs") or {}).get("fstype"),
            }
        )
    return rows


def generate_markdown_report(run_dir: Path, payload: Dict[str, Any]) -> Path:
    lines: List[str] = []
    lines.append("# Android Comprehensive Benchmark Report")
    lines.append("")
    lines.append(f"- Started: `{payload.get('started_at')}`")
    lines.append(f"- Finished: `{payload.get('finished_at')}`")
    lines.append(f"- Devices: `{len(payload.get('devices', []))}`")
    lines.append("")
    lines.append("## Run Health")
    lines.append("")
    health_rows = summarize_run_health(payload)
    if health_rows:
        headers = list(health_rows[0].keys())
        lines.extend(markdown_table(headers, [[row[h] for h in headers] for row in health_rows]))
    else:
        lines.append("_no devices completed_")
    lines.append("")
    failures = payload.get("failures") or []
    if failures:
        lines.append("### Device-level Failures")
        for msg in failures:
            lines.append(f"- `{msg}`")
        lines.append("")
    lines.append("## Measurement Caveats")
    lines.append("")
    lines.append("- `cpu.*` native int/fp = real CPU; `crypto.*` is HW-accelerated sha256, not CPU compute.")
    lines.append("- `memory.*` native STREAM = real DRAM bandwidth; `baseline.kernel_loop_mib_s` is a syscall loop.")
    lines.append("- `storage.buffered_read_mib_s` reflects the page cache, not raw storage.")
    lines.append("- `storage.random_*_iops` is shell-fork bound; treat as ordering only, not absolute IOPS.")
    lines.append("- `interaction.on_draw_median_ms` is draw-command-start, not visible-to-user.")
    lines.append("- `launch.*` is semi-cold on non-root devices (no drop_caches / compile reset).")
    lines.append("- `thermal.*` stressor is HW-accelerated; may under-saturate big cluster.")
    lines.append("")
    lines.append("## Scores")
    lines.append("")
    score_rows = []
    for label, score in payload.get("scores", {}).items():
        score_rows.append([label, score.get("total"), json.dumps(score.get("categories", {}), ensure_ascii=False)])
    lines.extend(markdown_table(["Device", "Total", "Categories"], score_rows))
    lines.append("")
    lines.append("## Comparison")
    lines.append("")
    comp_rows = []
    labels = [device["device_label"] for device in payload.get("devices", [])]
    for row in payload.get("comparisons", []):
        comp_rows.append(
            [
                row.get("category"),
                row.get("metric"),
                row.get("unit"),
                row.get("winner"),
                row.get("winner_delta_pct"),
            ]
            + [row.get(label, "") for label in labels]
        )
    lines.extend(markdown_table(["Category", "Metric", "Unit", "Winner", "Delta %"] + labels, comp_rows))
    lines.append("")
    lines.append("## Device Outputs")
    lines.append("")
    for device in payload.get("devices", []):
        lines.append(f"### {device['device_label']}")
        lines.append("")
        lines.append(f"- Device dir: `{device.get('device_dir')}`")
        lines.append(f"- Raw dumps: `{device.get('device_dir')}/raw`")
        lines.append(f"- getprop CSV: `{device.get('device_dir')}/details/getprop.csv`")
        lines.append(f"- /proc index CSV: `{device.get('device_dir')}/details/proc_index.csv`")
        for name in ("system_trace", "interaction", "launch", "scroll"):
            stage = device.get("benchmarks", {}).get(name, {})
            if stage:
                lines.append(f"- {name} status: `{stage.get('status')}`")
                for key in ("json", "markdown", "summary_json", "final_json", "log_file", "csv", "trace_path", "trace_dir", "log_dir"):
                    if stage.get(key):
                        lines.append(f"- {name} {key}: `{stage.get(key)}`")
        lines.append("")
        metric_rows = [
            [m["category"], m["name"], m["value"], m["unit"], m["direction"]]
            for m in device.get("metrics", [])
        ]
        lines.extend(markdown_table(["Category", "Metric", "Value", "Unit", "Direction"], metric_rows))
        lines.append("")
    lines.append("## Coverage")
    lines.append("")
    lines.extend(
        markdown_table(
            ["Dimension", "Status", "Method"],
            [[row["dimension"], row["status"], row["method"]] for row in payload.get("coverage", [])],
        )
    )
    path = run_dir / "report.md"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def markdown_table(headers: Sequence[Any], rows: Sequence[Sequence[Any]]) -> List[str]:
    out = ["| " + " | ".join(str(x) for x in headers) + " |"]
    out.append("| " + " | ".join("---" for _ in headers) + " |")
    for row in rows:
        out.append("| " + " | ".join("" if x is None else str(x) for x in row) + " |")
    return out


def _html_category_comparison(payload: Dict[str, Any]) -> str:
    devices = payload.get("devices", []) or []
    if len(devices) < 2:
        return ""
    aggs = compute_category_aggregates(devices)
    if not aggs:
        return ""
    palette = ["#0f766e", "#7c3aed", "#dc2626", "#0891b2", "#ca8a04", "#65a30d"]
    label_to_color = {d["device_label"]: palette[i % len(palette)] for i, d in enumerate(devices)}
    sections: List[str] = ["<h2>Per-category comparison</h2>"]
    sections.append("<div class='cat-grid'>")
    for cat in sorted(aggs.keys(), key=lambda k: -SCORE_WEIGHTS.get(k, 0)):
        data = aggs[cat]
        peak = max((info.get("normalized") or 0) for info in data["devices"].values()) or 1
        weight = SCORE_WEIGHTS.get(cat)
        sections.append(f"<div class='cat-card'><h3>{html.escape(cat)} <span class='wt'>weight={weight}</span></h3>")
        for label, info in data["devices"].items():
            v = info.get("normalized")
            v_text = "—" if v is None else f"{v:.1f}"
            width = 0 if v is None else max(0, min(100, v / peak * 100))
            color = label_to_color.get(label, "#0f766e")
            sections.append(
                f"<div class='cat-row'><span class='cat-label'>{html.escape(label)}</span>"
                f"<div class='cat-bar'><i style='width:{width}%;background:{color}'></i></div>"
                f"<b>{v_text}</b></div>"
            )
        sections.append("</div>")
    sections.append("</div>")
    return "".join(sections)


def _html_config_diffs(payload: Dict[str, Any]) -> str:
    devices = payload.get("devices", []) or []
    if len(devices) < 2:
        return ""
    diffs = compute_config_diffs(devices)
    if not diffs:
        return "<h2>Configuration differences</h2><p><em>All inspected device fields agree.</em></p>"
    labels = [d["device_label"] for d in devices]
    rows_html = []
    for row in diffs:
        cells = [f"<td>{html.escape(str(row.get(h, '')))}</td>" for h in ["field"] + labels]
        rows_html.append("<tr>" + "".join(cells) + "</tr>")
    th = "".join(f"<th>{html.escape(h)}</th>" for h in ["field"] + labels)
    return ("<h2>Configuration differences</h2>"
            f"<table><thead><tr>{th}</tr></thead><tbody>{''.join(rows_html)}</tbody></table>")


def generate_html_report(run_dir: Path, payload: Dict[str, Any]) -> Path:
    labels = [device["device_label"] for device in payload.get("devices", [])]
    score_cards = []
    for label, score in payload.get("scores", {}).items():
        total = score.get("total")
        categories = score.get("categories", {})
        bar_rows: List[str] = []
        for k, v in sorted(categories.items()):
            if v is None:
                width = 0
                display = "—"
            else:
                width = max(0, min(100, float(v)))
                display = str(v)
            bar_rows.append(
                f"<div class='bar-row'><span>{html.escape(str(k))}</span>"
                f"<div class='bar'><i style='width:{width}%'></i></div>"
                f"<b>{html.escape(display)}</b></div>"
            )
        bars = "\n".join(bar_rows)
        total_display = "—" if total is None else str(total)
        note = score.get("note") or ""
        note_html = f"<div class='meta'>{html.escape(note)}</div>" if note else ""
        score_cards.append(
            f"<section class='card'><h2>{html.escape(label)}</h2>"
            f"<div class='score'>{html.escape(total_display)}</div>{bars}{note_html}</section>"
        )

    comparison_headers = ["Category", "Metric", "Unit", "Winner", "Delta %"] + labels
    comparison_rows = []
    for row in payload.get("comparisons", []):
        comparison_rows.append(
            [
                row.get("category"),
                row.get("metric"),
                row.get("unit"),
                row.get("winner"),
                row.get("winner_delta_pct"),
            ]
            + [row.get(label, "") for label in labels]
        )
    device_sections = []
    for device in payload.get("devices", []):
        metric_rows = [
            [m["category"], m["name"], m["value"], m["unit"], m["direction"]]
            for m in device.get("metrics", [])
        ]
        outputs = []
        outputs.append(f"<li>Raw dumps: <code>{html.escape(str(device.get('device_dir')))}/raw</code></li>")
        outputs.append(f"<li>getprop CSV: <code>{html.escape(str(device.get('device_dir')))}/details/getprop.csv</code></li>")
        outputs.append(f"<li>/proc index CSV: <code>{html.escape(str(device.get('device_dir')))}/details/proc_index.csv</code></li>")
        for stage_name in ("system_trace", "interaction", "launch", "scroll"):
            stage = device.get("benchmarks", {}).get(stage_name, {})
            if not stage:
                continue
            outputs.append(
                f"<li><b>{html.escape(stage_name)}</b>: {html.escape(str(stage.get('status')))} "
                f"<code>{html.escape(str(stage.get('log_file') or ''))}</code></li>"
            )
        device_sections.append(
            f"<section><h2>{html.escape(device['device_label'])}</h2>"
            f"<p><code>{html.escape(str(device.get('device_dir')))}</code></p>"
            f"<ul>{''.join(outputs)}</ul>"
            f"{html_table(['Category', 'Metric', 'Value', 'Unit', 'Direction'], metric_rows)}</section>"
        )

    doc = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Android Comprehensive Benchmark Report</title>
<style>
body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 24px; color: #172026; }}
h1, h2 {{ margin: 0 0 12px; }}
.meta {{ color: #5b6670; margin-bottom: 24px; }}
.cards {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin: 16px 0 28px; }}
.card {{ border: 1px solid #d9dee3; border-radius: 8px; padding: 16px; background: #fff; }}
.score {{ font-size: 40px; font-weight: 700; margin: 6px 0 12px; }}
.bar-row {{ display: grid; grid-template-columns: 90px 1fr 48px; gap: 8px; align-items: center; margin: 8px 0; font-size: 13px; }}
.bar {{ height: 8px; background: #e8edf2; border-radius: 999px; overflow: hidden; }}
.bar i {{ display: block; height: 100%; background: #0f766e; }}
.cat-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 12px; margin-bottom: 28px; }}
.cat-card {{ border: 1px solid #d9dee3; border-radius: 8px; padding: 12px; background: #fff; }}
.cat-card h3 {{ font-size: 14px; margin: 0 0 8px; }}
.wt {{ color: #5b6670; font-size: 11px; font-weight: 400; }}
.cat-row {{ display: grid; grid-template-columns: 220px 1fr 56px; gap: 8px; align-items: center; margin: 4px 0; font-size: 12px; }}
.cat-label {{ overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #172026; }}
.cat-bar {{ height: 8px; background: #e8edf2; border-radius: 999px; overflow: hidden; }}
.cat-bar i {{ display: block; height: 100%; }}
table {{ border-collapse: collapse; width: 100%; margin: 12px 0 28px; font-size: 13px; }}
th, td {{ border: 1px solid #d9dee3; padding: 7px 8px; text-align: left; vertical-align: top; }}
th {{ background: #f4f6f8; }}
code {{ background: #f4f6f8; padding: 1px 4px; border-radius: 4px; }}
section {{ margin-bottom: 28px; }}
</style>
</head>
<body>
<h1>Android Comprehensive Benchmark Report</h1>
<div class="meta">Started: <code>{html.escape(str(payload.get('started_at')))}</code> &nbsp; Finished: <code>{html.escape(str(payload.get('finished_at')))}</code></div>
<div class="cards">{''.join(score_cards)}</div>
<section>{_html_category_comparison(payload)}</section>
<section>{_html_config_diffs(payload)}</section>
<section><h2>Comparison (raw metrics)</h2>{html_table(comparison_headers, comparison_rows)}</section>
{''.join(device_sections)}
<section><h2>Coverage</h2>{html_table(['Dimension', 'Status', 'Method'], [[r['dimension'], r['status'], r['method']] for r in payload.get('coverage', [])])}</section>
</body>
</html>
"""
    path = run_dir / "report.html"
    path.write_text(doc, encoding="utf-8")
    return path


def html_table(headers: Sequence[Any], rows: Sequence[Sequence[Any]]) -> str:
    head = "".join(f"<th>{html.escape(str(h))}</th>" for h in headers)
    body_rows = []
    for row in rows:
        body_rows.append("<tr>" + "".join(f"<td>{html.escape('' if c is None else str(c))}</td>" for c in row) + "</tr>")
    return f"<table><thead><tr>{head}</tr></thead><tbody>{''.join(body_rows)}</tbody></table>"
