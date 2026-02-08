# HighPerformanceFriendsCircle 重构计划

**日期**: 2026-02-07
**版本**: 1.0
**当前版本**: v1.2.0
**模块数量**: 31 (28 app + 3 library)

---

## Completion Status (2026-02-08)

| Phase | Status | Summary |
|-------|--------|---------|
| Phase 1 | DONE | 10+ critical/high bug fixes across app, load-config, automation-test |
| Phase 2 | DONE | Version Catalog (libs.versions.toml), shared signing config, all 31 modules migrated |
| Phase 3 | DONE | scrolling-common library extracted (beans, interfaces, widgets, utils, resources) |
| Phase 4 | DONE | Context leak fixes (WeakReference), DiffUtil adoption (7 adapters), reflection elimination (16 adapters, StatusBar) |
| Phase 5 | DONE | shell=True→shlex, subprocess leak fixes, dead code removal, warmup/iterations for scrolling, install_apks.sh reads from registry |
| Phase 6 | DONE | AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, Convention Plugin (build-logic), Configuration Cache enabled |

---

## 总览

基于 5 个维度的全面 Review（架构、代码质量、构建系统、模块一致性、自动化测试），本文档汇总了所有发现并提出分阶段重构计划。

### 问题统计

| 严重度 | 数量 | 来源 |
|--------|------|------|
| Critical | 7 | 代码质量(2) + 构建系统(3) + 架构(2) |
| High | 16 | 代码质量(8) + 构建系统(5) + 架构(3) |
| Medium | 30+ | 分布在所有 5 个维度 |
| Low | 15+ | 风格、命名、文档 |

---

## Phase 1: 紧急修复（Quick Wins - 低工作量、高影响）

> 预计工作量：2-3 天

### 1.1 [Critical] 修复 UncaughtExceptionHandler 链断裂

**文件**: `app/src/main/java/.../FriendsCircleApplication.java:23-27`

当前代码调用 `Thread.getDefaultUncaughtExceptionHandler()` 作为 getter 而未转发异常，导致：
- 崩溃被静默吞掉
- 原有 handler（如 Firebase Crashlytics）永远不会收到异常
- App 卡死而非崩溃

```java
// 修复方案
Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    Log.e("FriendsCircleApp", "Uncaught exception in " + thread.getName(), throwable);
    if (prev != null) prev.uncaughtException(thread, throwable);
});
```

### 1.2 [Critical] 修复 VerticalCommentWidget 共享 LayoutParams

**文件**: `app/src/main/java/.../VerticalCommentWidget.java:197-205`

单个 `LayoutParams` 实例被所有子 View 共享并反复修改 `bottomMargin`，导致所有评论间距错误。

**修复**: 每次调用 `generateMarginLayoutParams()` 创建新实例。

### 1.3 [Critical] 修复 FPS 计算错误

**文件**: `automation-test/lib/utils.py (FPSAnalyzer.parse_gfxinfo)`

当前 `avg_fps` 使用 `target_fps * (1 - janky_percent/100)` 近似计算，数学上是错误的。

**修复**: 使用实际帧时间计算 `avg_fps = 1000.0 / mean(frame_durations)`

### 1.4 [High] 修复 LoadConfig 常量不一致

**文件**: `load-config/src/main/java/.../LoadConfig.java:180,191,202`

文档注释声称"2.25x"但实际值为 1.5x，导致 `validateConfig()` 永远返回 false。

**修复**: 将 Heavy 强度值校正为 2250（或更新注释为 1.5x）。

### 1.5 [High] 修复 SimpleWeakObjectPool 数组越界

**文件**: `app/src/main/java/.../SimpleWeakObjectPool.java:25-30`

`curPointer > objsPool.length` 应为 `>=`，且缺少 null 检查。

### 1.6 [High] 修复 DataCenter 线程安全

**文件**: `app/src/main/java/.../DataCenter.java`

- `java.util.Random` 不是线程安全的 → 替换为 `ThreadLocalRandom`
- `emojiDataSources` ArrayList 并发读写 → 使用 `CopyOnWriteArrayList` 或同步

### 1.7 [High] 修复 RxJava Disposable 泄漏

**文件**: `app/src/main/java/.../MainActivity.java`

多次刷新时前一个 Disposable 未被 dispose → 使用 `CompositeDisposable`

### 1.8 [High] 修复 onCreateViewHolder 返回 null

**文件**: `app/src/main/java/.../FriendCircleAdapter.java:105`

未知 viewType 返回 null 导致 NPE → 抛出 `IllegalArgumentException`

### 1.9 [P0-Security] 修复 Activity exported 属性

**文件**: scrolling-webview, scrolling-gl-map, scrolling-surface-map 等 AndroidManifest.xml

10+ 个子 Activity 不必要地设置 `exported="true"` → 改为 `false`

### 1.10 [High] 修复 Constants.BLUE 颜色错误

**文件**: `app/src/main/java/.../Constants.java:25`

`BLUE = "#ff0000"` — 命名为蓝色但值是红色。

---

## Phase 2: 构建系统现代化（Medium Effort, High Impact）

> 预计工作量：3-5 天

### 2.1 创建 Version Catalog (`libs.versions.toml`)

**影响**: 消除 40+ 处版本声明散布在 32 个 build.gradle 文件中的问题

当前版本漂移：
- appcompat: 1.6.1 vs 1.7.1
- material: 1.11.0 vs 1.12.0
- constraintlayout: 2.1.4 vs 2.2.1
- compose-bom: 2023.08 vs 2024.02
- coroutines: 1.7.1 vs 1.7.3

**实施步骤**:
1. 创建 `gradle/libs.versions.toml`
2. 在 `settings.gradle` 添加 `dependencyResolutionManagement`
3. 逐模块替换版本声明为 `libs.xxx` 引用
4. 统一所有依赖到最新稳定版

### 2.2 统一签名配置

**影响**: 3 种不同签名模式 → 1 种

创建 `gradle/signing.gradle` 共享脚本，所有 28 个 app 模块 `apply from`。

### 2.3 统一 Java/Kotlin 兼容版本

**影响**: 消除 VERSION_1_8 / VERSION_11 / VERSION_17 混用

统一到 `VERSION_17`（与 CI JDK 17 匹配）。

### 2.4 清理 gradle.properties

- 移除 `android.enableJetifier=true`（所有依赖已 AndroidX，节省 10-15% 构建时间）
- 移除 `app/build.gradle` 中 force kotlin-stdlib 1.8.22（与 Kotlin 1.9.22 不匹配）
- 修复 Kotlin 1.9.22 / Compose Compiler Plugin 2.0.0 版本不匹配

### 2.5 统一 minSdk

`app` 和 `load-config` 使用 minSdk 21，其余 27 个模块使用 24 → 统一到 24。

### 2.6 简化 CI APK 收集

替换 `android.yml` 中 60+ 行的逐文件 copy 为 `find` 循环模式（`release.yml` 已使用此模式）。

---

## Phase 3: 架构重构 - 消除代码重复（High Effort, High Impact）

> 预计工作量：1-2 周

### 3.1 创建 `scrolling-common` 共享模块

**影响**: 消除约 5000+ 行重复代码

当前 8+ 个 scrolling-aosp-* 模块各自复制了：
- Bean 类（FriendCircleBean, CommentBean, UserBean, PraiseBean, OtherInfoBean）— 复制 10 次
- NineGridView — 复制 9 次
- NineImageAdapter — 复制 9 次
- 接口类 — 复制 9 次

**新模块结构**:
```
scrolling-common/
  src/main/java/com/example/scrolling/common/
    beans/          # 统一 Bean 类
    widgets/        # NineGridView
    adapters/       # NineImageAdapter, BaseFriendCircleAdapter
    data/           # BaseDataCenter, ScrollingConstants
    interfaces/     # OnPraiseOrCommentClickListener
    utils/          # SpanUtils
```

scrolling-aosp-power 的额外字段（isExpanded, showComment 等）通过继承或组合扩展。

### 3.2 合并 Load Activity 为参数化 Activity

**影响**: 每个 scrolling 模块减少 11 个 Activity 类为 1 个

```java
// 之前: 11 个几乎相同的 Activity
public class LightLoadActivity extends ... { mLoadType = LoadType.LIGHT; }
public class MediumLoadActivity extends ... { mLoadType = LoadType.MEDIUM; }
// ...

// 之后: 1 个通用 Activity
public class ScrollingLoadActivity extends ... {
    mLoadType = getIntent().getIntExtra(EXTRA_LOAD_TYPE, LoadType.LIGHT);
}
```

注意：AndroidManifest 中仍需声明 activity-alias 以保持 ADB 自动化兼容性。

### 3.3 数据驱动的 MainActivity 路由

**影响**: 每个模块减少约 200 行路由代码

用 `Map<Integer, Integer>` 替换 if-else 链。

### 3.4 创建 `scrolling-webview-common` 模块

**影响**: 消除 GeckoView 3 个变体（surface/texture/imagereader）之间的代码重复

提取共享的 GeckoView 基础代码，每个变体只保留 rendering backend 差异。

### 3.5 统一 LoadType 体系

当前存在 3 套独立的 LoadType：
1. `load-config/LoadType.java` — 11 种 @IntDef
2. `launch-common/LoadSimulator.kt` — 3 种 enum
3. `switch-common/SwitchLoadType.kt` — 独立枚举

**方案**: 在 `load-config` 定义共享的 `LoadLevel` (LIGHT/MEDIUM/HEAVY) 基础类型，各模块的特定类型组合/扩展此基础。

### 3.6 重命名 LoadSimulator 避免混淆

- `load-config/LoadSimulator.java` → `FrameLoadSimulator`
- `launch-common/LoadSimulator.kt` → `LaunchLoadSimulator`
- 保留 `switch-common/SwitchLoadManager`

### 3.7 提取共享 load primitives

从 `launch-common/LoadSimulator.kt` 提取 `runCpuLoad()`, `runIoLoad()`, `runBinderLoad()`, `runMemoryLoad()` 到 `load-primitives` 模块，供三个模块家族共用。

---

## Phase 4: 代码质量提升（Medium Effort, Medium Impact）

> 预计工作量：3-5 天

### 4.1 消除 Context 泄漏

**受影响文件**:
- `FriendCircleAdapter.java` — Activity Context 强引用
- `TextClickSpan.java` — 嵌入 SpannableString 的 Context 引用
- `NineImageAdapter.java` — Activity Context 引用
- `CommentOrPraisePopupWindow.java` — PopupWindow Context 引用

**方案**: 使用 Application Context 或 `WeakReference<Context>`

### 4.2 消除 `getResources().getIdentifier()` 反射调用

**受影响文件**: `FriendCircleAdapter.java:155`, `NineImageAdapter.java:62`

在滚动路径中使用反射极慢 → 预构建 resource name → ID 映射 Map。

### 4.3 使用 DiffUtil 替换 notifyDataSetChanged

**文件**: `FriendCircleAdapter.java:80`

1000 项列表全量刷新 → DiffUtil 增量更新。

### 4.4 修复 notifyItemRangeInserted 参数错误

**文件**: `FriendCircleAdapter.java:89`

起始位置应为添加前的 size，而非添加后的 size。

### 4.5 替换反射计算 StatusBar 高度

**文件**: `Utils.java:53-62`

使用 `WindowInsetsCompat` 替代 `com.android.internal.R$dimen` 反射。

### 4.6 统一包名

当前混乱：
- `com.kcrason.highperformancefriendscircle` (app)
- `com.example.wechatfriendforperformance` (performance)
- `com.android.wechatfriendforpower` (power, 与 AOSP 命名空间冲突!)
- `com.example.switch_common` (下划线) vs `com.example.launch.common` (点号)

**方案**: 统一为 `com.example.perftest.{scrolling|launch|switch}.{module}` 格式。

### 4.7 launch-game 与其他 launch 模块对齐

- Java → Kotlin（与其他 launch 模块一致）
- 使用 launch-common 的 LoadSimulator 而非自有 LoadSimulator.java
- 统一 flavor 配置（LOAD_TYPE vs LOAD_DURATION_MS）

---

## Phase 5: 自动化测试改进（Medium Effort, Medium Impact）

> 预计工作量：3-5 天

### 5.1 [P0] 修复 FPS 计算和 janky_percent 不一致

- `avg_fps = 1000.0 / mean(frame_durations)` 替换近似计算
- 统一 `janky_percent` 的分母（parsed frames vs summary total）

### 5.2 [P1] 修复 ADB 命令安全性

- `subprocess.run(shell=True)` → 使用参数列表
- 添加 ADB 重试逻辑

### 5.3 [P1] 修复 LogcatMonitor 子进程泄漏

- 添加 `finally` 块确保 `proc.terminate()` + `proc.wait()` 始终执行
- Monitor 启动应在触发动作之前

### 5.4 [P2] 替换 switch 测试硬编码坐标

当前使用固定屏幕坐标点击按钮，UI 变化会静默失败 → 使用 UIAutomator 或 accessibility ID。

### 5.5 [P2] 添加滚动测试预热和多次迭代

- 启动后先做一次丢弃的预热滚动
- 像 launch/switch 测试一样支持多次迭代+统计聚合

### 5.6 [P2] 清理死代码

- 删除未使用的 `run_single_launch()` 和 `run_single_switch()`
- 移除 debug print 语句

### 5.7 [P3] 从 apk_registry.json 读取 test_type

替换 `TestStrategy.get_strategy_for_app()` 中的硬编码应用名检查。

### 5.8 [P3] 统一 APK 安装脚本

`install_apks.sh` 应从 `apk_registry.json` 读取 APK 列表而非硬编码。

### 5.9 [P4] 添加框架单元测试

优先为 `FPSAnalyzer.parse_gfxinfo()` 和 `LogcatMonitor` 模式匹配编写测试。

---

## Phase 6: 可选改进（Nice to Have）

### 6.1 构建系统

- 创建 Gradle Convention Plugin（替代 `apply from` 模式）
- AGP 升级到 8.7.x
- Kotlin 升级到 2.0.x（配合 Compose Compiler Plugin 2.0.0）
- 注解处理器迁移到 KSP（Hilt, Room, Glide）
- 启用 Configuration Cache

### 6.2 代码现代化

- app 模块从 RxJava 2 迁移到 Kotlin Coroutines/Flow
- 文档说明 app 模块作为 "baseline" 的角色
- 文档说明 MVVM+Hilt 仅用于 customscroller 和 renderstress 两个模块的原因

### 6.3 自动化测试增强

- 报告中添加图表（matplotlib 或 Chart.js）
- CI/CD 输出 JUnit XML 格式
- 支持多设备并行测试（`-s serial`）
- 添加基线对比模式（检测性能回归）
- 添加 `results/.gitignore`

---

## 依赖关系图

```
Phase 1 (紧急修复)
  └── 无依赖，可立即开始

Phase 2 (构建系统)
  ├── 2.1 Version Catalog → 独立
  ├── 2.2 签名配置 → 独立
  ├── 2.3 Java 版本统一 → 独立
  └── 2.4-2.6 → 独立

Phase 3 (架构重构)
  ├── 3.1 scrolling-common → 依赖 2.1 (统一版本后更容易)
  ├── 3.2 参数化 Activity → 依赖 3.1
  ├── 3.3 数据驱动路由 → 依赖 3.2
  ├── 3.4 webview-common → 依赖 3.1
  ├── 3.5 统一 LoadType → 独立
  └── 3.6-3.7 LoadSimulator 重命名 → 依赖 3.5

Phase 4 (代码质量)
  └── 可与 Phase 2/3 并行

Phase 5 (自动化测试)
  └── 可与其他 Phase 并行
```

---

## 风险评估

| 重构项 | 风险等级 | 缓解措施 |
|--------|---------|---------|
| scrolling-common 提取 | 中 | 逐模块迁移，每次迁移后运行完整测试 |
| Version Catalog 迁移 | 低 | 纯构建配置变更，不影响运行时 |
| Activity 合并 | 中 | 需要确保 ADB 自动化脚本兼容 |
| 包名统一 | 高 | 影响所有已安装 APK，需要同步更新自动化脚本 |
| AGP/Kotlin 升级 | 中 | 需要全量构建验证 |

---

## 总结

本项目作为 Android 性能测试平台，架构设计清晰（load-config 中心化、*-common 共享模式），但在快速扩展 30+ 模块的过程中积累了大量代码重复（约 5000+ 行 Bean/Widget 重复）和构建配置漂移。

**最高优先级**: Phase 1 的 Critical Bug 修复（UncaughtExceptionHandler、LayoutParams、FPS 计算）和 Phase 2 的 Version Catalog 引入。

**最大收益**: Phase 3.1 创建 `scrolling-common` 模块可消除最多的代码重复。

**建议执行顺序**: Phase 1 → Phase 2.1 → Phase 3.1 → Phase 2 其余 → Phase 3 其余 → Phase 4/5 并行。
