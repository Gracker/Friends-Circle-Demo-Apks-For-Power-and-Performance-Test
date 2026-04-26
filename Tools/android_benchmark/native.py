from __future__ import annotations

import json
import os
import shlex
import subprocess
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from .adb import ADB
from .paths import SCRIPT_DIR
from .utils import combined_output, log, parse_float, write_csv


NATIVE_SRC = SCRIPT_DIR / "android_benchmark_native" / "hpfc_native_bench.c"
NATIVE_BUILD_DIR = SCRIPT_DIR / "bin"
NATIVE_BIN_ARM64 = NATIVE_BUILD_DIR / "hpfc_native_bench_arm64"


def find_ndk_clang() -> Optional[Path]:
    candidates: List[Path] = []
    search_roots: List[Path] = []
    for env_var in ("ANDROID_HOME", "ANDROID_SDK_ROOT", "HPFC_ANDROID_SDK"):
        value = os.environ.get(env_var)
        if value:
            search_roots.append(Path(value))
    default_sdk = Path.home() / "Library" / "Android" / "sdk"
    if default_sdk.exists():
        search_roots.append(default_sdk)
    for root in search_roots:
        for host in ("darwin-*", "linux-*"):
            candidates.extend(
                sorted(
                    root.glob(f"ndk/*/toolchains/llvm/prebuilt/{host}/bin/aarch64-linux-android*-clang"),
                    reverse=True,
                )
            )
    for candidate in candidates:
        if candidate.exists() and os.access(candidate, os.X_OK):
            return candidate
    return None


def build_native_benchmark(force: bool = False) -> Dict[str, Any]:
    if not NATIVE_SRC.exists():
        return {"status": "skipped", "reason": f"native source missing: {NATIVE_SRC}"}
    if (
        not force
        and NATIVE_BIN_ARM64.exists()
        and NATIVE_BIN_ARM64.stat().st_mtime >= NATIVE_SRC.stat().st_mtime
    ):
        return {"status": "success", "binary": str(NATIVE_BIN_ARM64), "cached": True}
    clang = find_ndk_clang()
    if not clang:
        return {"status": "skipped", "reason": "Android NDK clang not found"}
    NATIVE_BUILD_DIR.mkdir(parents=True, exist_ok=True)
    cmd = [
        str(clang),
        "-O3",
        "-march=armv8-a+simd",
        "-fPIE",
        "-pie",
        "-pthread",
        "-std=c11",
        str(NATIVE_SRC),
        "-o",
        str(NATIVE_BIN_ARM64),
        "-lm",
    ]
    log("INFO", "Build native benchmark: " + " ".join(shlex.quote(x) for x in cmd))
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if proc.returncode != 0:
        return {"status": "failed", "returncode": proc.returncode, "output": combined_output(proc)}
    return {"status": "success", "binary": str(NATIVE_BIN_ARM64), "cached": False}


def run_native_benchmark(adb: ADB, args: Any, device_dir: Path) -> Dict[str, Any]:
    details = device_dir / "details"
    if adb.dry_run:
        write_csv(details / "native_cpu.csv", [{"status": "dry_run"}])
        write_csv(details / "native_memory.csv", [{"status": "dry_run"}])
        return {"status": "dry_run", "reason": "dry-run does not execute native benchmark"}
    if getattr(args, "skip_native", False):
        return {"status": "skipped", "reason": "--skip-native"}

    build = build_native_benchmark(force=getattr(args, "rebuild_native", False))
    if build.get("status") != "success":
        return build

    remote = f"/data/local/tmp/hpfc_native_bench_{os.getpid()}"
    push = adb.push(NATIVE_BIN_ARM64, remote, timeout=60)
    if push.returncode != 0:
        return {"status": "failed", "reason": "adb push failed", "output": combined_output(push)}
    adb.shell(f"chmod 755 {shlex.quote(remote)}", timeout=20)

    threads = ",".join(str(x) for x in native_thread_counts(args))
    pin_big = 0 if getattr(args, "native_no_pin_big", False) else 1
    sched_fifo = 1 if getattr(args, "native_sched_fifo", False) else 0
    cmd = (
        f"{shlex.quote(remote)} "
        f"--threads {shlex.quote(threads)} "
        f"--memory-mib {int(args.native_memory_mib)} "
        f"--memory-iterations {int(args.native_memory_iterations)} "
        f"--latency-mib {int(args.native_latency_mib)} "
        f"--latency-visits {int(args.native_latency_visits)} "
        f"--int-iterations {int(args.native_int_iterations)} "
        f"--fp-iterations {int(args.native_fp_iterations)} "
        f"--pin-big {pin_big} "
        f"--sched-fifo {sched_fifo}"
    )
    rows_cpu: List[Dict[str, Any]] = []
    rows_memory: List[Dict[str, Any]] = []
    payloads: List[Dict[str, Any]] = []
    try:
        for iteration in range(1, int(args.benchmark_iterations) + 1):
            log("INFO", f"Native benchmark iteration {iteration}/{args.benchmark_iterations}")
            start = time.monotonic()
            proc = adb.shell(cmd, timeout=max(60, int(args.native_timeout_sec)))
            elapsed = time.monotonic() - start
            out = combined_output(proc)
            row_base = {
                "iteration": iteration,
                "status": "success" if proc.returncode == 0 else "failed",
                "wall_sec": round(elapsed, 4),
            }
            if proc.returncode != 0:
                rows_cpu.append({**row_base, "test": "native", "output": out[:1000]})
                continue
            try:
                payload = json.loads(out)
            except json.JSONDecodeError:
                rows_cpu.append({**row_base, "test": "native", "status": "failed", "output": out[:1000]})
                continue
            payloads.append(payload)
            for row in payload.get("cpu_int", []):
                rows_cpu.append({**row_base, "test": "cpu_int", **row})
            for row in payload.get("cpu_fp", []):
                rows_cpu.append({**row_base, "test": "cpu_fp", **row})
            memory_stream = payload.get("memory_stream", {})
            if memory_stream:
                rows_memory.append({**row_base, "test": "memory_stream", **memory_stream})
            memory_latency = payload.get("memory_latency", {})
            if memory_latency:
                rows_memory.append({**row_base, "test": "memory_latency", **memory_latency})
    finally:
        if not getattr(args, "keep_device_temp", False):
            adb.shell(f"rm -f {shlex.quote(remote)}", timeout=20)

    write_csv(details / "native_cpu.csv", rows_cpu)
    write_csv(details / "native_memory.csv", rows_memory)
    summary = summarize_native_rows(rows_cpu, rows_memory)
    meta = payloads[-1].get("meta", {}) if payloads else {}
    return {
        "status": "success" if payloads else "failed",
        "build": build,
        "iterations": int(args.benchmark_iterations),
        "threads": native_thread_counts(args),
        "summary": summary,
        "native_meta": meta,
        "cpu_csv": str(details / "native_cpu.csv"),
        "memory_csv": str(details / "native_memory.csv"),
    }


_ARM64_ABIS = ("arm64-v8a", "aarch64")


def detect_device_abi(adb: ADB) -> Optional[str]:
    """Return the device's primary ABI, or None on failure."""
    if getattr(adb, "dry_run", False):
        return None
    out = adb.shell_out("getprop ro.product.cpu.abi", timeout=10).strip()
    return out or None


def push_native_binary(adb: ADB, force_rebuild: bool = False) -> Dict[str, Any]:
    """Build (if needed) and push the native binary to /data/local/tmp.

    Returns {status, binary_remote, build} where binary_remote is the device path.
    Skips early on non-arm64 devices since we only ship an arm64 binary.
    """
    abi = detect_device_abi(adb)
    if abi and not any(token in abi for token in _ARM64_ABIS):
        return {"status": "skipped", "reason": f"device abi {abi!r} is not arm64; only arm64 binary is built", "abi": abi}
    build = build_native_benchmark(force=force_rebuild)
    if build.get("status") != "success":
        return {"status": build.get("status", "failed"), "build": build}
    remote = f"/data/local/tmp/hpfc_native_bench_{os.getpid()}"
    push = adb.push(NATIVE_BIN_ARM64, remote, timeout=60)
    if push.returncode != 0:
        return {"status": "failed", "build": build, "reason": "adb push failed", "output": combined_output(push)}
    adb.shell(f"chmod 755 {shlex.quote(remote)}", timeout=20)
    return {"status": "success", "binary_remote": remote, "build": build, "abi": abi}


def run_native_mode(adb: ADB, remote: str, args_str: str, timeout: int = 300) -> Dict[str, Any]:
    """Run a single mode invocation; return parsed JSON or error dict."""
    cmd = f"{shlex.quote(remote)} {args_str}"
    start = time.monotonic()
    proc = adb.shell(cmd, timeout=timeout)
    elapsed = time.monotonic() - start
    out = combined_output(proc)
    if proc.returncode != 0:
        return {"status": "failed", "wall_sec": round(elapsed, 4), "output": out[:1000]}
    try:
        payload = json.loads(out)
    except json.JSONDecodeError:
        return {"status": "failed", "wall_sec": round(elapsed, 4), "output": out[:1000], "reason": "invalid_json"}
    return {"status": "success", "wall_sec": round(elapsed, 4), "payload": payload}


def run_geekbench_mix(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_geekbench_mix", False):
        return {"status": "skipped", "reason": "--skip-geekbench-mix"}
    cmd = (
        f"--mode geekbench-mix "
        f"--aes-bytes {int(getattr(args, 'gb_aes_bytes', 64*1024*1024))} "
        f"--sha-bytes {int(getattr(args, 'gb_sha_bytes', 64*1024*1024))} "
        f"--fft-log2 {int(getattr(args, 'gb_fft_log2', 14))} "
        f"--fft-repeats {int(getattr(args, 'gb_fft_repeats', 50))} "
        f"--matmul-n {int(getattr(args, 'gb_matmul_n', 256))} "
        f"--matmul-repeats {int(getattr(args, 'gb_matmul_repeats', 5))} "
        f"--nbody-n {int(getattr(args, 'gb_nbody_n', 256))} "
        f"--nbody-steps {int(getattr(args, 'gb_nbody_steps', 100))} "
        f"--sort-n {int(getattr(args, 'gb_sort_n', 1000000))} "
        f"--sort-repeats {int(getattr(args, 'gb_sort_repeats', 5))}"
    )
    log("INFO", "Native geekbench-mix")
    result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
    if result["status"] != "success":
        return result
    p = result["payload"]
    summary = {
        "aes_round_mib_s": parse_float(p.get("aes_round", {}).get("mib_s")),
        "sha256_mib_s": parse_float(p.get("sha256", {}).get("mib_s")),
        "fft_mflops": parse_float(p.get("fft", {}).get("mflops")),
        "matmul_mflops": parse_float(p.get("matmul", {}).get("mflops")),
        "nbody_mflops": parse_float(p.get("nbody", {}).get("mflops")),
        "sort_mops": parse_float(p.get("sort", {}).get("mops")),
    }
    write_csv(device_dir / "details" / "native_geekbench.csv", [{"test": k, "value": v} for k, v in summary.items()])
    return {"status": "success", "summary": summary, "payload": p}


def run_ux_data(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_ux_data", False):
        return {"status": "skipped", "reason": "--skip-ux-data"}
    cmd = (
        f"--mode ux-data "
        f"--regex-kib {int(getattr(args, 'ux_regex_kib', 512))} "
        f"--regex-repeats {int(getattr(args, 'ux_regex_repeats', 8))} "
        f"--json-kib {int(getattr(args, 'ux_json_kib', 512))} "
        f"--json-repeats {int(getattr(args, 'ux_json_repeats', 8))} "
        f"--sort-n {int(getattr(args, 'ux_sort_n', 500000))} "
        f"--sort-repeats {int(getattr(args, 'ux_sort_repeats', 5))}"
    )
    log("INFO", "Native ux-data")
    result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
    if result["status"] != "success":
        return result
    p = result["payload"]
    summary = {
        "regex_mib_per_s": parse_float(p.get("regex", {}).get("mib_scanned_per_s")),
        "json_tokenize_mib_per_s": parse_float(p.get("json_tokenize", {}).get("mib_per_s")),
        "data_sort_mops": parse_float(p.get("sort", {}).get("mops")),
    }
    write_csv(device_dir / "details" / "native_ux_data.csv", [{"test": k, "value": v} for k, v in summary.items()])
    return {"status": "success", "summary": summary, "payload": p}


def run_ux_image(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_ux_image", False):
        return {"status": "skipped", "reason": "--skip-ux-image"}
    cmd = (
        f"--mode ux-image "
        f"--image-w {int(getattr(args, 'ux_image_w', 1024))} "
        f"--image-h {int(getattr(args, 'ux_image_h', 1024))} "
        f"--image-repeats {int(getattr(args, 'ux_image_repeats', 8))}"
    )
    log("INFO", "Native ux-image")
    result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
    if result["status"] != "success":
        return result
    p = result["payload"]
    summary = {
        "image_mpix_out_per_s": parse_float(p.get("image_proc", {}).get("mpix_out_per_s")),
    }
    write_csv(device_dir / "details" / "native_ux_image.csv", [{"test": k, "value": v} for k, v in summary.items()])
    return {"status": "success", "summary": summary, "payload": p}


def run_storage_random_native(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_storage_random_native", False):
        return {"status": "skipped", "reason": "--skip-storage-random-native"}
    target = "/data/local/tmp/hpfc_random.bin"
    rows: List[Dict[str, Any]] = []
    summary: Dict[str, Any] = {}
    bs = int(getattr(args, "storage_native_bs", 4096))
    ops = int(getattr(args, "storage_native_ops", 4096))
    direct_first = 1
    for io_kind, writes in (("read", 0), ("write", 1)):
        cmd = (
            f"--mode storage-random "
            f"--storage-path {shlex.quote(target)} "
            f"--storage-bs {bs} "
            f"--storage-ops {ops} "
            f"--storage-direct {direct_first} "
            f"--storage-writes {writes}"
        )
        log("INFO", f"Native storage-random {io_kind} (direct_io={direct_first})")
        result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
        payload = result.get("payload", {}) if result.get("status") == "success" else {}
        sr = payload.get("storage_random", {}) if payload else {}
        # Fallback when O_DIRECT was rejected outright (error field) OR was accepted by open()
        # but every I/O returned EINVAL (ops_ok == 0). Both manifest as "no usable data".
        ops_ok = parse_float(sr.get("ops_ok")) if sr else None
        needs_fallback = (not sr) or ("error" in sr) or (ops_ok is not None and ops_ok == 0)
        if needs_fallback:
            log("WARN", f"storage-random {io_kind} with direct_io produced no data; retrying without O_DIRECT")
            cmd2 = cmd.replace("--storage-direct 1", "--storage-direct 0")
            result = run_native_mode(adb, remote, cmd2, timeout=int(getattr(args, "native_timeout_sec", 300)))
            payload = result.get("payload", {}) if result.get("status") == "success" else {}
            sr = payload.get("storage_random", {}) if payload else {}
        rows.append({"kind": io_kind, **sr, "wall_sec": result.get("wall_sec")})
        summary[f"random_{io_kind}_iops"] = parse_float(sr.get("iops"))
        summary[f"random_{io_kind}_mib_s"] = parse_float(sr.get("mib_s"))
        summary[f"random_{io_kind}_direct_io"] = sr.get("direct_io")
    adb.shell(f"rm -f {shlex.quote(target)}", timeout=20)
    write_csv(device_dir / "details" / "native_storage_random.csv", rows)
    return {"status": "success" if any(r.get("ops_ok") for r in rows) else "failed",
            "summary": summary, "rows": rows}


def run_per_cluster_native(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_per_cluster", False):
        return {"status": "skipped", "reason": "--skip-per-cluster"}
    cmd = (
        f"--mode per-cluster "
        f"--int-iterations {int(getattr(args, 'per_cluster_int_iters', 30000000))} "
        f"--fp-iterations {int(getattr(args, 'per_cluster_fp_iters', 30000000))}"
    )
    log("INFO", "Native per-cluster (big vs little)")
    result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
    if result["status"] != "success":
        return result
    pc = (result["payload"] or {}).get("per_cluster", {})
    summary = {
        "hetero": pc.get("hetero"),
        "big_cpu": pc.get("big_cpu"),
        "big_max_khz": pc.get("big_max_khz"),
        "little_cpu": pc.get("little_cpu"),
        "little_max_khz": pc.get("little_max_khz"),
        "big_int_mops": parse_float(pc.get("big_int_mops")),
        "big_fp_mflops": parse_float(pc.get("big_fp_mflops")),
        "little_int_mops": parse_float(pc.get("little_int_mops")),
        "little_fp_mflops": parse_float(pc.get("little_fp_mflops")),
        "int_big_over_little": parse_float(pc.get("int_big_over_little")),
        "fp_big_over_little": parse_float(pc.get("fp_big_over_little")),
    }
    write_csv(device_dir / "details" / "native_per_cluster.csv", [{"test": k, "value": v} for k, v in summary.items()])
    return {"status": "success", "summary": summary, "payload": result["payload"]}


def run_latency_curve_native(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_latency_curve", False):
        return {"status": "skipped", "reason": "--skip-latency-curve"}
    sizes = getattr(args, "latency_sizes_kb", "32,128,512,2048,8192,32768,131072") or "32,128,512,2048,8192,32768"
    visits = int(getattr(args, "latency_curve_visits", 5000000))
    cmd = f"--mode latency-curve --latency-sizes-kb {shlex.quote(sizes)} --latency-visits {visits}"
    log("INFO", f"Native memory hierarchy curve (sizes_kb={sizes})")
    result = run_native_mode(adb, remote, cmd, timeout=int(getattr(args, "native_timeout_sec", 300)))
    if result["status"] != "success":
        return result
    points = (result["payload"] or {}).get("latency_curve", {}).get("points", []) or []
    rows = [{"working_set_kib": p.get("working_set_kib"), "ns_per_access": p.get("ns_per_access")} for p in points]
    write_csv(device_dir / "details" / "native_latency_curve.csv", rows)
    summary: Dict[str, Any] = {"points": rows}
    # Surface a few representative tier values for scoring
    for p in points:
        kb = p.get("working_set_kib")
        ns = parse_float(p.get("ns_per_access"))
        if ns is None or kb is None:
            continue
        if kb <= 64:
            summary["l1_ns"] = ns
        elif kb <= 1024:
            summary["l2_ns"] = ns
        elif kb <= 8192:
            summary["l3_ns"] = ns
        else:
            summary["dram_ns"] = ns
    return {"status": "success", "summary": summary, "payload": result["payload"]}


def run_sustained_native(adb: ADB, args: Any, device_dir: Path, remote: str) -> Dict[str, Any]:
    if getattr(args, "skip_sustained", False):
        return {"status": "skipped", "reason": "--skip-sustained"}
    rounds = int(getattr(args, "sustained_rounds", 20))
    kernel = getattr(args, "sustained_kernel", "int")
    per_round = int(getattr(args, "sustained_per_round", 50000000))
    cmd = (
        f"--mode sustained "
        f"--sustained-rounds {rounds} "
        f"--sustained-kernel {shlex.quote(kernel)} "
        f"--sustained-per-round {per_round} "
        f"--pin-big {0 if getattr(args, 'native_no_pin_big', False) else 1}"
    )
    log("INFO", f"Native sustained: rounds={rounds} kernel={kernel}")
    timeout = max(int(getattr(args, "native_timeout_sec", 300)), rounds * 8 + 30)
    result = run_native_mode(adb, remote, cmd, timeout=timeout)
    if result["status"] != "success":
        return result
    p = result["payload"]
    s = p.get("sustained", {})
    per_round_rows = s.get("per_round", []) or []
    durations = [parse_float(r.get("duration_sec")) for r in per_round_rows]
    durations = [d for d in durations if d is not None]
    summary = {
        "rounds": rounds,
        "kernel": kernel,
        "round_first_sec": parse_float(s.get("round_first_sec")),
        "round_last_sec": parse_float(s.get("round_last_sec")),
        "drift_pct": parse_float(s.get("drift_pct")),
        "best_sec": min(durations) if durations else None,
        "worst_sec": max(durations) if durations else None,
        "stability_ratio": (min(durations) / max(durations)) if durations and max(durations) > 0 else None,
    }
    write_csv(device_dir / "details" / "native_sustained.csv", [{"round": i + 1, **r} for i, r in enumerate(per_round_rows)])
    return {"status": "success", "summary": summary, "payload": p}


def native_thread_counts(args: Any) -> List[int]:
    if getattr(args, "native_threads", None):
        return sorted({int(x) for x in args.native_threads if int(x) > 0})
    max_workers = int(getattr(args, "cpu_max_workers", 0) or 8)
    return sorted({1, min(2, max_workers), min(4, max_workers), max_workers})


def summarize_native_rows(cpu_rows: List[Dict[str, Any]], memory_rows: List[Dict[str, Any]]) -> Dict[str, Any]:
    def best(test: str, key: str) -> Optional[float]:
        values = [parse_float(row.get(key)) for row in cpu_rows if row.get("test") == test]
        values = [v for v in values if v is not None]
        return round(max(values), 3) if values else None

    def best_for_threads(test: str, key: str, thread_target: int) -> Optional[float]:
        values = [
            parse_float(row.get(key))
            for row in cpu_rows
            if row.get("test") == test and int(row.get("threads") or 0) == thread_target
        ]
        values = [v for v in values if v is not None]
        return round(max(values), 3) if values else None

    def best_mem(test: str, key: str, higher: bool = True) -> Optional[float]:
        values = [parse_float(row.get(key)) for row in memory_rows if row.get("test") == test]
        values = [v for v in values if v is not None]
        if not values:
            return None
        return round((max(values) if higher else min(values)), 3)

    return {
        "cpu_int_single_mops": best_for_threads("cpu_int", "mops", 1),
        "cpu_int_best_mops": best("cpu_int", "mops"),
        "cpu_fp_single_mflops": best_for_threads("cpu_fp", "estimated_mflops", 1),
        "cpu_fp_best_mflops": best("cpu_fp", "estimated_mflops"),
        "memory_copy_mib_s": best_mem("memory_stream", "copy_mib_s_median") or best_mem("memory_stream", "copy_mib_s"),
        "memory_scale_mib_s": best_mem("memory_stream", "scale_mib_s_median") or best_mem("memory_stream", "scale_mib_s"),
        "memory_add_mib_s": best_mem("memory_stream", "add_mib_s_median") or best_mem("memory_stream", "add_mib_s"),
        "memory_triad_mib_s": best_mem("memory_stream", "triad_mib_s_median") or best_mem("memory_stream", "triad_mib_s"),
        "memory_latency_ns": best_mem("memory_latency", "ns_per_access", higher=False),
    }
