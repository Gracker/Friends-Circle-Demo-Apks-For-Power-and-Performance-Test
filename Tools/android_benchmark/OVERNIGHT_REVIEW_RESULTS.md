# Android Benchmark Overnight Review & Rework — Results

**Date:** 2026-04-25 (overnight pass)  
**Scope:** Tools/android_benchmark/ + Tools/android_benchmark_native/ + Tools/perfetto.config + run_android_comprehensive_benchmark.sh

---

## 工作流（按 CLAUDE.md 规则执行）

1. **Memory 搜索** — 无前序工作，全新审查。
2. **4 位专家并行 Review**（不同视角）：
   - Microbenchmark methodology（CPU/Mem/IO/Native/Network）
   - System config coverage（perf engineer 该知道的设备状态）
   - UI perf methodology（launch/scroll/interaction/trace）
   - Reproducibility + reports + operator ergonomics
3. **Plan → Codex read-only review → Revise**（CLAUDE.md rule 的双 agent 规则）。Codex 指出了 ≥8 处方案缺陷（`valid_for_score` 字段、battery 方向、preset 不能覆盖用户值、stay_on_while_plugged=3 只在插电时有效、SCHED_FIFO 默认关闭、GPU freq 单位歧义…），我采纳后再执行。
4. **Execute** — 13 个文件改动 + 2 个新文件 + 1 个测试套件。
5. **`/simplify` 三 agent 并行**（reuse / quality / efficiency），收敛成 15 个清理改动。
6. **17 个单元测试 + 4 组 `--dry-run` 场景** 全部通过。

---

## 一、四位 Reviewer 找到的核心问题

### P0（Bogus measurement — 测的不是你以为的东西）

| 文件:行 | 问题 | 原因 |
|---|---|---|
| `stages_adb.py::run_cpu_benchmark` | `dd if=/dev/zero \| sha256sum` 不是 CPU 分数 | ARMv8 `sha256` 是硬件加速指令；量的是 kernel memcpy + HW crypto |
| `stages_adb.py::run_memory_benchmark` | `dd /dev/zero -> /dev/null` 不碰 DRAM | 纯 kernel syscall loop，从不分配用户态 buffer |
| `hpfc_native_bench.c::run_memory` | STREAM `best` biased 偏高，无 warmup/affinity | 首轮 page-fault 计入；无 big.LITTLE 意识 |
| `stages_adb.py::run_storage_benchmark` | 顺序 read 命中 page cache | 刚写完立刻读同一个文件，全命中 buffer cache |
| 同上 | random IOPS 被 shell fork 开销封顶 | `while...dd` 循环；fork ~1ms，IOPS 上限 ~500 |
| `stages_adb.py::run_thermal_benchmark` | HW crypto 压力不满载大核 | 同 CPU benchmark 的问题 |
| `collectors.py` | 漏 HWUI pipeline / SELinux / 前台应用 / animation scales / zram / scheduler / GPU freq / UFS 身份 / vendor power mode | 性能工程师诊断时必看的状态没抓 |
| `cli.py` | 默认跑多小时 | 无 preset；`launch-timeout-sec=3h`, `scroll-timeout-sec=6h` |
| `perfetto.config` | `duration_ms: 20000` 硬编码 | `--system-trace-seconds` 无效；interaction trace 20s 空转 |
| `reports.py` | 单设备 score 永远 100 | `len(present) == 1 -> normalized=100` |
| `reports.py::compute_scores` | winner_delta_pct 可 ZeroDivisionError | 没 guard ping_jitter=0 等情况 |
| `stages_app.py::run_launch_benchmark` | "冷启动"其实是半温 | 非 root 设备没法 drop_caches / compile reset |
| `interaction.py` | `onDraw` 不等于"可见" | onDraw 是 draw-command-start，不是 SF present_fence |
| `env` | 没屏幕/动画/充电/热身控制 | run 开始不唤醒屏幕、不关动画、不检查是否在充电 |

### P1+ 见 coverage 章节（下）。

---

## 二、实际修的改动清单

### Phase 1 — 诚实标注（metric 框架升级）
- `metric_row` 增加 `status` (`active`/`advisory`)、`advisory_reason`、`valid_for_score` 字段。
- 只有 `valid_for_score=True` 的 metric 进入 `compute_scores`。
- `crypto`（旧 `cpu` shell hash）和 `baseline`（旧 `memory` kernel loop）降级成 advisory 类别，不计分。
- 存储 `buffered_read`、random IOPS 都带 `advisory_reason`。
- launch、interaction、thermal 整个 stage status 自动设为 `advisory` 并附原因。

### Phase 2 — 系统状态采集（collectors.py 新增）
新增 **13 个 raw 文件** + **9 个 parser 函数**：

- `selinux.txt`（getenforce）
- `sys_zram_snapshot.txt` + `proc_swaps.txt`（压缩算法、swap 占用）
- `proc_sys_vm.txt`（swappiness、dirty_ratio、min_free_kbytes 等 8 项）
- `mglru.txt`（`/sys/kernel/mm/lru_gen/enabled`）
- `proc_sys_kernel_sched.txt` + `cpufreq_schedutil.txt`（调度器 / EAS / hispeed_freq）
- `gpu_freq.txt`（kgsl / devfreq / mali）
- `cpu_idle_usage.txt`（C-state 驻留）
- `storage_identity.txt`（UFS/eMMC 型号 + `/proc/scsi/ufs/*`）
- `dumpsys_game.txt`（Android 12+ Game Mode）
- `dumpsys_jobscheduler.txt`（前 200 行）
- `foreground_state.txt`（从 `dumpsys activity activities` 取当前前台）
- `cmd_wifi_status.txt`（RSSI / link speed）
- `choreographer_log.txt`（logcat Skipped frames）

parsers: `parse_animation_scales`, `parse_foreground_app`, `parse_data_filesystem`, `parse_zram`, `parse_wifi_link`, `parse_gpu_freq`, `parse_vendor_power_mode`, `parse_hwui_pipeline`, `parse_dalvik_vm`.

`device_info` 新增 surface 字段：`selinux_mode`, `hwui`, `dalvik_vm`, `animation_scales`, `foreground_package`, `storage.data_fs`, `mm.zram`, `mm.mglru_enabled`, `mm.vm_tuning`, `gpu_freq`, `wifi_link`, `vendor_power_mode`, `memory_available_mb`, `memory_free_mb`。

`hardware_baseline.csv` 行列扩展，包含 HWUI、SELinux、data_fs、zram/MGLRU、wifi RSSI。

### Phase 3 — 环境稳定（新模块 `env.py`）
- `EnvSnapshot` dataclass，保存用户原值
- `prepare_environment(adb, args)`：
  - `svc power stayon usb` + `stay_on_while_plugged_in=3`
  - `screen_off_timeout=1800000` ms
  - `KEYCODE_WAKEUP` + `wm dismiss-keyguard`
  - 三个 animation scale 强制成 `--animation-scale`（默认 1.0）
  - `screen_brightness_mode=0` + `screen_brightness=--screen-brightness`
  - 可选 `--dnd-on` 打开 zen_mode
  - `logcat -c`
- `restore_environment()`：原值为空 → `settings delete`；原值存在 → 写回
- `read_battery_gate()`：运行前抓充电状态和电量；复用 `parse_battery_dump`
- `thermal_precool()`：可选等待最大热区 ≤ 阈值
- `cli.py::run_device` 用 try/finally 包所有 stage，保证 restore 必执行

### Phase 4 — Native binary（hpfc_native_bench.c + native.py）
- **int MOPS ×5 修正**（以前 `iterations / elapsed / 1e6`，漏乘 ops_per_iter）
- `run_memory`：warmup 一轮丢弃 + 每轮结果数组 → `best_*` 与 `*_median` 双输出
- `mlock()` STREAM buffer，排除页错影响
- 单线程 pin 到 big cluster（扫描 `cpuinfo_max_freq` 取最大）；多线程 `clear_affinity` 让 scheduler 自由
- `--sched-fifo 0/1` opt-in（默认关，避免卡 adb）
- `--pin-big 0/1`（默认开）
- JSON 增加 `"meta": {compile_flags, sched_fifo_applied, int_ops_per_iter, fp_ops_per_iter}`
- `run_memory` / `run_latency` 失败路径只 printf 一次错误对象（避免半截 JSON）
- `native.py` 编译加 `-march=armv8-a+simd`
- `find_ndk_clang()` 移除硬编码路径，改为 `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `HPFC_ANDROID_SDK` 环境变量 + `~/Library/Android/sdk` 默认值

### Phase 5 — stages_adb.py
- `run_battery_benchmark`：
  - 开始前抓 battery_gate；若 `ac/usb/wireless_powered=true` 且无 `--allow-charging-battery-stage` → 直接返回 `status=advisory, advisory_reason="device is charging"`
  - 新增 `drain_pct = start - end, direction=lower`（修正旧 `level_delta_pct` 的方向 bug）
  - 新增 `avg_power_mw_estimate = |I| × |V|`
- `run_cpu_benchmark` / `run_memory_benchmark` / `run_thermal_benchmark` status 设为 `advisory`，附 `advisory_reason`
- `run_storage_benchmark`：random read/write row 标 `advisory` + reason；sequential_read 加 `cache_warning` 字段
- 返回 key 改名（主要的）：`sequential_read_mib_s → buffered_read_mib_s`，`random_read_iops → random_read_iops_advisory`，`single_worker_mib_s → crypto_single_mib_s`，`throughput_mib_s → kernel_loop_mib_s`

### Phase 6 — reports.py
- `compute_scores`：
  - 单设备返回 `{label: {total: None, absolute_mode: False, note: "…"}}`（不再虚报 100）
  - 多设备：固定 `weight_denominator` = `sum(SCORE_WEIGHTS.values())`，缺失 category 对总分贡献 0 但仍在 `categories` 里显示 `None`
  - 只扫 `valid_for_score=True` 的 metric
  - `SCORE_WEIGHTS` 重平衡：scroll 20 / launch 15 / interaction 15 / cpu 15 / memory 10 / storage 10 / thermal 8 / battery 3 / network 2 / install 1（符合这个项目 UI perf testbed 的定位）
- `_safe_delta_pct` guard 除零
- `coverage_matrix(device_results)` 现在按 stage 聚合实际 status，不再写死 "automated"
- 新增 `summarize_run_health()`：充电状态、电量、前台应用、animation scale applied、failed/skipped/advisory stages、SELinux、HWUI renderer、data_fs
- Markdown/HTML report 头部加 **Run Health** 和 **Measurement Caveats** 两个块
- HTML 处理 `None` category（显示 `—`，不再 crash）

### Phase 7 — cli.py
- `PRESETS = {smoke, standard, deep}`
- `apply_preset(args, argv)`：只填用户没显式传的字段（token-scan argv）
- `_validate_presets_against_parser()`：启动时检查 PRESETS 键全部匹配 argparse dest（防 typo）
- 新增 flags：`--animation-scale`, `--animations-off`, `--screen-brightness`, `--dnd-on`, `--allow-charging-battery-stage`, `--thermal-precool-sec`, `--thermal-precool-ceiling-c`, `--no-env-stabilize`, `--native-sched-fifo`, `--native-no-pin-big`, `--output-dir-force`
- `validate_args` 扩展到 `thermal_duration_sec`, `battery_duration_sec`, `system_trace_seconds`, `launch_iterations`, `scroll_trace_seconds`, `native_*`, `animation_scale`, `screen_brightness`, `thermal_precool_sec`
- `--output-dir` 非空时默认拒绝（除非 `--output-dir-force`）
- `schema_version: 1.1`

### Phase 8 — Perfetto
- `Tools/perfetto.config`：
  - 删除 `duration_ms: 20000`（以前覆盖 `-t`，让 `--system-trace-seconds` 失效）
  - 主 buffer 62 MiB → 256 MiB；第二 buffer 2 MiB → 4 MiB
  - `linux.sys_stats` 采样 1s → 250ms，增加 `meminfo_period_ms=1000` + `vmstat_period_ms=1000`
  - 增加 `atrace_categories: "hwui"`
- `perfetto.py::TraceRecorder` 新增 `duration_sec` 参数，start() 时传 `-t Ns`
- `system_trace.py` 用 `args.system_trace_seconds + 2`
- `interaction.py` 用 `warmup + post_tap + TRACE_TAIL_BUFFER_SEC(1.5)`
- `TraceRecorder.stop()` kill 后 `communicate(timeout=5)` 防 perfetto wedged 挂死

### Phase 9 — 文档
- `README.md` 重写：
  - **Measurement caveats** 表（每个 metric 的 status + 原因）
  - **Environment stabilization** 说明
  - **Presets**
  - **Deferred work**（见下）
  - **明确不在范围**（WALT、音频 RT、长期电池、0→100、boot time）

### Phase 10 — 测试
- 新建 `tests/test_parsers.py`：17 个测试
  - 7 个 parser fixture（animation scales、foreground app、data filesystem、zram、wifi link、battery dump、HWUI+vendor）
  - 10 个 report math（`_safe_delta_pct` 的 lower/higher/zero 边界、`metric_row` 的 status 推导、`compute_scores` 单/多设备、advisory 排除、flatten 新旧 key 迁移、`compare_devices` 零值）
- `python3 -m unittest Tools.android_benchmark.tests.test_parsers` → 17/17 ✓

### Phase 11 — /simplify 三 agent 清理
- `stages_app.py` 缺 `List` import（潜在 NameError）→ 补
- `collectors.py` 未用的 `sanitize_for_filename` import → 删
- `env.py::read_battery_gate` 重写 `dumpsys battery` 解析 → 改为复用 `parse_battery_dump`
- `env.py::ANIMATION_KEYS` 与 `collectors.ANIMATION_SCALE_KEYS` 重复 → 合并
- `interaction.py::parse_screen_size` 重写 regex → 改为复用 `collectors.parse_wm_size`
- `interaction.py` 魔数 `1.5` → `TRACE_TAIL_BUFFER_SEC` 常量 + 注释
- `native.py` `/Users/chris/tools/Android-sdk` 硬编码 → `HPFC_ANDROID_SDK` env + `~/Library/Android/sdk` 默认
- `native.py` 未用的 `REPO_ROOT` import → 删
- `stages_adb.py` storage stage 死代码变量 `scoring_rows` → 删
- `reports.py::_safe_delta_pct` 两分支折叠成一个表达式
- `reports.py::write_hardware_baseline_csv` animation_scales 字面量元组 → 复用 `ANIMATION_SCALE_KEYS`
- `perfetto.py` `import math` 从函数内移到模块顶部
- `perfetto.py::TraceRecorder.stop()` kill 后 communicate 加 `timeout=5`
- `collectors.py::parse_data_filesystem` 把 bool 字段从 `str(bool)` 改成真 bool（与 `parse_zram` 一致）
- `collectors.py` `dumpsys procstats --hours 3` → `--hours 1`（省 3-15s）
- `cli.py::apply_preset` 删除未用的 `parser` 参数
- `cli.py` 新增 `_validate_presets_against_parser()` 启动 guardrail

---

## 三、Coverage — 采集 / 测量能力前后对照

### 新增采集（系统状态）
| 维度 | 前 | 后 |
|---|---|---|
| HWUI pipeline | 埋在 getprop 里 | `device_info.hwui.{hwui_renderer, hwui_use_vulkan, skia_pipeline, renderengine_backend}` |
| SELinux mode | 无 | `device_info.selinux_mode` |
| 前台应用 | 无 | `device_info.foreground_package` |
| Animation scales | 无 | `device_info.animation_scales.{window,transition,animator}_*_scale` |
| zram / swap | 无 | `device_info.mm.zram.{enabled,comp_algorithm,disksize_bytes,orig_data_bytes,compr_data_bytes,mem_used_bytes}` |
| MGLRU | 无 | `device_info.mm.mglru_enabled` |
| VM tuning | 无 | `device_info.mm.vm_tuning.{swappiness,dirty_ratio,…}` |
| Scheduler / EAS | 无 | `raw/proc_sys_kernel_sched.txt`, `raw/cpufreq_schedutil.txt` |
| GPU freq / governor | 无 | `device_info.gpu_freq.{gpu_cur_freq_raw,gpu_governor,gpu_freq_source_line}` |
| CPU idle 驻留 | 无 | `raw/cpu_idle_usage.txt` |
| UFS/eMMC 身份 | 无 | `raw/storage_identity.txt` |
| /data filesystem 类型 | 埋在 mount 里 | `device_info.storage.data_fs.{fstype,noatime,discard}` |
| Wi-Fi RSSI / link | 埋在 dumpsys wifi 里 | `device_info.wifi_link.{rssi_dbm,link_mbps,frequency_mhz}` |
| Vendor power mode | 无 | `device_info.vendor_power_mode`（MIUI/OPPO/vivo/honor getprop） |
| Game mode | 无 | `raw/dumpsys_game.txt` |
| Choreographer "Skipped N frames" | 无 | `raw/choreographer_log.txt` |
| Dalvik VM 配置 | 无 | `device_info.dalvik_vm.{heapsize,heapgrowthlimit,image_dex2oat_filter,...}` |

### 指标诚实化
| 指标 | 前 | 后 |
|---|---|---|
| CPU score | 25% 权重，shell hash 参与 | 改 10% 从 cpu 权重匀给 UI，shell hash 降级到 `crypto` 类不计分 |
| Memory score | 10% 权重，kernel syscall loop 参与 | shell memory loop 降级到 `baseline` 不计分 |
| Storage read | `sequential_read_mib_s`（误以为存储） | 改名 `buffered_read_mib_s`（page cache）+ advisory |
| Storage IOPS | 当作真实 IOPS | advisory + `shell-fork bound` |
| Thermal | active | advisory + `sha256 HW-accelerated, big cluster may not saturate` |
| Battery | `level_delta_pct` 方向反了 | `drain_pct = start - end, direction=lower` + avg_power_mw |
| Launch | "冷启动" | `advisory + semi-cold on non-root` |
| Interaction | 当作 tap-to-visible | `advisory + onDraw != visible` |
| Run score (1 device) | 永远 = 100 | `None` + note |
| compare_devices | ZeroDivisionError on jitter=0 | `_safe_delta_pct` 已修 |
| coverage_matrix | 写死 "automated" | 从实际 benchmark status 聚合 |
| perfetto duration | 硬编码 20s | `-t` 参数化，按 stage 需要 |

### 环境控制
| 变量 | 前 | 后 |
|---|---|---|
| 屏幕保活 | 无 | `stay_on_while_plugged_in=3` + `screen_off_timeout=1800s` + `KEYCODE_WAKEUP` + `wm dismiss-keyguard` |
| 动画 scale | 无 | 三个 scale 强制（默认 1.0，可 0.0）+ run 结束恢复 |
| 亮度 | 无 | `screen_brightness_mode=0` + 固定 128 |
| DND | 无 | opt-in `--dnd-on` |
| logcat 缓冲区 | 无 | `logcat -c` |
| 充电 gate | 无 | battery_gate 自动检查，charging 时 battery stage 变 advisory |
| 热身 | 无 | `--thermal-precool-sec` 可选 |

---

## 四、Deferred work（需要真机/根权限验证，下一轮做）

- **Native 随机 I/O helper**（`pread64/pwrite64` + `O_DIRECT` / per-4KiB fsync）替换 shell while+dd，拿真实 IOPS
- **UiAutomator fling** 替换 `adb shell input swipe`，消除 fork jitter，区分 fling vs drag
- **Perfetto FrameTimeline SQL 后处理**：用 `trace_processor` 查 `actual_frame_timeline_slice.jank_type` 分布
- **Interaction 用 SF `present_fence_time`** 取代 onDraw，做真 tap-to-visible
- **Launch 冷启动硬化**（userdebug/root 设备）：`drop_caches` + `cmd package compile --reset`
- **HTTP / DNS / RSSI 网络** 指标（ping 之外）
- **Thermal 压力换 native int worker pin-big**（从 advisory → active）
- **Baseline 跨 run 持久化**：`--baseline <summary.json>`
- **Multi-device parallel mode + 热串扰警告**
- **状态字符串收敛成 enum**（`StageStatus`/`MetricStatus`）
- **dumpsys 分节的 single-shell 批处理**（现在 60 次 adb roundtrip）

---

## 五、明确不在范围（用户指定跳过）

- WALT 硬件触控延迟
- 真实音频 round-trip（audiolat / OboeTester）
- 长期电池 Historian A/B
- 充电 0→100

---

## 六、文件改动

**新文件（3）：**
- `Tools/android_benchmark/env.py`（189 行，环境稳定模块）
- `Tools/android_benchmark/tests/__init__.py`
- `Tools/android_benchmark/tests/test_parsers.py`（17 测试）
- `Tools/android_benchmark/OVERNIGHT_REVIEW_RESULTS.md`（本文档）

**改动文件（11）：**
- `Tools/android_benchmark/collectors.py`
- `Tools/android_benchmark/cli.py`
- `Tools/android_benchmark/interaction.py`
- `Tools/android_benchmark/native.py`
- `Tools/android_benchmark/perfetto.py`
- `Tools/android_benchmark/README.md`
- `Tools/android_benchmark/reports.py`
- `Tools/android_benchmark/stages_adb.py`
- `Tools/android_benchmark/stages_app.py`
- `Tools/android_benchmark/system_trace.py`
- `Tools/android_benchmark_native/hpfc_native_bench.c`
- `Tools/perfetto.config`
- `Tools/run_android_comprehensive_benchmark.sh`

---

## 七、验证命令（你明早可以跑一遍复核）

```bash
# 单元测试（17 项）
cd /Users/chris/Code/HighPerformanceFriendsCircle
python3 -m unittest Tools.android_benchmark.tests.test_parsers -v

# smoke dry-run
python3 Tools/android_comprehensive_benchmark.py --dry-run --preset smoke

# multi-device deep dry-run
python3 Tools/android_comprehensive_benchmark.py --dry-run --serial A --serial B --preset deep

# 确认 user override 胜过 preset
python3 Tools/android_comprehensive_benchmark.py --dry-run --preset smoke --launch-iterations 99
# → summary.json.config.launch_iterations == 99

# 看 report
open Tools/results/android-comprehensive-runs/run-*/report.html
```

---

## 八、没 commit

按 CLAUDE.md，除非你明确要求 commit，我不会主动提交。所有改动都在 working tree，可以用 `git status` / `git diff` 随时回看。
