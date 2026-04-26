"""Cross-metric interpretation / dependency analysis.

For each performance metric we surface, which configuration / runtime
parameters could plausibly explain a gap? This module encodes those
relationships as rules and emits findings the LLM (or human) can read
without having to memorize the framework's internals.

Output shape: a list of `Finding` dicts with:
  - kind: "configuration_diff" | "performance_gap" | "anomaly" | "advisory"
  - severity: 0..3 (3 = major / blocker, 0 = informational)
  - title:   short Chinese headline
  - detail:  longer Chinese explanation
  - related_metrics: list of metric keys this is about
  - related_params: list of device_info / env keys to inspect
  - devices:  optional set of device labels involved
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional, Sequence

from .reports import compute_category_aggregates, compute_config_diffs, pct_diff
from .utils import parse_float


# ===== dependency knowledge base =====
# Map a metric key to the device_info / env params that plausibly affect it.
# Keys here are loose paths into device_info / env. Used for "this is slow,
# inspect these knobs" guidance.
METRIC_DEPENDENCIES: Dict[str, List[str]] = {
    # CPU
    "native.cpu_int_best_mops": [
        "soc_model", "cpu_topology", "thermal.min_freq_ratio",
        "vendor_power_mode", "battery.charging",
    ],
    "native.cpu_fp_best_mflops": ["soc_model", "thermal.min_freq_ratio", "vendor_power_mode"],
    "gb.matmul_mflops": ["soc_model", "L1/L2 cache size (无法从 adb 直接读)"],
    "gb.fft_mflops": ["soc_model", "FP unit width (NEON/SVE)"],
    "gb.aes_round_mib_s": ["soc_model"],
    "gb.sha256_mib_s": ["soc_model", "compiler vectorization (固定)"],

    # Memory
    "native.memory_triad_mib_s": [
        "soc_model", "DRAM type (LPDDR4X/5/5x，需要 board info)",
        "memory_total_mb",
    ],
    "native.memory_latency_ns": ["soc_model", "DRAM type", "internal bus 频率"],
    "memory_pressure.max_resident_mb": [
        "memory_total_mb", "mm.zram_enabled", "mm.zram_comp_algorithm",
        "mm.mglru_enabled", "vm.swappiness", "vm.dirty_ratio",
        "vm.min_free_kbytes", "foreground_package",
    ],
    "memory_pressure.headroom_pct": [
        "memory_total_mb", "vm.min_free_kbytes", "mm.zram_enabled",
    ],

    # Storage
    "storage.random_read_iops_native": [
        "storage.data_fs.fstype", "storage.data_fs.options",
        "UFS/eMMC 型号 (raw/storage_identity.txt)", "thermal.min_freq_ratio",
    ],
    "storage.random_write_iops_native": [
        "storage.data_fs.fstype", "storage.data_fs.options",
        "fsync_mode (nobarrier/wbarrier)", "discard 是否启用",
    ],
    "storage.seq_write_mib_s": [
        "UFS/eMMC 型号", "storage.data_fs.options", "writeback 队列",
    ],

    # UI
    "scroll.avg_fps": [
        "peak_refresh_rate", "min_refresh_rate", "hwui.hwui_use_vulkan",
        "hwui.renderengine_backend", "env.animation_scale",
        "thermal.min_freq_ratio", "gpu_freq.gpu_governor",
        "foreground_package (是否被其他 app 干扰)",
    ],
    "scroll.avg_jank_pct": [
        "hwui.hwui_use_vulkan", "hwui.renderengine_backend",
        "env.animation_scale", "thermal.min_freq_ratio",
        "选择的 scroll module（renderstress 是设计就慢的）",
    ],
    "scroll.p99_frame_ms": [
        "hwui.renderengine_backend", "env.animation_scale",
        "GC pauses (dalvik.vm.heapsize)", "binder 拥塞",
    ],
    "interaction.dispatch_median_ms": [
        "input subsystem 调度", "thermal.min_freq_ratio",
        "foreground_package activity 是否 native",
    ],
    "interaction.on_draw_median_ms": [
        "hwui pipeline", "env.animation_scale", "RenderThread 优先级",
    ],
    "launch.best_avg_ms": [
        "drop_caches state (非 root 不可控)", "cmd package compile 状态",
        "dalvik.vm.dex2oat-filter / image-dex2oat-filter",
        "process freezer 状态", "foreground_package (启动时其他 app 占资源)",
    ],

    # Sustained / Thermal
    "sustained.stability_ratio": [
        "thermal.max_temp_c", "thermal.min_freq_ratio", "battery.charging",
        "vendor_power_mode (省电/性能模式)",
    ],
    "sustained.drift_pct": [
        "thermal.max_temp_c", "thermal.min_freq_ratio",
        "GPU governor 是否随 CPU 一起 throttle",
    ],
    "thermal.max_temp_c": [
        "case_count (累计跑了多少 stage)", "precool 是否启用",
        "battery.charging (充电会发热)", "chassis material (无法采集)",
    ],
    "thermal.min_freq_ratio": [
        "thermal.max_temp_c", "vendor_power_mode", "soc_model",
        "ARMv8 sha256 stressor 局限 (advisory)",
    ],

    # Battery
    "battery.drain_pct": [
        "battery.charging", "屏幕亮度 (env.screen_brightness)",
        "Wi-Fi/cellular 状态", "采样窗口太短 → 量化噪声",
    ],
    "battery.avg_power_mw": [
        "current_now 符号 OEM 相关", "充电时无意义", "屏幕亮度",
    ],

    # Install
    "install.duration_sec": [
        "memory_total_mb (pm 行为受可用内存影响)",
        "data_fs IO 速度 (写 APK)",
        "正在前台运行的应用 (并发 IO)",
    ],

    # UX
    "ux.image_mpix_out_per_s": [
        "L2/L3 cache 大小 (1024×1024 RGBA = 4MiB 工作集)",
        "thermal.min_freq_ratio", "memory_latency_ns",
    ],
    "ux.regex_mib_per_s": ["L1 cache speed", "branch predictor"],
    "ux.json_tokenize_mib_per_s": ["branch predictor", "memory_triad"],
}


METRIC_NAMES_ZH: Dict[str, str] = {
    # populated lazily via _zh_name
}


def _zh_name(metric_key: str) -> str:
    """Best-effort Chinese name from excel_report.METRIC_DICT, else the key."""
    try:
        from .excel_report import METRIC_DICT
        if metric_key in METRIC_DICT:
            return METRIC_DICT[metric_key][0]
    except Exception:
        pass
    return metric_key


# ===== finding helpers =====
def _finding(kind: str, severity: int, title: str, detail: str,
             related_metrics: Optional[Sequence[str]] = None,
             related_params: Optional[Sequence[str]] = None,
             devices: Optional[Sequence[str]] = None) -> Dict[str, Any]:
    return {
        "kind": kind,
        "severity": severity,
        "title": title,
        "detail": detail,
        "related_metrics": list(related_metrics or []),
        "related_params": list(related_params or []),
        "devices": list(devices or []),
    }


def _device_metric_value(device: Dict[str, Any], key: str) -> Optional[float]:
    for m in device.get("metrics") or []:
        if m["key"] == key:
            return parse_float(m.get("value"))
    return None


def _device_label(device: Dict[str, Any]) -> str:
    return device.get("device_label", "?")


def _info_path(device: Dict[str, Any], path: str) -> Any:
    """Walk a dotted path inside device_info or env. Tolerates None."""
    info = device.get("device_info", {}) or {}
    env = (device.get("env", {}) or {}).get("applied", {}) or {}
    battery_gate = device.get("battery_gate") or {}
    if path.startswith("env."):
        return env.get(path[4:])
    if path.startswith("battery."):
        return battery_gate.get(path[8:])
    cur: Any = info
    for part in path.split("."):
        if isinstance(cur, dict):
            cur = cur.get(part)
        else:
            return None
    return cur


# ===== rules =====
def _rule_charging_warns(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for d in payload.get("devices") or []:
        gate = d.get("battery_gate") or {}
        if gate.get("charging"):
            out.append(_finding(
                "advisory", 1,
                f"`{_device_label(d)}` 充电中运行",
                "battery 阶段电量变化与热行为均受充电干扰：(a) battery.drain_pct 已自动改为 advisory；"
                "(b) thermal_max_temp 偏高（充电额外发热）；(c) 部分 OEM 在充电时启用更激进的限频/省电策略。"
                "建议复测时拔出 USB（仍然 USB-debug 用 wireless adb 或 OTG 触发）以拿到无干扰数据。",
                related_metrics=["battery.drain_pct", "battery.avg_power_mw", "thermal.max_temp_c"],
                related_params=["battery.charging", "vendor_power_mode"],
                devices=[_device_label(d)],
            ))
    return out


def _rule_thermal_throttle(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for d in payload.get("devices") or []:
        ratio = _device_metric_value(d, "thermal.min_freq_ratio")
        max_temp = _device_metric_value(d, "thermal.max_temp_c")
        drift = _device_metric_value(d, "sustained.drift_pct")
        stability = _device_metric_value(d, "sustained.stability_ratio")
        label = _device_label(d)
        if ratio is not None and ratio < 0.85:
            out.append(_finding(
                "performance_gap", 2,
                f"`{label}` 压测期间发生限频 (min_freq_ratio={ratio:.2f})",
                "限频意味着 CPU 在压测期间被降到最大频率的 85% 以下；"
                "这会同时影响 CPU 整数/浮点跑分和 sustained 稳定性。"
                f"对应的最高温度 {max_temp}°C，sustained drift {drift}%，stability {stability}。"
                "如果重视长跑性能，建议先 thermal_precool 几分钟再测。",
                related_metrics=["thermal.min_freq_ratio", "sustained.drift_pct", "sustained.stability_ratio",
                                  "native.cpu_int_best_mops"],
                related_params=["thermal.max_temp_c", "battery.charging", "vendor_power_mode"],
                devices=[label],
            ))
        sustained_summary = (d.get("benchmarks", {}).get("sustained") or {}).get("summary") or {}
        best_sec = sustained_summary.get("best_sec")
        # Below ~1s per round, drift % is dominated by clock noise (especially under
        # the smoke preset which uses tiny iteration counts). Suppress the warning;
        # the drift number is still surfaced in the metrics table for transparency.
        round_meaningful = isinstance(best_sec, (int, float)) and best_sec >= 1.0
        if drift is not None and drift > 15 and round_meaningful:
            out.append(_finding(
                "performance_gap", 1,
                f"`{label}` 持续性能漂移 {drift:.1f}%（首末轮 duration 差异）",
                "drift > 15% 说明长时间负载下首末轮性能差异显著，常见原因："
                "(1) 热降频；(2) 调度器把 worker 从 big cluster 迁出；(3) 充电带来的热扰动。"
                "对比 thermal.max_temp_c 与 thermal.min_freq_ratio 同设备数据可定位是否为热问题。",
                related_metrics=["sustained.drift_pct", "thermal.max_temp_c", "thermal.min_freq_ratio"],
                related_params=["env.animation_scale", "battery.charging"],
                devices=[label],
            ))
    return out


def _rule_low_lmk_headroom(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for d in payload.get("devices") or []:
        max_resident = _device_metric_value(d, "memory_pressure.max_resident_mb")
        headroom = _device_metric_value(d, "memory_pressure.headroom_pct")
        info = d.get("device_info", {}) or {}
        ram = info.get("memory_total_mb")
        label = _device_label(d)
        # Suppress when the test reached its configured cap before LMK ever fired —
        # the headroom number is then just (target_mb / ram_mb) and tells us nothing
        # about the device's real LMK threshold (common with the smoke preset's 256 MiB cap).
        mem_press = d.get("benchmarks", {}).get("memory_pressure") or {}
        capped_at_target = bool(mem_press.get("reached_target"))
        if headroom is not None and headroom < 30 and not capped_at_target:
            out.append(_finding(
                "performance_gap", 2,
                f"`{label}` 内存 headroom 仅 {headroom:.0f}% (max_resident={max_resident} MiB / RAM={ram} MiB)",
                "headroom = max_resident_mb / memory_total_mb；表示在被 LMK 杀掉前能保留多少比例的 RAM。"
                "<30% 通常意味着多任务场景下后台 app 容易被 evict。"
                "影响因素：vm.swappiness、zram 是否启用 + 压缩算法、mglru、min_free_kbytes、"
                "前台应用启动时已驻留的内存。",
                related_metrics=["memory_pressure.max_resident_mb", "memory_pressure.headroom_pct",
                                  "memory_pressure.lmkd_kills_during"],
                related_params=["memory_total_mb", "mm.zram_enabled", "mm.zram_comp_algorithm",
                                 "mm.mglru_enabled", "vm.swappiness", "vm.dirty_ratio"],
                devices=[label],
            ))
        if max_resident is not None and ram and max_resident >= ram * 0.9:
            out.append(_finding(
                "anomaly", 0,
                f"`{label}` memory_pressure 达到目标上限 ({max_resident} MiB ≈ {ram} MiB 的 {max_resident/ram*100:.0f}%)",
                "压测目标设置过低，没观察到 LMK 真正介入。建议把 --memory-pressure-target-pct 调到 0.7-0.8 "
                "再跑一次，找到 LMK 实际介入的水位。",
                related_metrics=["memory_pressure.max_resident_mb"],
                related_params=["memory_total_mb"],
                devices=[label],
            ))
    return out


def _rule_scroll_jank(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for d in payload.get("devices") or []:
        jank = _device_metric_value(d, "scroll.avg_jank_pct")
        fps = _device_metric_value(d, "scroll.avg_fps")
        info = d.get("device_info", {}) or {}
        label = _device_label(d)
        if jank is not None and jank > 20:
            anim_scale = ((d.get("env") or {}).get("applied") or {}).get("animation_scale")
            hwui = info.get("hwui") or {}
            out.append(_finding(
                "performance_gap", 2,
                f"`{label}` 滑动 jank 比例 {jank:.1f}% (avg fps {fps})",
                "jank > 20% 是显著掉帧。可能原因（按可控性排序）："
                "(1) 选了 renderstress 模块——它设计就慢，是预期；"
                "(2) HWUI 渲染管线（vulkan/skia-gl）匹配度；"
                "(3) animation_scale 偏小（0.5x）导致更频繁帧；"
                "(4) 热降频；(5) 前台应用未稳定就开始测；"
                "(6) input swipe 本身的抖动（gesture 不真实）。",
                related_metrics=["scroll.avg_jank_pct", "scroll.p95_frame_ms", "scroll.p99_frame_ms",
                                  "thermal.min_freq_ratio"],
                related_params=["hwui.renderengine_backend", "hwui.hwui_use_vulkan",
                                 "env.animation_scale", "peak_refresh_rate"],
                devices=[label],
            ))
    return out


def _rule_storage_dependencies(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    devices = payload.get("devices") or []
    if len(devices) < 2:
        return out
    write_iops_pairs: List[tuple] = []
    for d in devices:
        v = _device_metric_value(d, "storage.random_write_iops_native")
        if v is not None:
            write_iops_pairs.append((_device_label(d), v, d))
    if len(write_iops_pairs) < 2:
        return out
    write_iops_pairs.sort(key=lambda x: x[1])
    slowest = write_iops_pairs[0]
    fastest = write_iops_pairs[-1]
    if fastest[1] > 0:
        gap = (fastest[1] - slowest[1]) / fastest[1] * 100
        if gap > 15:
            slow_d = slowest[2]
            fast_d = fastest[2]
            slow_opts = ((slow_d.get("device_info", {}).get("storage") or {}).get("data_fs") or {}).get("options") or ""
            fast_opts = ((fast_d.get("device_info", {}).get("storage") or {}).get("data_fs") or {}).get("options") or ""
            same_fs = (((slow_d.get("device_info", {}).get("storage") or {}).get("data_fs") or {}).get("fstype")
                       == ((fast_d.get("device_info", {}).get("storage") or {}).get("data_fs") or {}).get("fstype"))
            detail = (
                f"`{fastest[0]}` 比 `{slowest[0]}` 4K 随机写快 {gap:.1f}% "
                f"({fastest[1]:.0f} vs {slowest[1]:.0f} IOPS)。"
            )
            if same_fs:
                detail += " 文件系统类型相同，差异可能来自："
            else:
                detail += " 文件系统类型不同，差异主要是："
            detail += "(a) UFS/eMMC 物理介质等级；(b) mount options（fsync_mode=nobarrier 写入不刷 cache，会更快但不安全）；"
            detail += "(c) 厂商对 IO scheduler 的 tuning。"
            if "nobarrier" in fast_opts and "nobarrier" not in slow_opts:
                detail += " 注意：fastest 启用了 nobarrier（写性能更高，但断电时数据风险更大）。"
            out.append(_finding(
                "configuration_diff", 1,
                f"存储随机写差距 {gap:.1f}% 可由 mount options 部分解释",
                detail,
                related_metrics=["storage.random_write_iops_native", "storage.seq_write_mib_s"],
                related_params=["storage.data_fs.fstype", "storage.data_fs.options"],
                devices=[fastest[0], slowest[0]],
            ))
    return out


def _rule_install_dependencies(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Install speed correlates with available RAM and storage write speed."""
    out = []
    devices = payload.get("devices") or []
    if len(devices) < 2:
        return out
    pairs: List[tuple] = []
    for d in devices:
        v = _device_metric_value(d, "install.duration_sec")
        if v is not None:
            pairs.append((_device_label(d), v, d))
    if len(pairs) < 2:
        return out
    pairs.sort(key=lambda x: x[1])
    fastest = pairs[0]
    slowest = pairs[-1]
    if slowest[1] > 0:
        gap = (slowest[1] - fastest[1]) / slowest[1] * 100
        if gap > 15:
            fast_ram = (fastest[2].get("device_info") or {}).get("memory_total_mb")
            slow_ram = (slowest[2].get("device_info") or {}).get("memory_total_mb")
            fast_write = _device_metric_value(fastest[2], "storage.seq_write_mib_s")
            slow_write = _device_metric_value(slowest[2], "storage.seq_write_mib_s")
            out.append(_finding(
                "configuration_diff", 1,
                f"`{fastest[0]}` APK 安装比 `{slowest[0]}` 快 {gap:.1f}%",
                f"{fastest[1]:.2f}s vs {slowest[1]:.2f}s。安装时间主要受三个变量影响："
                f"(a) 顺序写带宽（fastest {fast_write} MiB/s vs slowest {slow_write} MiB/s）；"
                f"(b) 可用内存（fastest {fast_ram} MiB vs slowest {slow_ram} MiB —— pm 在内存紧张时会先压缩再写）；"
                f"(c) dex2oat 编译策略 —— pm install -d 跳过验证，'install --no-incremental' 走全量。"
                "本工具用 -r -d，相对一致。",
                related_metrics=["install.duration_sec", "storage.seq_write_mib_s", "memory_pressure.max_resident_mb"],
                related_params=["memory_total_mb", "storage.data_fs.options"],
                devices=[fastest[0], slowest[0]],
            ))
    return out


def _rule_ram_capacity_signal(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    """When devices have different RAM, where does that show up in the data?"""
    out = []
    devices = payload.get("devices") or []
    if len(devices) < 2:
        return out
    rams = [(d, parse_float((d.get("device_info") or {}).get("memory_total_mb"))) for d in devices]
    rams = [(d, r) for d, r in rams if r is not None]
    if len(rams) < 2:
        return out
    rams.sort(key=lambda x: x[1])
    smallest = rams[0]
    largest = rams[-1]
    if largest[1] - smallest[1] < 1024:
        return out  # less than 1 GiB difference, ignore
    headroom_small = _device_metric_value(smallest[0], "memory_pressure.headroom_pct")
    headroom_large = _device_metric_value(largest[0], "memory_pressure.headroom_pct")
    detail_lines = [
        f"`{largest[0].get('device_label')}` ({largest[1]:.0f} MiB) "
        f"vs `{smallest[0].get('device_label')}` ({smallest[1]:.0f} MiB)，相差 {largest[1] - smallest[1]:.0f} MiB。",
        "RAM 容量差异在以下指标中**应当**显现，但本工具的支持程度："
    ]
    detail_lines.append(
        f"  - **memory_pressure.headroom_pct** ✓：{largest[0].get('device_label')}={headroom_large}, "
        f"{smallest[0].get('device_label')}={headroom_small}（这是直接信号；目前 target_pct={(payload.get('config') or {}).get('memory_pressure_target_pct', '默认未设')}）"
    )
    detail_lines.append(
        "  - **install.duration_sec** ~：内存紧张时 pm 行为变慢；间接信号"
    )
    multi_lines: List[str] = []
    for dev in payload.get("devices") or []:
        mt = dev.get("benchmarks", {}).get("multitask") or {}
        ratio = mt.get("revisit_to_cold_ratio")
        chain = mt.get("apps_in_chain")
        if ratio is not None or chain is not None:
            multi_lines.append(
                f"{dev.get('device_label')}: chain={chain} apps, revisit/cold={ratio}"
            )
    if multi_lines:
        detail_lines.append(
            "  - **multitask 行为** ✓：" + "; ".join(multi_lines)
            + "（revisit_ms/cold_ms <0.2 表示首 app 仍驻留内存；接近 1 表示已被 evict）"
        )
    else:
        detail_lines.append(
            "  - **multitask 行为** —：本次未跑 multitask stage（链长 <3 或被 --skip-multitask 跳过）"
        )
    detail_lines.append(
        "  - **DRAM 带宽 / 延迟** —：和 RAM 容量基本无关（取决于 LPDDR 代际，与 SoC 配套）；"
        "本次两台都是 MT6855 配 LPDDR5/5x，所以 triad 接近"
    )
    out.append(_finding(
        "configuration_diff", 1,
        "RAM 容量差异的可观测信号清单",
        "\n".join(detail_lines),
        related_metrics=["memory_pressure.max_resident_mb", "memory_pressure.headroom_pct",
                          "install.duration_sec", "native.memory_triad_mib_s"],
        related_params=["memory_total_mb"],
        devices=[d.get("device_label") for d, _ in rams],
    ))
    return out


def _rule_cpu_scaling_efficiency(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for d in payload.get("devices") or []:
        single = _device_metric_value(d, "native.cpu_int_single_mops")
        best = _device_metric_value(d, "native.cpu_int_best_mops")
        cpu_count = (d.get("device_info") or {}).get("cpu_count")
        label = _device_label(d)
        if single is None or best is None or not cpu_count:
            continue
        ideal_8x = single * cpu_count
        efficiency = best / ideal_8x * 100 if ideal_8x > 0 else None
        if efficiency is None:
            continue
        if efficiency < 60:
            out.append(_finding(
                "anomaly", 1,
                f"`{label}` CPU 多线程扩展效率仅 {efficiency:.1f}% "
                f"({best:.0f} 实测 / {ideal_8x:.0f} 理论 = {cpu_count} × {single:.0f})",
                "理想情况下 N 核应该接近 N × 单核分数。低效率原因："
                "(1) big.LITTLE 异构（小核帮不上大忙）—— 这通常会让效率落在 40-70%；"
                "(2) 调度器把 worker 在不同 cluster 间反复迁移；"
                "(3) thermal throttle 让某些核降频；"
                "(4) memory 子系统瓶颈（多 worker 共享 cache/带宽）。"
                f"参考下面的 thermal.min_freq_ratio 看是否有热限频。",
                related_metrics=["native.cpu_int_single_mops", "native.cpu_int_best_mops",
                                  "native.cpu_fp_single_mflops", "native.cpu_fp_best_mflops",
                                  "thermal.min_freq_ratio"],
                related_params=["soc_model", "cpu_topology"],
                devices=[label],
            ))
        else:
            out.append(_finding(
                "configuration_diff", 0,
                f"`{label}` CPU 多线程扩展效率 {efficiency:.1f}% (n={cpu_count})",
                f"单核 {single:.0f} Mops/s × {cpu_count} = {ideal_8x:.0f} 理论；实测 best={best:.0f}。"
                "对 big.LITTLE 异构 SoC 这是正常水平。",
                related_metrics=["native.cpu_int_single_mops", "native.cpu_int_best_mops"],
                devices=[label],
            ))
    return out


def _rule_advisory_inventory(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    failed_stages = []
    advisory_stages = []
    for d in payload.get("devices") or []:
        for k, v in (d.get("benchmarks") or {}).items():
            status = (v or {}).get("status")
            if status in ("failed", "missing"):
                failed_stages.append((_device_label(d), k, status, (v or {}).get("reason", "")))
            elif status == "advisory":
                advisory_stages.append((_device_label(d), k))
    if failed_stages:
        bullets = "; ".join(f"`{label}/{stage}` ({status})" for label, stage, status, _ in failed_stages[:8])
        out.append(_finding(
            "advisory", 1,
            f"{len(failed_stages)} 个 stage 没产出数据",
            f"{bullets}；这些 stage 在 metrics 与 score 中缺席，但不会让其他数据失效。",
            related_metrics=[], related_params=[],
        ))
    return out


# ===== entry point =====
def interpret_findings(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    findings: List[Dict[str, Any]] = []
    findings.extend(_rule_charging_warns(payload))
    findings.extend(_rule_thermal_throttle(payload))
    findings.extend(_rule_low_lmk_headroom(payload))
    findings.extend(_rule_scroll_jank(payload))
    findings.extend(_rule_storage_dependencies(payload))
    findings.extend(_rule_install_dependencies(payload))
    findings.extend(_rule_ram_capacity_signal(payload))
    findings.extend(_rule_cpu_scaling_efficiency(payload))
    findings.extend(_rule_advisory_inventory(payload))
    findings.sort(key=lambda f: -f["severity"])
    return findings


def metric_dependency_hint(metric_key: str) -> List[str]:
    """Public helper so reports can show 'this metric depends on…' inline."""
    return METRIC_DEPENDENCIES.get(metric_key, [])
