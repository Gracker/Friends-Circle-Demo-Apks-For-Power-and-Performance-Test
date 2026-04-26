---
title: "Android 全面 Benchmark 工具调研 & 对比方案"
date: 2026-04-24
status: final
type: research
tags: [Android, Benchmark, 性能测试, 对比评测, 硬件检测]
---

# Android 全面 Benchmark 工具调研 & 两机对比方案

## 背景与目标

手里两台手机需要做全面对比评测。已有启动速度和滑动流畅性测试方案，需要补齐其余维度。

**目标**：产出一份完整的 Benchmark 对比框架，后续基于此框架做一个一键跑完自动出对比数据的工具。只管做对，不管工作量。

---

## 一、现成可用的开源工具（按维度分类）

### 1. 硬件信息采集 & 对比

| 工具 | GitHub | 能力 | 可用性 |
|------|--------|------|--------|
| **SysInfo** | `kl3jvi/sysinfo_app` | CPU、RAM、OS、传感器、存储、电池、SIM、蓝牙、网络、Display、Camera、热管理，一站式全采集 | ⭐⭐⭐ MIT，Kotlin，活跃维护 |
| **DeviceInfo** | `ahmmedrejowan/DeviceInfo` | 硬件+传感器+软件详情，实时监控+硬件测试 | ⭐⭐⭐ Kotlin+Java |
| **cpu-info** | `kamgurgul/cpu-info` | CPU 规格详情+温度监控 | ⭐⭐ Kotlin，轻量 |

**结论**：硬件信息采集不需要重复造轮子。SysInfo 的数据结构最完整，可直接参考其采集逻辑，或用 `adb shell dumpsys` + `/proc` + `build.prop` 自建采集层。

### 2. CPU 计算性能

| 工具 | GitHub | 能力 | 可用性 |
|------|--------|------|--------|
| **OpenCL-Benchmark** | `ProjectPhysX/OpenCL-Benchmark` | FP64/FP32/FP16/INT64/32/16/8 全数据类型峰值算力 + 内存带宽 + PCIe 带宽 | ⭐⭐⭐ 支持 Android（NDK 编译），跨平台，数据维度最全 |
| **AndroidBenchmark** | `vnemes/AndroidBenchmark` | 整数运算、浮点运算、PI 计算（多线程）+ 排名系统 | ⭐⭐ 功能完整但年久失修（Java 时代） |
| **0xbench** | `josephcc/0xbench` | C 库系统调用、OpenGL ES、2D Canvas、GC、JS 引擎 | ⭐ TI 开发板标配，但 Dalvik 时代产物 |

**结论**：OpenCL-Benchmark 是目前最能打的开源 CPU/GPU 计算基准。直接跑 NDK 编译的 binary 即可，不依赖 App 壳。AndroidBenchmark 的排名思路可以借鉴。

### 3. GPU 渲染性能

| 工具 | 能力 | 来源 |
|------|------|------|
| **glmark2** | OpenGL ES 2.0+ 标准基准，多场景渲染评分 | `glmark2`（apt/brew 可装，Android 需 NDK 交叉编译）|
| **OpenCL-Benchmark** | GPU 计算+内存带宽（见上） | 同上 |
| **Android GPU Inspector (AGI)** | GPU Profiling，帧级分析 | `google/agi`，偏 debug 不偏 benchmark |

**结论**：GPU benchmark 领域开源工具稀缺。glmark2 是 OpenGL 标杆但需要交叉编译。实际项目中可以考虑直接调 `adb shell dumpsys gfxinfo` + `adb shell dumpsys SurfaceFlinger` 获取 GPU 渲染数据，配合 Perfetto Trace 做 Frame Timeline 分析。

### 4. 存储 I/O 性能

| 工具 | GitHub | 能力 | 可用性 |
|------|--------|------|--------|
| **CPDT** (Cross Platform Disk Test) | `maxim-saplin/CrossPlatformDiskTest` | 顺序读/写、随机读/写 MB/s + RAM 内存拷贝测试，支持 Android/Win/Mac | ⭐⭐⭐ 跨平台，结果可导出 CSV |
| **AndroidBenchmark** | `vnemes/AndroidBenchmark` | 固定 4KB buffer 读写，16×64MB 文件 | ⭐⭐ 简单粗暴 |
| **iotest** | `boxymoron/iotest` | 命令行，不同 buffer size 的顺序读写，输出 MB/s + IOPS | ⭐ 轻量 CLI |

**结论**：CPDT 是存储 benchmark 最靠谱的选择。Play Store 有上架，也可自行编译。随机读写是真实体验的关键指标，CPDT 覆盖了。

### 5. 内存性能

| 工具 | 能力 |
|------|------|
| **CPDT RAM Test** | 内存拷贝速度（MB/s） |
| **STREAM Benchmark** | 经典内存带宽基准（Triad/Copy/Scale/Add），需 NDK 编译 |
| **adb shell dumpsys meminfo** | 实时内存使用快照 |

**结论**：内存带宽用 STREAM（学术界标准）+ CPDT（应用层验证）。日常监控用 `dumpsys meminfo` 足够。内存泄漏检测不在此方案范围内（那是 App 侧的事）。

### 6. 触摸延迟 & 响应速度

| 工具 | GitHub | 能力 | 可用性 |
|------|--------|------|--------|
| **WALT** | `google/walt` | Tap 延迟、Drag 延迟、屏幕绘制延迟、音频延迟、MIDI 延迟——硬件级端到端测量 | ⭐⭐⭐ Google 出品，但需要 Teensy 硬件（~$20）|
| **touchpaint-android** | `kdrag0n/touchpaint-android` | 触摸延迟定性对比（画笔模式/全屏填充/方块跟随） | ⭐⭐ 无需硬件，可视化定性对比 |
| **latens** | `Orange-OpenSource/latens` | 触摸界面端到端延迟测量 | ⭐⭐ |

**结论**：WALT 是触摸延迟测量的金标准，但需要硬件。如果手头没有 WALT 设备，可以用 high-speed camera（240fps 手机录像）+ 触摸点帧数来算延迟。touchpaint 可作为定性对比的辅助工具。

### 7. FPS / 卡顿 / 滑动流畅性（你已有方案，这里列出补充工具）

| 工具 | 能力 |
|------|------|
| **Perfetto Frame Timeline** | 系统级帧时间分析，`actual_frame_timeline_slice` + `expected_frame_timeline_slice`，能区分 App jank / SF jank |
| **JankStats** | AndroidX 库，运行时 jank 统计上报 |
| **SoloX** | `smart-test-ti/SoloX`，Python 工具，实时采集 FPS/CPU/Memory/GPU/Battery/Thermal，支持双机 PK 模式 |
| **FrameX-Android** | `MaheshSharan/FrameX-Android`，基于 Shizuku 读 SurfaceFlinger FPS，零 overhead |

**结论**：你已有滑动测试方案。Perfetto SQL + Frame Timeline 可做深度帧分析（P50/P90/P95 帧时间分布）。SoloX 的双机 PK 模式值得借鉴——它同时采集两台设备数据并出对比报告。

### 8. 网络性能

| 工具 | 能力 |
|------|------|
| **Open-RMBT** | `rtr-nettest/open-rmbt-android`，多线程带宽测试 + QoS 测试，奥地利官方测速工具 |
| **Internet Speed Tester** | `ZohaibKhanDev/Internet-Speed-Tester`，Jetpack Compose，下载/上传/Ping/Jitter/丢包 |

**结论**：如果两台手机网络硬件相同（大概率），网络 benchmark 差异不大。但如果要测 WiFi 吞吐量差异，Open-RMBT 是最专业的开源方案。

### 9. 电池 & 功耗

| 工具 | 能力 |
|------|------|
| **Battery Historian** | `google/battery-historian`，分析 bugreport 中的电池事件时间线，**支持 A/B 对比** |
| **Wattson** | AOSP 内置，基于 Perfetto Trace 的硬件功耗估算 |
| **GamePulse** | 游戏场景电池监控 + 定时电池测试 |

**结论**：Battery Historian 的 A/B 对比功能直接命中需求。Wattson 配合 Perfetto 可做更细粒度的功耗分析。

### 10. 热管理 & 降频

| 工具 | 能力 |
|------|------|
| **Android Thermal Monitor** | `saschabrunner/Android-Thermal-Monitor`，overlay 显示 SoC 各组件温度 + CPU 频率，无需 root |
| **thermstat** | ADB 命令行工具，检测热降频（对比当前频率 vs 最高频率） |
| **AndroidCPUThrottling** | `hak/AndroidCPUThrottling`，直观展示 CPU 温度+频率随负载变化 |

**结论**：跑 CPU/GPU 压力测试时同时开启 Thermal Monitor，记录温度曲线和降频时间点。thermstat 适合命令行批量化。

### 11. 音频延迟

| 工具 | 能力 |
|------|------|
| **audiolat** | `chemag/audiolat`，ear-to-mouth 全链路音频延迟测量 |
| **SuperpoweredLatency** | `superpoweredSDK/SuperpoweredLatency`，round-trip 音频延迟 |
| **OboeTester** | Google AOSP，Oboe 库测试套件，含 round-trip 延迟测量 |

**结论**：音频延迟对用户体验影响大但常被忽视。audiolat 方案最自洽（无需外部硬件），可作为自动化测试项。

### 12. 显示器参数

| 指标 | 采集方法 |
|------|---------|
| 刷新率 | `adb shell settings get system peak_refresh_rate` + `adb shell dumpsys display` |
| 触控采样率 | 需 WALT 硬件或 high-speed camera 测量 |
| 分辨率/DPI | `adb shell wm size` + `adb shell wm density` |
| 色域/亮度 | `adb shell dumpsys display` 中的 DisplayDeviceInfo |

---

## 二、自研 Benchmark 工具方案设计

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    BenchmarkRunner                       │
│                   (Android App / Python)                 │
├─────────────┬──────────────┬──────────────┬─────────────┤
│  DeviceInfo │  PerfRunner  │  DataCollector│ ReportGen   │
│  Collector  │              │              │             │
├─────────────┼──────────────┼──────────────┼─────────────┤
│ Hardware    │ CPU Compute  │ FPS/Jank     │ JSON/CSV    │
│ Software    │ GPU Render   │ FrameTime    │ HTML Report │
│ Display     │ Storage IO   │ Input Latency│ Charts      │
│ Sensors     │ Memory BW    │ Thermal      │ Comparison  │
│ Network     │ Network      │ Battery      │ Score       │
│ Audio       │ Audio Latency│ Power        │ Ranking     │
└─────────────┴──────────────┴──────────────┴─────────────┘
```

### 全量对比维度 & 指标设计

#### A. 硬件基线（DeviceInfo）

| 维度 | 指标 | 采集方式 |
|------|------|---------|
| SoC | 芯片型号、制程、核心数、大/中/小核架构 | `Build.SOC_MODEL` + `/proc/cpuinfo` |
| CPU | 各核频率（min/max）、调度器 | `/sys/devices/system/cpu/cpu*/cpufreq/` |
| GPU | 型号、驱动版本、OpenGL ES/Vulkan 版本 | `adb shell dumpsys gfxinfo` |
| RAM | 总量、可用、类型（LPDDR4X/5） | `ActivityManager.MemoryInfo` + `/proc/meminfo` |
| ROM | 总量、可用、存储类型（UFS 3.1/4.0） | `StatFs` + `/proc/emmc` 或 `sysfs` |
| Display | 分辨率、刷新率、DPI、亮度范围 | `adb shell dumpsys display` |
| Battery | 容量（mAh）、充电功率 | `adb shell dumpsys batteryproperties` |
| Network | WiFi 标准（WiFi 5/6/7）、蓝牙版本、蜂窝基带 | `Build.RADIO` + `adb shell iwconfig` |
| Sensors | 传感器列表（加速度计/陀螺仪/气压计等） | `SensorManager.getSensorList()` |
| Audio | 采样率、缓冲区大小、低延迟支持标记 | `AudioManager.getProperty()` |

#### B. 计算性能（PerfRunner）

| 测试项 | 具体指标 | 参考实现 |
|--------|---------|---------|
| CPU 单核整数 | MIPS / 时间（ms） | AndroidBenchmark 的 ALU 测试 |
| CPU 单核浮点 | MFLOPS / 时间（ms） | AndroidBenchmark 的 FPU 测试 |
| CPU 多核 | 多线程 PI 计算 / 并行矩阵运算 | AndroidBenchmark PI 多线程 |
| CPU 多核扩展性 | 核数-性能曲线（1/2/4/8 核） | 自建：逐步开核跑同一任务 |
| GPU 计算峰值 | FP32/FP16 TFLOPS | OpenCL-Benchmark |
| GPU 内存带宽 | 合并读/写、非对齐读/写 GB/s | OpenCL-Benchmark |
| 内存带宽 | Copy/Scale/Add/Triad GB/s | STREAM benchmark（NDK） |
| 内存延迟 | 随机访问延迟 ns | 自建：pointer chasing |

#### C. 存储性能（PerfRunner）

| 测试项 | 具体指标 | 参考实现 |
|--------|---------|---------|
| 顺序读 | MB/s（1MB block） | CPDT |
| 顺序写 | MB/s（1MB block） | CPDT |
| 随机读 | IOPS + MB/s（4KB block） | CPDT |
| 随机写 | IOPS + MB/s（4KB block） | CPDT |
| 混合读写 | MB/s（70%读30%写） | 自建 |
| Append 写入 | MB/s | 自建 |
| SQLite 插入/查询 | ops/s | 自建：标准化表结构+索引 |

#### D. 交互性能（DataCollector）

| 测试项 | 具体指标 | 采集方式 |
|--------|---------|---------|
| **启动速度**（你已有） | 冷启TTID/TTFD、温启、热启 | 已有方案 |
| **滑动流畅性**（你已有） | FPS、掉帧率、帧时间分布 | 已有方案 |
| 触摸响应延迟 | Tap latency（ms）、Drag latency（ms） | WALT 硬件 或 高速摄像 |
| 点击响应时间 | ACTION_DOWN → 视觉反馈（ms） | 高速摄像 / `InputDispatcher` trace |
| 页面切换速度 | 过渡动画起始→完成（ms） | `adb screenrecord` + 逐帧分析 |
| 键盘弹出延迟 | 焦点获取→键盘完全展开（ms） | `adb screenrecord` + 逐帧分析 |

#### E. 流畅性深度分析（DataCollector）

| 指标 | 采集方式 |
|------|---------|
| P50/P90/P95/P99 帧时间 | Perfetto `android.frames.timeline` SQL |
| 大帧（>32ms / >50ms）数量 | Perfetto SQL 统计 |
| App jank vs SF jank vs 合成 jank | Frame Timeline `jank_type` 分类 |
| 帧节奏波动（cadence discrepancy） | 相邻帧时间标准差 |
| 滑动曲线分析（位移-时间-速度） | MotionEvent 序列 + 插值 |

#### F. 热管理 & 降频（DataCollector）

| 测试项 | 指标 | 采集方式 |
|--------|------|---------|
| 空闲温度 | 各传感器基线温度（°C） | Thermal HAL `/sys/class/thermal/` |
| 满载温度曲线 | 温度随时间变化（持续 CPU+GPU 压力） | Thermal Monitor 模式，每秒采样 |
| 降频起始时间 | 从开始满载到首次降频的时间（s） | 对比实时频率 vs 最大频率 |
| 降频幅度 | 降频后频率占最大频率的比例 | 频率曲线 |
| 恢复时间 | 停止负载后恢复到最大频率的时间（s） | 持续监控 |

#### G. 电池 & 功耗（DataCollector）

| 测试项 | 指标 | 采集方式 |
|--------|------|---------|
| 静态功耗 | 待机 1h 电量消耗（%） | `dumpsys battery` 定时采样 |
| 视频播放功耗 | 播放 1h 视频电量消耗（%） | 标准化测试视频+定时采样 |
| 游戏功耗 | 游戏 30min 电量消耗（%） | 标准化游戏场景 |
| 充电速度 | 0→100% 充电曲线（% / min） | 充电过程定时采样 |
| 电流/电压 | 实时电流（mA）、电压（mV） | `dumpsys batteryproperties` |

#### H. 网络性能（PerfRunner）

| 测试项 | 指标 |
|--------|------|
| WiFi 下载吞吐量 | Mbps |
| WiFi 上传吞吐量 | Mbps |
| WiFi 延迟 | ms（Ping） |
| WiFi Jitter | ms |
| 蜂窝下载/上传 | Mbps |
| 蓝牙传输速度 | MB/s |

#### I. 音频性能（PerfRunner）

| 测试项 | 指标 | 采集方式 |
|--------|------|---------|
| Round-trip 延迟 | ms | audiolat（ear-to-mouth） |
| 输出延迟 | ms | `AudioManager.getProperty(PROPERTY_OUTPUT_SAMPLE_RATE)` 估算 |
| 低延迟支持 | boolean | `PROPERTY_OUTPUT_LOW_LATENCY` |
| 采样率 | Hz | `PROPERTY_OUTPUT_SAMPLE_RATE` |
| 缓冲区大小 | frames | `PROPERTY_OUTPUT_FRAMES_PER_BUFFER` |

#### J. 系统级指标（DataCollector）

| 测试项 | 指标 | 采集方式 |
|--------|------|---------|
| App 安装速度 | APK 安装时间（ms） | `adb install` + 计时 |
| 系统启动时间 | 开机动画→Launcher 可用（ms） | `adb shell bootchart` 或 logcat `sys.boot_completed` |
| 编译速度 | odex/编译时间 | `cmd package compile` + 计时 |
| Zygote 预加载 | 预加载完成时间 | logcat `Zygote` tag |
| GC 暂停 | 平均暂停时间 / 最大暂停时间 | Perfetto `art_gc` slice |

---

## 三、对比报告输出设计

### 报告结构

```
BenchmarkReport_2026-04-24/
├── summary.json              # 全量结构化数据
├── report.html               # 可视化报告（含对比图表）
├── details/
│   ├── hardware_baseline.csv # 硬件参数对比
│   ├── cpu_compute.csv       # CPU 计算成绩
│   ├── gpu_compute.csv       # GPU 计算成绩
│   ├── storage_io.csv        # 存储读写成绩
│   ├── memory_bw.csv         # 内存带宽
│   ├── fps_jank.csv          # 帧时间分布
│   ├── thermal_curve.csv     # 温度曲线
│   ├── battery_drain.csv     # 电池消耗
│   ├── network.csv           # 网络成绩
│   └── audio.csv             # 音频延迟
└── traces/                   # 原始 Perfetto traces（可选归档）
```

### 评分体系设计（参考安兔兔但更合理）

| 大项 | 权重 | 子项 |
|------|------|------|
| CPU 性能 | 25% | 单核整数+浮点、多核扩展性 |
| GPU 性能 | 20% | 计算峰值、渲染帧率 |
| 存储性能 | 15% | 顺序+随机读写 |
| 内存性能 | 10% | 带宽+延迟 |
| 交互体验 | 15% | 启动速度、滑动流畅性、触摸延迟 |
| 热管理 | 10% | 降频时间+幅度 |
| 续航 | 5% | 标准化场景功耗 |

每项满分 100，加权汇总。评分公式参考 Geekbench 的几何平均数而非算术平均数（避免单项极端值拉偏总分）。

### 对比可视化方案

1. **雷达图**：各维度分数对比（一眼看出谁强谁弱）
2. **柱状图**：单项指标对比（精确数值）
3. **折线图**：温度曲线、帧时间曲线、电池消耗曲线（时间维度对比）
4. **表格**：硬件参数详细对比
5. **Verdict**：每项标注胜出方 + 百分比差异

---

## 四、技术实现建议

### 方案选择：Python + ADB（推荐）

理由：
- SoloX 已验证 Python + ADB 方案的可行性
- 无需在手机端装 App，降低用户门槛
- 两台手机同时连接电脑，统一调度采集
- 报告生成（HTML + Chart.js / Matplotlib）在 PC 端完成

### 核心依赖

```python
# 性能数据采集
adb_shell          # ADB 通信
solox              # FPS/CPU/Memory/GPU/Battery/Thermal 实时采集

# 报告生成
jinja2             # HTML 模板
plotly / matplotlib # 图表
pandas             # 数据处理

# 存储测试
# 直接通过 adb shell 跑 dd / fio 命令
```

### 实现步骤（优先级排序）

| 优先级 | 模块 | 工作量 | 依赖 |
|--------|------|--------|------|
| P0 | DeviceInfo Collector（硬件基线） | 1天 | adb shell |
| P0 | 对比报告框架（HTML + 雷达图 + 表格） | 2天 | jinja2 + plotly |
| P0 | 集成已有启动+滑动测试 | 1天 | 已有代码 |
| P1 | 存储IO测试（dd/fio via adb） | 1天 | adb shell |
| P1 | CPU/GPU 压力测试 + 降频检测 | 2天 | adb + stress-ng |
| P1 | FPS/Jank 实时采集 + Perfetto SQL 分析 | 2天 | solox + perfetto |
| P1 | 温度曲线采集 | 0.5天 | adb shell thermal |
| P2 | 电池标准化测试 | 1天 | adb shell |
| P2 | 网络测试 | 1天 | speedtest-cli 或 iperf3 |
| P2 | 音频延迟测试 | 1天 | audiolat APK |
| P3 | 评分体系 + 排名系统 | 2天 | pandas |
| P3 | 一键式 Runner（串联所有测试） | 1天 | python |

---

## 五、参考项目源码索引

| 项目 | GitHub 地址 | 用途 |
|------|-------------|------|
| SoloX | `smart-test-ti/SoloX` | 双机实时性能采集 PK 模式（Python） |
| CPDT | `maxim-saplin/CrossPlatformDiskTest` | 存储 I/O benchmark 参考 |
| OpenCL-Benchmark | `ProjectPhysX/OpenCL-Benchmark` | CPU/GPU 计算峰值 benchmark |
| WALT | `google/walt` | 触摸/音频延迟金标准（需硬件） |
| touchpaint | `kdrag0n/touchpaint-android` | 触摸延迟定性对比 |
| Battery Historian | `google/battery-historian` | 电池 A/B 对比 |
| Android Thermal Monitor | `saschabrunner/Android-Thermal-Monitor` | 温度监控 overlay |
| audiolat | `chemag/audiolat` | 音频延迟测量 |
| SysInfo | `kl3jvi/sysinfo_app` | 硬件信息采集参考 |
| AndroidBenchmark | `vnemes/AndroidBenchmark` | CPU benchmark + 排名系统参考 |
| FrameX-Android | `MaheshSharan/FrameX-Android` | FPS 采集（Shizuku 方案） |
| 0xbench | `josephcc/0xbench` | 系统级 benchmark 参考（历史） |
| open-rmbt-android | `rtr-nettest/open-rmbt-android` | 网络测速参考 |
| Android performance-samples | `android/performance-samples` | Macrobenchmark 官方示例 |
