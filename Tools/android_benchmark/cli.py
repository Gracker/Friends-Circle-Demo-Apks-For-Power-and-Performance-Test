from __future__ import annotations

import argparse
import sys
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

from concurrent.futures import ThreadPoolExecutor, as_completed

from .adb import ADB, discover_adb_devices
from .ai_report import generate_ai_report
from .excel_report import generate_excel_report
from .collectors import collect_device_info
from .env import (
    COOLDOWN_STATUS_DISABLED, COOLDOWN_STATUS_SKIPPED_NO_HEAT,
    inter_stage_cooldown, prepare_environment, read_battery_gate,
    restore_environment, thermal_precool,
)
from .interaction import run_interaction_benchmark
from .native import (
    push_native_binary, run_geekbench_mix, run_latency_curve_native, run_native_benchmark,
    run_per_cluster_native, run_storage_random_native, run_sustained_native,
    run_ux_data, run_ux_image,
)
from .stages_multitask import run_multitask_benchmark
from .stages_ai import AI_DELEGATES, run_ai_benchmark
from .stages_gpu import run_gpu_benchmark
from .stages_memory_pressure import run_memory_pressure
from .stages_video import run_video_benchmark
from .system_trace import run_system_trace_benchmark
from .paths import DEFAULT_RUNS_ROOT, REPO_ROOT
from .reports import (
    compare_devices, compute_scores, coverage_matrix, flatten_device_metrics,
    generate_html_report, generate_markdown_report, write_hardware_baseline_csv,
    write_summary_metrics_csv,
)
from .stages_adb import (
    maybe_install_benchmark, run_battery_benchmark, run_cpu_benchmark,
    run_memory_benchmark, run_network_benchmark, run_storage_benchmark,
    run_thermal_benchmark,
)
from .stages_app import run_launch_benchmark, run_scroll_benchmark
from .utils import log, sanitize_for_filename, write_csv, write_json

def default_install_apk() -> Optional[str]:
    candidates = sorted((REPO_ROOT / "apk-released").glob("app-release-*.apk"))
    if candidates:
        return str(candidates[-1])
    candidates = sorted((REPO_ROOT / "apk-debug").glob("app-debug-*.apk"))
    if candidates:
        return str(candidates[-1])
    return None


def run_device(serial: str, args: argparse.Namespace, run_dir: Path) -> Dict[str, Any]:
    adb = ADB(serial=serial, dry_run=args.dry_run)
    if not adb.check_device():
        raise RuntimeError(f"ADB device unavailable: {serial}")
    model = adb.get_model()
    device_label = sanitize_for_filename(f"{model}-{serial}")
    device_dir = run_dir / "devices" / device_label
    for sub in ("details", "logs", "raw"):
        (device_dir / sub).mkdir(parents=True, exist_ok=True)

    log("INFO", "=" * 72)
    log("INFO", f"Device start: {device_label}")
    device_started = datetime.now().isoformat(timespec="seconds")
    device_info = collect_device_info(adb, device_dir)
    if args.collect_bugreport and not args.dry_run:
        bugreport_path = device_dir / "raw" / "bugreport.zip"
        log("INFO", f"Collect bugreport: {bugreport_path}")
        proc = adb.run(["bugreport", str(bugreport_path)], timeout=args.bugreport_timeout_sec)
        device_info["bugreport"] = {
            "status": "success" if proc.returncode == 0 else "failed",
            "path": str(bugreport_path),
            "output": (proc.stdout or proc.stderr or "")[-1000:],
        }

    env_snapshot = prepare_environment(adb, args)
    battery_gate = read_battery_gate(adb) if not adb.dry_run else {"charging": False, "level_pct": None}
    precool_info: Dict[str, Any] = {}
    if getattr(args, "thermal_precool_sec", 0) > 0:
        precool_info = thermal_precool(
            adb,
            target_ceiling_c=float(getattr(args, "thermal_precool_ceiling_c", 42.0)),
            max_wait_sec=int(args.thermal_precool_sec),
            dry_run=adb.dry_run,
        )

    benchmarks: Dict[str, Any] = {}
    cooldown_records: List[Dict[str, Any]] = []

    def cool(label: str, prev_stage: Optional[str] = None) -> None:
        prev_status = benchmarks.get(prev_stage, {}).get("status") if prev_stage else None
        rec = inter_stage_cooldown(
            adb, label,
            max_wait_sec=args.inter_stage_cooldown_sec,
            ceiling_c=args.inter_stage_cooldown_ceiling_c,
            min_wait_sec=args.inter_stage_cooldown_min_sec,
            dry_run=adb.dry_run,
            prev_status=prev_status,
        )
        if rec.get("status") not in (COOLDOWN_STATUS_DISABLED, COOLDOWN_STATUS_SKIPPED_NO_HEAT):
            cooldown_records.append(rec)

    pure_adb_skip_stages = ("cpu", "memory", "storage", "thermal", "battery", "network",
                             "native", "system_trace", "geekbench_mix", "ux_data", "ux_image",
                             "storage_random_native", "sustained", "memory_pressure",
                             "multitask", "per_cluster", "latency_curve",
                             "gpu", "ai", "video")
    native_extra_stages = ("geekbench_mix", "ux_data", "ux_image", "storage_random_native",
                           "sustained", "per_cluster", "latency_curve")
    app_skip_stages = ("launch", "scroll", "interaction")
    native_remote: Optional[str] = None
    try:
        if args.skip_pure_adb:
            for stage in pure_adb_skip_stages:
                benchmarks[stage] = {"status": "skipped", "reason": "--skip-pure-adb"}
        else:
            benchmarks["native"] = run_native_benchmark(adb, args, device_dir)
            # If native_benchmark succeeded, the binary already on device (it cleans up
            # at end of native_benchmark). Push fresh for the remaining native modes
            # so we don't depend on internal state from run_native_benchmark.
            if not adb.dry_run and not getattr(args, "skip_native_extras", False):
                push = push_native_binary(adb, force_rebuild=getattr(args, "rebuild_native", False))
                if push.get("status") == "success":
                    native_remote = push.get("binary_remote")
            try:
                if adb.dry_run:
                    for stage in native_extra_stages:
                        benchmarks[stage] = {"status": "dry_run",
                                              "reason": "dry-run does not exercise native extras"}
                elif native_remote:
                    cool("before_geekbench_mix", prev_stage="native")
                    benchmarks["geekbench_mix"] = run_geekbench_mix(adb, args, device_dir, native_remote)
                    cool("before_ux_data", prev_stage="geekbench_mix")
                    benchmarks["ux_data"] = run_ux_data(adb, args, device_dir, native_remote)
                    cool("before_ux_image", prev_stage="ux_data")
                    benchmarks["ux_image"] = run_ux_image(adb, args, device_dir, native_remote)
                    benchmarks["storage_random_native"] = run_storage_random_native(adb, args, device_dir, native_remote)
                    cool("before_sustained", prev_stage="ux_image")
                    benchmarks["sustained"] = run_sustained_native(adb, args, device_dir, native_remote)
                    cool("before_per_cluster", prev_stage="sustained")
                    benchmarks["per_cluster"] = run_per_cluster_native(adb, args, device_dir, native_remote)
                    cool("before_latency_curve", prev_stage="per_cluster")
                    benchmarks["latency_curve"] = run_latency_curve_native(adb, args, device_dir, native_remote)
                else:
                    skip_reason = "native binary unavailable"
                    for stage in native_extra_stages:
                        benchmarks[stage] = {"status": "skipped", "reason": skip_reason}
                cool("before_cpu", prev_stage="latency_curve")
                benchmarks["cpu"] = run_cpu_benchmark(adb, args, device_dir)
                cool("before_memory", prev_stage="cpu")
                benchmarks["memory"] = run_memory_benchmark(adb, args, device_dir)
                benchmarks["storage"] = run_storage_benchmark(adb, args, device_dir)
                cool("before_system_trace", prev_stage="memory")
                benchmarks["system_trace"] = run_system_trace_benchmark(serial, adb, args, device_dir)
                cool("before_thermal", prev_stage="system_trace")
                benchmarks["thermal"] = run_thermal_benchmark(adb, args, device_dir)
                cool("before_battery", prev_stage="thermal")
                benchmarks["battery"] = run_battery_benchmark(adb, args, device_dir)
                benchmarks["network"] = run_network_benchmark(adb, args, device_dir)
                benchmarks["install"] = maybe_install_benchmark(adb, args, device_dir)
                cool("before_memory_pressure", prev_stage="battery")
                benchmarks["memory_pressure"] = run_memory_pressure(adb, args, device_dir)
                cool("before_multitask", prev_stage="memory_pressure")
                benchmarks["multitask"] = run_multitask_benchmark(adb, args, device_dir)
                cool("before_gpu", prev_stage="multitask")
                benchmarks["gpu"] = run_gpu_benchmark(adb, args, device_dir)
                cool("before_ai", prev_stage="gpu")
                benchmarks["ai"] = run_ai_benchmark(adb, args, device_dir)
                cool("before_video", prev_stage="ai")
                benchmarks["video"] = run_video_benchmark(adb, args, device_dir)
            finally:
                if native_remote and not getattr(args, "keep_device_temp", False):
                    adb.shell(f"rm -f {native_remote}", timeout=20)

        if args.skip_app_benchmarks:
            for stage in app_skip_stages:
                benchmarks[stage] = {"status": "skipped", "reason": "--skip-app-benchmarks"}
        else:
            cool("before_interaction", prev_stage="video")
            benchmarks["interaction"] = run_interaction_benchmark(serial, adb, args, device_dir)
            cool("before_launch", prev_stage="interaction")
            benchmarks["launch"] = run_launch_benchmark(serial, args, device_dir, adb=adb)
            cool("before_scroll", prev_stage="launch")
            benchmarks["scroll"] = run_scroll_benchmark(serial, args, device_dir)
    finally:
        restore_environment(adb, env_snapshot)
        # Defense-in-depth thermal cleanup: if any stage's host-side timeout fired,
        # the on-device shell can outlive the disconnect and keep dd / native bench
        # workers running. Force-kill anything matching our /data/local/tmp/hpfc_* prefix
        # and remove the temp files (unless the user explicitly opted to keep them).
        if not adb.dry_run:
            try:
                adb.shell("pkill -f /data/local/tmp/hpfc_ 2>/dev/null; true", timeout=15)
                if not getattr(args, "keep_device_temp", False):
                    adb.shell("rm -rf /data/local/tmp/hpfc_*", timeout=20)
            except Exception as exc:
                log("WARN", f"final device cleanup failed: {exc}")

    device_finished = datetime.now().isoformat(timespec="seconds")
    result = {
        "device_label": device_label,
        "serial": serial,
        "device_dir": str(device_dir),
        "started_at": device_started,
        "finished_at": device_finished,
        "device_info": device_info,
        "benchmarks": benchmarks,
        "env": env_snapshot.to_dict(),
        "battery_gate": battery_gate,
        "precool": precool_info,
        "cooldowns": cooldown_records,
        "coverage": [],  # filled in by caller with full device_results context
    }
    metrics = flatten_device_metrics(result)
    result["metrics"] = metrics
    write_summary_metrics_csv(device_dir, metrics)
    write_json(device_dir / "device_summary.json", result)
    return result


PRESETS = {
    "smoke": {
        "benchmark_iterations": 1,
        "cpu_mb": 16,
        "memory_mb": 64,
        "storage_mb": 32,
        "storage_random_ops": 64,
        "thermal_duration_sec": 15,
        "battery_duration_sec": 15,
        "ping_count": 5,
        "system_trace_seconds": 3,
        "system_trace_cpu_mb": 32,
        "system_trace_io_mb": 32,
        "native_memory_mib": 32,
        "native_memory_iterations": 2,
        "native_latency_mib": 16,
        "native_latency_visits": 5000000,
        "native_int_iterations": 10000000,
        "native_fp_iterations": 10000000,
        "interaction_iterations": 3,
        "scroll_trace_seconds": 5,
        "launch_iterations": 1,
        "gb_aes_bytes": 16 * 1024 * 1024,
        "gb_sha_bytes": 16 * 1024 * 1024,
        "gb_fft_log2": 12,
        "gb_fft_repeats": 10,
        "gb_matmul_n": 128,
        "gb_matmul_repeats": 1,
        "gb_nbody_n": 128,
        "gb_nbody_steps": 20,
        "gb_sort_n": 200000,
        "gb_sort_repeats": 2,
        "ux_regex_kib": 128,
        "ux_regex_repeats": 2,
        "ux_json_kib": 128,
        "ux_json_repeats": 2,
        "ux_sort_n": 100000,
        "ux_sort_repeats": 2,
        "ux_image_w": 256,
        "ux_image_h": 256,
        "ux_image_repeats": 2,
        "storage_native_ops": 512,
        "sustained_rounds": 3,
        "sustained_per_round": 5000000,
        "memory_pressure_max_mb": 256,
        "memory_pressure_step_mb": 32,
        "gpu_duration_sec": 10,
        "ai_runs": 10,
        "video_seconds": 2,
    },
    "standard": {},  # leaves all argparse defaults untouched
    "deep": {
        "benchmark_iterations": 5,
        "thermal_duration_sec": 240,
        "battery_duration_sec": 240,
        "interaction_iterations": 10,
        "scroll_trace_seconds": 30,
        "launch_iterations": 8,
        "system_trace_seconds": 20,
        "gb_fft_repeats": 100,
        "gb_matmul_repeats": 10,
        "gb_nbody_steps": 200,
        "gb_sort_repeats": 10,
        "ux_regex_repeats": 16,
        "ux_json_repeats": 16,
        "ux_image_repeats": 16,
        "storage_native_ops": 16384,
        "sustained_rounds": 30,
        "memory_pressure_max_mb": 2048,
        "gpu_duration_sec": 120,
        "ai_runs": 100,
        "video_seconds": 8,
    },
}


def _validate_presets_against_parser() -> None:
    """Guardrail: every PRESETS key must correspond to a real argparse dest.

    Runs once at module import so typos in PRESETS fail fast rather than at runtime
    on the next benchmark run.
    """
    dummy = build_arg_parser().parse_args([])
    known = {k for k, v in vars(dummy).items()}
    for preset, overrides in PRESETS.items():
        for key in overrides:
            if key not in known:
                raise AssertionError(f"PRESETS[{preset!r}] has unknown arg '{key}'")


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Run comprehensive Android benchmarks across one or more connected devices."
    )
    parser.add_argument("--serial", action="append", default=[], help="ADB serial. Repeat for multiple devices.")
    parser.add_argument("--output-dir", type=Path, default=None, help="Run output directory.")
    parser.add_argument("--output-dir-force", action="store_true", help="Allow writing into a non-empty --output-dir.")
    parser.add_argument("--dry-run", action="store_true", help="Print adb commands and emit a fake dry-run bundle.")
    parser.add_argument("--preset", choices=sorted(PRESETS.keys()), default="standard",
                        help="Preset that pre-fills durations/iterations. User-supplied flags override it.")
    parser.add_argument("--skip-pure-adb", action="store_true", help="Skip hardware/CPU/memory/storage/thermal/battery/network stages.")
    parser.add_argument("--skip-app-benchmarks", action="store_true", help="Skip integrated launch and scroll benchmark stages.")
    parser.add_argument("--keep-device-temp", action="store_true", help="Keep /data/local/tmp benchmark files on device.")
    parser.add_argument("--benchmark-iterations", type=int, default=3, help="Iterations for pure ADB/native microbenchmarks.")
    parser.add_argument("--collect-bugreport", action="store_true", help="Also collect full adb bugreport.zip under raw/. Slow but comprehensive.")
    parser.add_argument("--bugreport-timeout-sec", type=int, default=900)

    # Environment stabilization
    parser.add_argument("--no-env-stabilize", action="store_true", help="Skip screen/animation/brightness setup and restore.")
    parser.add_argument("--animation-scale", type=float, default=1.0, help="Force window/transition/animator scales (0.0..N).")
    parser.add_argument("--animations-off", action="store_true", help="Shortcut for --animation-scale 0.0.")
    parser.add_argument("--screen-brightness", type=int, default=128, help="Fixed brightness (0..255) during run.")
    parser.add_argument("--dnd-on", action="store_true", help="Enable Do Not Disturb (zen_mode=1) during run.")
    parser.add_argument("--allow-charging-battery-stage", action="store_true",
                        help="Run battery drain stage even when charging (meaningless value; for testing).")
    parser.add_argument("--thermal-precool-sec", type=int, default=0,
                        help="Wait up to N seconds for max thermal zone <= ceiling before starting stages.")
    parser.add_argument("--thermal-precool-ceiling-c", type=float, default=42.0,
                        help="Target max thermal zone temperature for --thermal-precool-sec.")
    parser.add_argument("--inter-stage-cooldown-sec", type=int, default=0,
                        help="Cap (sec) for cooldown before throttle-sensitive stages "
                             "(launch/scroll/sustained/thermal/...). 0 disables. "
                             "Recommended 60 for accurate launch/scroll measurements.")
    parser.add_argument("--inter-stage-cooldown-ceiling-c", type=float, default=42.0,
                        help="Target max thermal zone temperature for inter-stage cooldown.")
    parser.add_argument("--inter-stage-cooldown-min-sec", type=int, default=0,
                        help="Always wait at least N sec at each cooldown gate (lets PMIC/skin temp "
                             "decay even when thermal zones already read cool). 0 disables minimum.")

    parser.add_argument("--cpu-mb", type=int, default=128, help="MiB per single-worker CPU hash pass.")
    parser.add_argument("--cpu-max-workers", type=int, default=0, help="0 means min(cpu_count, 8).")
    parser.add_argument("--memory-mb", type=int, default=1024)
    parser.add_argument("--storage-mb", type=int, default=512)
    parser.add_argument("--storage-random-ops", type=int, default=512)
    parser.add_argument("--thermal-duration-sec", type=int, default=120)
    parser.add_argument("--thermal-workers", type=int, default=0)
    parser.add_argument("--throttle-ratio-threshold", type=float, default=0.75)
    parser.add_argument("--battery-duration-sec", type=int, default=120)
    parser.add_argument("--battery-wake-screen", action="store_true")
    parser.add_argument("--sample-interval-sec", type=float, default=5.0)
    parser.add_argument("--ping-host", default="223.5.5.5")
    parser.add_argument("--ping-count", type=int, default=20)
    parser.add_argument("--ping-interval-sec", type=float, default=0.2)
    parser.add_argument("--install-benchmark-apk", default=default_install_apk())
    parser.add_argument("--install-timeout-sec", type=int, default=180)
    parser.add_argument("--skip-system-trace", action="store_true")
    parser.add_argument("--system-trace-seconds", type=int, default=10)
    parser.add_argument("--system-trace-cpu-mb", type=int, default=128)
    parser.add_argument("--system-trace-io-mb", type=int, default=128)
    parser.add_argument("--system-trace-stop-timeout-sec", type=int, default=45)
    parser.add_argument("--skip-native", action="store_true", help="Skip self-built native Geekbench-like benchmark.")
    parser.add_argument("--rebuild-native", action="store_true", help="Force rebuild native benchmark binary.")
    parser.add_argument("--native-threads", nargs="+", type=int, default=[])
    parser.add_argument("--native-memory-mib", type=int, default=128)
    parser.add_argument("--native-memory-iterations", type=int, default=3)
    parser.add_argument("--native-latency-mib", type=int, default=64)
    parser.add_argument("--native-latency-visits", type=int, default=20000000)
    parser.add_argument("--native-int-iterations", type=int, default=50000000)
    parser.add_argument("--native-fp-iterations", type=int, default=50000000)
    parser.add_argument("--native-timeout-sec", type=int, default=300)
    parser.add_argument("--native-sched-fifo", action="store_true",
                        help="Best-effort SCHED_FIFO for native workload (usually requires root; rarely useful).")
    parser.add_argument("--native-no-pin-big", action="store_true",
                        help="Disable pinning single-thread native runs to the big cluster.")
    parser.add_argument("--skip-native-extras", action="store_true",
                        help="Skip geekbench-mix/ux-data/ux-image/storage-random/sustained native modes.")

    # Geekbench-style mix
    parser.add_argument("--skip-geekbench-mix", action="store_true")
    parser.add_argument("--gb-aes-bytes", type=int, default=64 * 1024 * 1024)
    parser.add_argument("--gb-sha-bytes", type=int, default=64 * 1024 * 1024)
    parser.add_argument("--gb-fft-log2", type=int, default=14)
    parser.add_argument("--gb-fft-repeats", type=int, default=50)
    parser.add_argument("--gb-matmul-n", type=int, default=256)
    parser.add_argument("--gb-matmul-repeats", type=int, default=5)
    parser.add_argument("--gb-nbody-n", type=int, default=256)
    parser.add_argument("--gb-nbody-steps", type=int, default=100)
    parser.add_argument("--gb-sort-n", type=int, default=1000000)
    parser.add_argument("--gb-sort-repeats", type=int, default=5)

    # AnTuTu UX-style
    parser.add_argument("--skip-ux-data", action="store_true")
    parser.add_argument("--ux-regex-kib", type=int, default=512)
    parser.add_argument("--ux-regex-repeats", type=int, default=8)
    parser.add_argument("--ux-json-kib", type=int, default=512)
    parser.add_argument("--ux-json-repeats", type=int, default=8)
    parser.add_argument("--ux-sort-n", type=int, default=500000)
    parser.add_argument("--ux-sort-repeats", type=int, default=5)
    parser.add_argument("--skip-ux-image", action="store_true")
    parser.add_argument("--ux-image-w", type=int, default=1024)
    parser.add_argument("--ux-image-h", type=int, default=1024)
    parser.add_argument("--ux-image-repeats", type=int, default=8)

    # True random IOPS via native binary
    parser.add_argument("--skip-storage-random-native", action="store_true")
    parser.add_argument("--storage-native-bs", type=int, default=4096)
    parser.add_argument("--storage-native-ops", type=int, default=4096)

    # Sustained perf
    parser.add_argument("--skip-sustained", action="store_true")
    parser.add_argument("--sustained-rounds", type=int, default=20)
    parser.add_argument("--sustained-kernel", choices=["int", "fp"], default="int")
    parser.add_argument("--sustained-per-round", type=int, default=50000000)

    # Per-cluster CPU (big.LITTLE)
    parser.add_argument("--skip-per-cluster", action="store_true")
    parser.add_argument("--per-cluster-int-iters", type=int, default=30000000)
    parser.add_argument("--per-cluster-fp-iters", type=int, default=30000000)

    # Memory hierarchy curve
    parser.add_argument("--skip-latency-curve", action="store_true")
    parser.add_argument("--latency-sizes-kb", type=str, default="32,128,512,2048,8192,32768,131072",
                        help="Comma-separated working-set sizes in KiB. Default spans L1d → DRAM.")
    parser.add_argument("--latency-curve-visits", type=int, default=5000000)

    # Multitask
    parser.add_argument("--skip-multitask", action="store_true")
    parser.add_argument("--multitask-max-apps", type=int, default=8,
                        help="Max apps in the multitask chain. Default 8 covers typical RAM (4-12GB).")
    parser.add_argument("--multitask-settle-sec", type=float, default=1.5)
    parser.add_argument("--multitask-apps", nargs="+", default=[],
                        help="Extra packages to prepend to the multitask chain (must be installed + launchable).")

    # Memory pressure
    parser.add_argument("--skip-memory-pressure", action="store_true")
    parser.add_argument("--memory-pressure-max-mb", type=int, default=1024)
    parser.add_argument("--memory-pressure-target-pct", type=float, default=0.0,
                        help="If > 0, auto-scale target to this fraction of MemTotal (overrides absolute "
                             "if larger). Use 0.5..0.7 to push devices into their LMK regime — "
                             "this is the only way to compare RAM-capacity behavior fairly.")
    parser.add_argument("--memory-pressure-step-mb", type=int, default=64)
    parser.add_argument("--memory-pressure-settle-sec", type=float, default=0.4)

    # GPU
    parser.add_argument("--skip-gpu", action="store_true")
    parser.add_argument("--gpu-duration-sec", type=int, default=60)

    # AI / NPU
    parser.add_argument("--skip-ai", action="store_true")
    parser.add_argument("--ai-runs", type=int, default=50)
    parser.add_argument("--ai-cpu-threads", type=int, default=4)
    parser.add_argument("--ai-timeout-sec", type=int, default=180)
    parser.add_argument("--ai-delegates", nargs="+", default=None,
                        choices=list(AI_DELEGATES),
                        help=f"Subset of {list(AI_DELEGATES)}; default tries all.")

    # Video
    parser.add_argument("--skip-video", action="store_true")
    parser.add_argument("--video-seconds", type=int, default=4)
    parser.add_argument("--video-bit-rate", type=int, default=8_000_000)

    # Cold launch hardening
    parser.add_argument("--no-harden-cold-launch", dest="harden_cold_launch",
                        action="store_false", default=True,
                        help="Skip drop_caches + compile reset before launch (otherwise auto when root available).")

    # Multi-device parallelism
    parser.add_argument("--parallel-devices", type=int, default=1,
                        help="Run up to N devices concurrently. WARNING: thermal cross-talk and "
                             "shared USB bandwidth may distort results when devices sit close together.")

    parser.add_argument("--skip-launch", action="store_true")
    parser.add_argument("--skip-launch-install", action="store_true")
    parser.add_argument("--launch-iterations", type=int, default=5)
    parser.add_argument("--launch-case-interval-ms", type=int, default=2000)
    parser.add_argument("--launch-timeout-sec", type=int, default=60 * 60 * 3)
    parser.add_argument("--launch-apps", nargs="+",
                        default=["launch-aosp", "launch-compose", "launch-webview", "launch-gl"],
                        help="Default covers 4 stack types (aosp/compose/webview/gl) at the cheapest "
                             "flavor; launch-game has a known activity-class mismatch and is excluded by default. "
                             "Pass empty list [] or specific names to override.")
    parser.add_argument("--launch-flavors", nargs="+", default=["light"],
                        help="Default 'light' to keep run time bounded.")

    parser.add_argument("--skip-scroll", action="store_true")
    parser.add_argument("--skip-scroll-install", action="store_true")
    parser.add_argument("--scroll-trace-seconds", type=int, default=20)
    parser.add_argument("--scroll-case-interval-ms", type=int, default=1000)
    parser.add_argument("--scroll-timeout-sec", type=int, default=60 * 60 * 6)
    parser.add_argument(
        "--scroll-loads",
        nargs="+",
        default=["heavy_mixed"],
        help="Default reduced to single representative load to keep run time bounded; "
             "previous default ['minimal','heavy','heavy_between_frames','heavy_mixed'] still works if passed explicitly.",
    )
    parser.add_argument(
        "--scroll-only-apk",
        nargs="+",
        default=[
            "scrolling-aosp-performance",
            "scrolling-aosp-customscroller",
            "scrolling-aosp-renderstress",
            "scrolling-aosp-video",
            "scrolling-aosp-picasso",
            "scrolling-compose",
            "scrolling-webview",
            "friends_circle_v19tv",
        ],
        help="Default skips known-flaky modules (scrolling-aosp-softwarerender on A15+, "
             "friends_circle_v19sv, scrolling-aosp-mixedrender). Pass module names to override.",
    )
    parser.add_argument("--scroll-disable-gfxinfo", action="store_true")
    parser.add_argument("--scroll-allow-main-entry", action="store_true")
    parser.add_argument("--scroll-loads-full", action="store_true",
                        help="Override --scroll-loads with the full 4-load set "
                             "[minimal, heavy, heavy_between_frames, heavy_mixed].")
    parser.add_argument("--skip-interaction", action="store_true")
    parser.add_argument("--skip-interaction-install", action="store_true")
    parser.add_argument("--interaction-apk", default="")
    parser.add_argument("--rebuild-interaction", action="store_true")
    parser.add_argument("--interaction-iterations", type=int, default=5)
    parser.add_argument("--interaction-settle-sec", type=float, default=1.0)
    parser.add_argument("--interaction-trace-warmup-sec", type=float, default=0.3)
    parser.add_argument("--interaction-post-tap-sec", type=float, default=0.8)
    parser.add_argument("--interaction-case-interval-sec", type=float, default=0.5)
    parser.add_argument("--interaction-trace-stop-timeout-sec", type=int, default=45)
    parser.add_argument("--interaction-tap-x-ratio", type=float, default=0.5)
    parser.add_argument("--interaction-tap-y-ratio", type=float, default=0.5)
    return parser


def apply_preset(args: argparse.Namespace, argv: List[str]) -> None:
    """Fill preset values only for flags the user did not explicitly set.

    We detect user-supplied flags by token-scanning argv (all preset-eligible
    flags are long-form, so this is sufficient). `parser.get_default` comparison
    won't work because an explicit `--flag=default` is indistinguishable from
    not passing the flag at all.
    """
    preset = PRESETS.get(args.preset, {})
    if not preset:
        return
    supplied: set[str] = set()
    for token in argv:
        if token.startswith("--"):
            name = token.split("=", 1)[0][2:].replace("-", "_")
            supplied.add(name)
    for key, value in preset.items():
        if key in supplied:
            continue
        if hasattr(args, key):
            setattr(args, key, value)


def validate_args(args: argparse.Namespace) -> None:
    if args.cpu_mb <= 0:
        raise ValueError("--cpu-mb must be > 0")
    if args.benchmark_iterations <= 0:
        raise ValueError("--benchmark-iterations must be > 0")
    if args.memory_mb <= 0:
        raise ValueError("--memory-mb must be > 0")
    if args.storage_mb <= 0:
        raise ValueError("--storage-mb must be > 0")
    if args.storage_random_ops < 0:
        raise ValueError("--storage-random-ops must be >= 0")
    if args.sample_interval_sec <= 0:
        raise ValueError("--sample-interval-sec must be > 0")
    if args.ping_count <= 0:
        raise ValueError("--ping-count must be > 0")
    if args.interaction_iterations <= 0:
        raise ValueError("--interaction-iterations must be > 0")
    if args.thermal_duration_sec < 0:
        raise ValueError("--thermal-duration-sec must be >= 0")
    if args.battery_duration_sec < 0:
        raise ValueError("--battery-duration-sec must be >= 0")
    if args.system_trace_seconds <= 0:
        raise ValueError("--system-trace-seconds must be > 0")
    if args.launch_iterations <= 0:
        raise ValueError("--launch-iterations must be > 0")
    if args.scroll_trace_seconds <= 0:
        raise ValueError("--scroll-trace-seconds must be > 0")
    if args.native_memory_mib <= 0:
        raise ValueError("--native-memory-mib must be > 0")
    if args.native_latency_mib <= 0:
        raise ValueError("--native-latency-mib must be > 0")
    if args.native_latency_visits <= 0:
        raise ValueError("--native-latency-visits must be > 0")
    if args.native_int_iterations <= 0:
        raise ValueError("--native-int-iterations must be > 0")
    if args.native_fp_iterations <= 0:
        raise ValueError("--native-fp-iterations must be > 0")
    if args.animation_scale < 0:
        raise ValueError("--animation-scale must be >= 0")
    if args.screen_brightness < 0 or args.screen_brightness > 255:
        raise ValueError("--screen-brightness must be in [0, 255]")
    if args.thermal_precool_sec < 0:
        raise ValueError("--thermal-precool-sec must be >= 0")
    if args.inter_stage_cooldown_sec < 0:
        raise ValueError("--inter-stage-cooldown-sec must be >= 0")
    if args.inter_stage_cooldown_ceiling_c <= 0:
        raise ValueError("--inter-stage-cooldown-ceiling-c must be > 0")
    if args.inter_stage_cooldown_min_sec < 0:
        raise ValueError("--inter-stage-cooldown-min-sec must be >= 0")
    if args.gb_fft_log2 < 6 or args.gb_fft_log2 > 20:
        raise ValueError("--gb-fft-log2 must be in [6, 20]")
    if args.gb_matmul_n <= 0:
        raise ValueError("--gb-matmul-n must be > 0")
    if args.gb_nbody_n <= 0:
        raise ValueError("--gb-nbody-n must be > 0")
    if args.gb_sort_n <= 0:
        raise ValueError("--gb-sort-n must be > 0")
    if args.ux_image_w <= 0 or args.ux_image_h <= 0:
        raise ValueError("--ux-image-w/--ux-image-h must be > 0")
    if args.storage_native_bs <= 0 or args.storage_native_bs % 4096 != 0:
        raise ValueError("--storage-native-bs must be a positive multiple of 4096")
    if args.storage_native_ops <= 0:
        raise ValueError("--storage-native-ops must be > 0")
    if args.sustained_rounds <= 0:
        raise ValueError("--sustained-rounds must be > 0")
    if args.sustained_per_round <= 0:
        raise ValueError("--sustained-per-round must be > 0")
    if args.memory_pressure_max_mb <= 0:
        raise ValueError("--memory-pressure-max-mb must be > 0")
    if args.memory_pressure_step_mb <= 0:
        raise ValueError("--memory-pressure-step-mb must be > 0")
    if args.gpu_duration_sec <= 0:
        raise ValueError("--gpu-duration-sec must be > 0")
    if args.ai_runs <= 0:
        raise ValueError("--ai-runs must be > 0")
    if args.video_seconds <= 0:
        raise ValueError("--video-seconds must be > 0")


def main() -> int:
    _validate_presets_against_parser()
    parser = build_arg_parser()
    argv = sys.argv[1:]
    args = parser.parse_args(argv)
    apply_preset(args, argv)
    if getattr(args, "scroll_loads_full", False):
        args.scroll_loads = ["minimal", "heavy", "heavy_between_frames", "heavy_mixed"]
    try:
        validate_args(args)
    except ValueError as exc:
        parser.error(str(exc))

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    run_dir = args.output_dir or (DEFAULT_RUNS_ROOT / f"run-{timestamp}")
    if args.output_dir and run_dir.exists() and any(run_dir.iterdir()) and not args.output_dir_force:
        parser.error(
            f"--output-dir {run_dir} is not empty; pass --output-dir-force to overwrite or choose a new directory."
        )
    run_dir.mkdir(parents=True, exist_ok=True)
    (run_dir / "details").mkdir(parents=True, exist_ok=True)

    if args.dry_run and not args.serial:
        serials = ["DRYRUN_SERIAL"]
    elif args.serial:
        serials = args.serial
    else:
        devices = discover_adb_devices()
        if not devices:
            log("ERROR", "No connected adb devices. Use --serial or connect a device.")
            return 1
        serials = [device["serial"] for device in devices]

    started_at = datetime.now().isoformat(timespec="seconds")
    log("INFO", f"Run dir: {run_dir}")
    log("INFO", f"Devices: {', '.join(serials)}")

    device_results: List[Dict[str, Any]] = []
    failures: List[str] = []
    parallel = max(1, int(getattr(args, "parallel_devices", 1)))
    if parallel > 1 and len(serials) > 1:
        log("INFO", f"Running {len(serials)} devices in parallel (max workers={parallel}). "
                    "Thermal cross-talk and USB bandwidth contention may affect results.")
        with ThreadPoolExecutor(max_workers=parallel) as ex:
            futs = {ex.submit(run_device, s, args, run_dir): s for s in serials}
            for fut in as_completed(futs):
                serial = futs[fut]
                try:
                    device_results.append(fut.result())
                except Exception as exc:
                    log("ERROR", f"Device failed: {serial}: {exc}")
                    failures.append(f"{serial}: {exc}")
        device_results.sort(key=lambda d: serials.index(d["serial"]))
    else:
        for serial in serials:
            try:
                device_results.append(run_device(serial, args, run_dir))
            except Exception as exc:
                log("ERROR", f"Device failed: {serial}: {exc}")
                failures.append(f"{serial}: {exc}")

    comparisons = compare_devices(device_results)
    scores = compute_scores(device_results)
    coverage = coverage_matrix(device_results)
    payload = {
        "schema_version": "1.1",
        "started_at": started_at,
        "finished_at": datetime.now().isoformat(timespec="seconds"),
        "run_dir": str(run_dir),
        "config": vars(args),
        "devices": device_results,
        "comparisons": comparisons,
        "scores": scores,
        "coverage": coverage,
        "failures": failures,
    }
    write_hardware_baseline_csv(run_dir, device_results)
    write_csv(run_dir / "details" / "comparison.csv", comparisons)
    write_json(run_dir / "summary.json", payload)
    md_path = generate_markdown_report(run_dir, payload)
    html_path = generate_html_report(run_dir, payload)
    ai_path = generate_ai_report(run_dir, payload)
    excel_path = generate_excel_report(run_dir, payload)

    # Chinese-named convenience copies (humans pick these; tooling still uses English).
    import shutil
    chinese_aliases = {
        "原始数据.json": run_dir / "summary.json",
        "综合报告.html": html_path,
        "综合报告.md": md_path,
        "AI对比报告.md": ai_path,
    }
    for zh_name, src in chinese_aliases.items():
        try:
            shutil.copy2(src, run_dir / zh_name)
        except OSError as exc:
            log("WARN", f"Could not write Chinese alias {zh_name}: {exc}")

    log("INFO", "=" * 72)
    log("INFO", f"原始 JSON:       {run_dir / 'summary.json'}")
    log("INFO", f"综合报告 (md):   {md_path}")
    log("INFO", f"综合报告 (html): {html_path}")
    log("INFO", f"AI 对比报告:     {ai_path}")
    log("INFO", f"Excel 对比:      {excel_path}")
    log("INFO", f"中文文件名副本:  原始数据.json / 综合报告.html / 综合报告.md / AI对比报告.md")
    if failures:
        log("WARN", f"Completed with device failures: {len(failures)}")
        return 1
    return 0
