# Android Comprehensive Benchmark

工程化入口：

```bash
Tools/run_android_comprehensive_benchmark.sh
```

不传 `--serial` 时会自动发现所有 `adb devices -l` 里处于 `device` 状态的手机，并逐台运行。传多台设备时重复 `--serial`：

```bash
Tools/run_android_comprehensive_benchmark.sh \
  --serial DEVICE_1 \
  --serial DEVICE_2
```

输出目录默认在：

```text
Tools/results/android-comprehensive-runs/run-YYYYmmdd-HHMMSS/
```

关键输出：

- `summary.json`：全量结构化结果（`schema_version: 1.1`）
- **`ai_report.md`：LLM-friendly 对比报告**，包含 device profiles、config diffs、per-category ASCII bar 对比、auto-findings、advisory inventory；典型 ~95 行（单设备）/ ~210 行（双设备），可直接粘到 ChatGPT/Claude 让 LLM 分析
- `report.md` / `report.html`：含 **Run Health** + **Measurement Caveats** + **Per-category 对比 SVG bar** 的可读报告（HTML 版本对比设备并排显示，按 weight 排序）
- `details/hardware_baseline.csv`：跨设备硬件基线（含 SELinux / HWUI / data_fs / zram / MGLRU 等）
- `details/comparison.csv`：跨设备指标对比，带 `status` 与 `advisory_reason`
- `devices/<device>/details/getprop.csv`：完整 `adb shell getprop` 键值表
- `devices/<device>/raw/getprop.txt`：原始 getprop 输出
- `devices/<device>/raw/selinux.txt`、`sys_zram_snapshot.txt`、`proc_sys_vm.txt`、`mglru.txt`、`proc_sys_kernel_sched.txt`、`cpufreq_schedutil.txt`、`gpu_freq.txt`、`cpu_idle_usage.txt`、`storage_identity.txt`、`dumpsys_game.txt`、`foreground_state.txt` 等系统状态快照
- `devices/<device>/details/proc_index.csv`：已采集 `/proc` 文件索引
- `devices/<device>/details/native_cpu.csv`：自研 native 整数/浮点多线程 benchmark 明细（含 `pinned_cpu`, `sched_fifo_applied`）
- `devices/<device>/details/native_memory.csv`：自研 STREAM-like 内存带宽（warmup-dropped median）和 pointer chasing 延迟
- `devices/<device>/details/click_response.csv`：自研点击响应多轮结果
- `devices/<device>/interaction/traces/*.ptrace`：每次点击响应对应的 Perfetto trace（duration 按需）
- `devices/<device>/system_trace/*.ptrace`：CPU+I/O mixed 场景通用 Perfetto trace

## Measurement caveats — 每个指标的语义

这是一份诚实清单，方便性能工程师判断 score 能信多少：

### CPU
| 指标 key | status | 说明 |
| --- | --- | --- |
| `native.cpu_int_*` / `cpu_fp_*` | active | 真实 CPU 计算，单线程自动 pin 到高频核；`--native-sched-fifo` 可选 SCHED_FIFO |
| `gb.aes_round_mib_s` | active | AES round 变换（S-box+ShiftRows+MixColumns）on 16B blocks，软件实现，不走 ARMv8 crypto 指令 |
| `gb.sha256_mib_s` | active | 软件 SHA-256（不走 `sha2` 扩展指令）；与 `crypto.hash_*` 区别开 |
| `gb.fft_mflops` | active | 4096-point Cooley-Tukey radix-2 |
| `gb.matmul_mflops` | active | 256×256 单精度浮点 GEMM |
| `gb.nbody_mflops` | active | 256-body Euler n-body |
| `gb.sort_mops` | active | qsort 1M `uint32_t` |
| `crypto.hash_*` | advisory | ARMv8 `sha256` HW 指令；HW crypto + kernel memcpy，不是 CPU 分数 |
| `baseline.kernel_loop_mib_s` | advisory | `dd /dev/zero -> /dev/null`，syscall loop，不触 DRAM |

### Memory
| 指标 key | status | 说明 |
| --- | --- | --- |
| `native.memory_*` | active | STREAM-like，warmup 一轮丢弃，best+median，`mlock` 防 page-fault |
| `native.memory_latency_ns` | active | 64-byte stride pointer chasing |
| `memory_pressure.max_resident_mb` | active | 渐进 mmap 直到被 LMK；OEM 相关，advisory_reason 解释 |
| `memory_pressure.lmkd_kills_during` | active | 期间观察到的 lmkd kill 行数 |

### GPU / AI / Video
| 指标 key | status | 说明 |
| --- | --- | --- |
| `gpu.glmark2_score` | active | glmark2-es2 的总分（需要用户先 push binary，否则 stage 自动 skipped） |
| `ai.best_inference_avg_us` | active | TFLite benchmark_model 在 cpu/xnnpack/gpu/nnapi 中最佳的平均推理 µs（lower better） |
| `ai.accel_speedup` | active | `cpu_avg_us / best_avg_us`，加速器相对 CPU 的加速比 |
| `video.encoder_mb_per_s` | advisory | `screenrecord` 编码 wall-clock 估算，不是真 fps |

### UX (AnTuTu-style)
| 指标 key | status | 说明 |
| --- | --- | --- |
| `ux.regex_mib_per_s` | active | 朴素 substring 扫描，无 PCRE，跨 ASCII 文本 |
| `ux.json_tokenize_mib_per_s` | active | JSON-ish 字符流的逐字符 tokenize 速率 |
| `ux.data_sort_mops` | active | 500K `uint32_t` qsort |
| `ux.image_mpix_out_per_s` | active | 5x5 separable Gaussian blur + 双线性 2x 缩小 + sepia color convert，输出像素率 |

### Storage
| 指标 key | status | 说明 |
| --- | --- | --- |
| `storage.seq_write_mib_s` | active | `conv=fsync` 末尾一次刷盘 |
| `storage.random_*_iops_native` | active | `pread64/pwrite64` + O_DIRECT（被拒则 fallback fsync）：真随机 IOPS |
| `storage.buffered_read_mib_s` | advisory | 读的是刚写完的同一个文件，几乎全命中 page cache |
| `storage.random_*_iops` | advisory | 旧 shell `while + dd` 循环；IOPS 被 fork 开销封顶，保留作为对照 |

### UI perf
| 指标 key | status | 说明 |
| --- | --- | --- |
| `scroll.*` | active | 透传 `auto_record_perfetto_*.py` / `perfetto_release_scroll_benchmark.py`；`input swipe` 抖动仍存在 |
| `launch.best_avg_ms` | active 或 advisory | 检测到 root → drop_caches + compile-reset → active；否则 advisory（semi-cold） |
| `interaction.dispatch_median_ms` | active | MotionEvent 内核 → Activity 分发耗时 |
| `interaction.on_draw_*` / `frame_*` | advisory | onDraw 是 draw-command-start，不等于可见；真值需要 SF `present_fence_time`（deferred） |

### Sustained / 热 / 电池 / 网络 / 安装
| 指标 key | status | 说明 |
| --- | --- | --- |
| `sustained.stability_ratio` | active | `min/max round duration`，越接近 1 越稳定 |
| `sustained.drift_pct` | active | round-1 → round-N 漂移百分比，越小越好 |
| `thermal.*` | advisory | 压力仍是 `sha256sum`（HW 加速），大核不一定满载 |
| `battery.drain_pct` | active | 方向 lower-is-better；充电中自动 advisory |
| `battery.avg_power_mw` | active | `|current_now| × |voltage_now|`，符号 OEM 相关 |
| `network.ping_*` | active | ICMP 单一主机；不是 TCP/HTTP/DNS |

`compute_scores` 只使用 `status=active` 且 `valid_for_score=true` 的指标。跨设备用 min-max 归一化；单设备运行时 `total=None`（不再虚报 100）。

## Score buckets（AnTuTu-style 总分）

权重见 `reports.SCORE_WEIGHTS`：

```
cpu(22) + gpu(12) + memory(10) + memory_pressure(4) + storage(8)
+ ux_data(5) + ux_image(5)
+ scroll(13) + launch(8) + interaction(6)
+ sustained(4) + ai(6) + thermal(3) + battery(2) + network(1) + install(1)
= 110
```

每个 metric 在它所属的 category 内归一（最佳设备=100），category 内 metric 取均值，再按上面权重加权汇总。`crypto` / `baseline` / `video` 是 advisory-only category，不计入 score 但仍在 report 显示。

## Environment stabilization

默认会自动：

- `svc power stayon usb` + `stay_on_while_plugged_in=3` + 30 分钟 screen-off 超时
- `KEYCODE_WAKEUP` + `wm dismiss-keyguard`
- 三个 animation scale 强制成 `--animation-scale`（默认 1.0，可 `--animations-off` 归零）
- `screen_brightness_mode=0` + `screen_brightness=--screen-brightness`（默认 128）
- `logcat -c`

DND 不会被默认修改；要打开就加 `--dnd-on`。全部 `settings` 在 run 结束时恢复原值（原来为空会 `settings delete`，不会留 `null`）。

充电状态下运行 `battery` stage 自动标 `advisory`（除非 `--allow-charging-battery-stage`）。

可选 `--thermal-precool-sec N --thermal-precool-ceiling-c 42` 在开始前等最大热区降到阈值。

## Presets

```
--preset smoke      # 端到端 <5 min：所有 iterations/durations 砍到最低
--preset standard   # 当前默认值（原有行为）
--preset deep       # iterations/durations ~2x
```

Preset **只填充用户没有显式传入的 flag**。`--preset smoke --launch-iterations 10` 会保留 10。

## Multi-device 对比

连多台手机（USB hub 或 Wi-Fi adb），不传 `--serial` 自动发现并依次跑：

```bash
Tools/run_android_comprehensive_benchmark.sh
# 自动跑 adb devices 里所有 device 状态的手机
```

并行跑（~2x 速度，但有 thermal cross-talk 风险）：

```bash
Tools/run_android_comprehensive_benchmark.sh --parallel-devices 2
```

⚠️ **并行警告**：手机靠近时互相加热可能让 thermal/sustained 数据失真；USB hub 共享带宽可能让 storage IO 数据偏低。如果对比的是 thermal 或 IO，建议用顺序模式或物理隔离设备。

跑完产出会自动包含：

- `ai_report.md`：双设备时含 config diff 表、per-category ASCII bar、auto-findings（如"graphics pipeline differs, may explain scroll gap"）
- `report.html`：含 per-category 并排对比柱图 + config diff 表
- `details/comparison.csv`：每 metric 的 raw winner + delta_pct
- `details/hardware_baseline.csv`：跨设备硬件差异表

### 给 LLM 的工作流

```bash
# 跑完后
cat results/android-comprehensive-runs/run-*/ai_report.md | pbcopy
# 粘到 ChatGPT/Claude，问"哪台手机 UI 更好，原因是什么"
```

`ai_report.md` 设计目标是 token-efficient（即使双设备也只 ~210 行，~10KB），且每个 metric 旁边都附了 unit + direction + advisory_reason，LLM 不需要外部上下文就能理解。

## 说明：分数

- **只连一台手机时**：`scores` 里 `total=None`，报告会显示绝对指标表而非虚假的 100 分。
- **两台或多台一起跑**：每个 metric 最好设备 = 100，其他按比例。权重见 `reports.SCORE_WEIGHTS`（scroll 20 / launch 15 / interaction 15 / cpu 15 / memory 10 / storage 10 / thermal 8 / battery 3 / network 2 / install 1；`crypto` 与 `baseline` 不计分）。

## 已实现模块

- `adb.py`：ADB 封装和设备发现
- `collectors.py`：硬件、getprop、/proc、dumpsys、thermalservice 采集，新增 SELinux / HWUI / 动画 scale / 前台应用 / zram / MGLRU / 调度器 / schedutil / GPU freq / UFS 身份 / wifi RSSI 等
- `env.py`：环境稳定（stayon、brightness、animation scales、zen_mode、logcat），`thermal_precool`，`read_battery_gate`
- `stages_adb.py`：CPU、内存、存储、热、短时电池、网络、安装速度（advisory 标签已贯穿）
- `native.py` + `Tools/android_benchmark_native/`：自研 Geekbench-like native workload，int MOPS 已修正（×5 ops/iter），STREAM 有 warmup + median + best、可选 SCHED_FIFO、单线程 pin 到 big cluster
- `interaction.py` + `benchmark-interaction/`：自研点击响应 APK 和自动化，trace duration 按 tap 周期
- `system_trace.py`：通用 Perfetto mixed CPU/I/O trace，duration 从 CLI 参数生效
- `perfetto.py` / `perfetto.config`：`duration_ms` 不再硬编码，交给 caller；buffer 从 62MiB → 256MiB；atrace 加 `hwui`；sys_stats 250ms
- `stages_app.py`：接入已有 launch / scroll Perfetto benchmark，launch 默认标 advisory
- `reports.py`：metric 扁平化（含 status/advisory_reason/valid_for_score）、跨设备对比（safe delta_pct、within-noise-ready）、评分（单设备不虚报分、权重重平衡）、Run Health 块、Markdown/HTML 报告
- `cli.py`：命令行参数、preset、env 包裹、output-dir 安全、validate_args 扩展

## 快速验证

```bash
Tools/run_android_comprehensive_benchmark.sh \
  --dry-run \
  --preset smoke
```

## 只跑自研 native + 点击响应 smoke

```bash
Tools/run_android_comprehensive_benchmark.sh \
  --skip-launch \
  --skip-scroll \
  --preset smoke
```

## 采集完整 bugreport

```bash
Tools/run_android_comprehensive_benchmark.sh --collect-bugreport
```

## 只跑纯 ADB 阶段

```bash
Tools/run_android_comprehensive_benchmark.sh --skip-app-benchmarks
```

## 只验证 launch / scroll / interaction（sanity）

```bash
Tools/run_android_comprehensive_benchmark.sh \
  --skip-pure-adb --skip-scroll \
  --launch-iterations 1 --launch-apps launch-aosp --launch-flavors light
```

```bash
Tools/run_android_comprehensive_benchmark.sh \
  --skip-pure-adb --skip-launch \
  --scroll-trace-seconds 3 --scroll-loads minimal --scroll-only-apk scrolling-aosp-performance
```

## Tier 2 — 需要外部 binary（用户 push 后自动生效）

stage 在 `/data/local/tmp` 找到 binary 就跑，找不到 status=skipped 并附 hint。

### GPU — glmark2-es2

```bash
# 在 Linux 主机上 build glmark2-es2 for Android (https://github.com/glmark2/glmark2)
adb push glmark2-es2 /data/local/tmp/glmark2-es2
adb shell chmod 755 /data/local/tmp/glmark2-es2
```

### NPU/AI — TFLite benchmark_model + 模型

```bash
# 下载 prebuilt: https://www.tensorflow.org/lite/performance/measurement
adb push benchmark_model /data/local/tmp/benchmark_model
adb shell chmod 755 /data/local/tmp/benchmark_model
adb push mobilenet_v2_1.0_224_quant.tflite /data/local/tmp/mobilenet_v2.tflite
```

工具会依次尝试 `cpu` / `xnnpack` / `gpu` / `nnapi` 四个 delegate 并报告各自的平均推理 µs，附上加速比 `cpu_us / best_us`。

## Tier 3 — Deferred (下次迭代)

需要新建 APK 或大改子脚本，今晚没动：

- **AnTuTu UX "Multitasking & Daily Use"**：UiAutomator 编排开 N 个 app + 切换。
- **Tap-to-visible Perfetto SQL**：`trace_processor` + SF `present_fence_time` 关联，把 interaction `onDraw` 从 advisory → active。
- **UiAutomator fling 替换 `adb shell input swipe`**：消除 fork 抖动，分开 fling / drag。
- **Perfetto FrameTimeline SQL 后处理**：`actual_frame_timeline_slice.jank_type` 分布、missed-vsync 计数。
- **HTTP / DNS / RSSI 网络指标**：补 ping 之外的链路信号。
- **Thermal 压力换成 native int worker pin-big**：thermal stage 从 advisory → active。
- **Baseline 跨 run 持久化**：`--baseline <summary.json>`。
- **Per-device parallel mode + 热串扰警告**：多机同跑时可选。
- **更真实的 video decode**：MediaCodec test APK + Perfetto trace（现在 `screenrecord` 探针只是 advisory）。

## 明确不在范围（scope rejected）

- WALT 级硬件触控延迟、高速摄像机触控
- 真实音频 round-trip（audiolat / OboeTester）
- 长时间电池 Historian A/B（batterystats --checkin 可采，但 A/B 不跑）
- 充电 0 → 100 曲线
- 系统 boot time（需要 reboot 编排，出报告困难）
