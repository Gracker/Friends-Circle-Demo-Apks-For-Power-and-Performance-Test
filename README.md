# 微信朋友圈测试 Demo

## 📊 项目状态

[![Android CI/CD](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/actions/workflows/android.yml/badge.svg)](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test?label=Release&color=brightgreen)](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/releases)
[![API Level](https://img.shields.io/badge/API-24%2B-blue.svg)](https://android-arsenal.com/api?level=24)
[![AGP](https://img.shields.io/badge/AGP-8.7.3-blue.svg)](https://developer.android.com/build)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/total?label=Downloads&color=orange)](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/releases)
[![Stars](https://img.shields.io/github/stars/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test?label=Stars&color=yellow)](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/stargazers)
[![Language](https://img.shields.io/badge/Language-Java%20%26%20Kotlin-orange.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

本项目是一个基于微信朋友圈 UI 的性能测试平台，旨在研究 Android 滑动性能和功耗表现。项目包含 32 个模块（28 个应用 + 4 个共享库），分别用于不同方面的测试和研究。

*Read this in [English](README_EN.md)*

## 技术栈

| 项目 | 版本 |
|------|------|
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| compileSdk | 35 |
| minSdk | 24 |
| Jetpack Compose BOM | 2024.12.01 |

**构建系统特性：**
- **Version Catalog** (`gradle/libs.versions.toml`)：集中管理所有依赖版本
- **Convention Plugin** (`build-logic/`)：统一 Android App / Library / Compose 构建配置
- **Configuration Cache**：加速增量构建

## 负载类型说明

所有测试模块都支持11种负载类型，覆盖不同的性能测试场景：

| 类型 | 负载名称 | 说明 |
|------|----------|------|
| 最轻负载 | Minimal Load | 不添加任何额外负载 |
| 帧内轻负载 | In-Frame Light | 每帧内执行轻量计算 |
| 帧内中负载 | In-Frame Medium | 每帧内执行中等计算 |
| 帧内高负载 | In-Frame Heavy | 每帧内执行密集计算 |
| 帧间轻负载 | Between-Frame Light | 帧与帧之间执行轻量任务 |
| 帧间中负载 | Between-Frame Medium | 帧与帧之间执行中等任务 |
| 帧间高负载 | Between-Frame Heavy | 帧与帧之间执行密集任务 |
| 混合轻负载 | Mixed Light | 同时执行帧内和帧间轻量负载 |
| 混合中负载 | Mixed Medium | 同时执行帧内和帧间中等负载 |
| 混合高负载 | Mixed Heavy | 同时执行帧内和帧间密集负载 |
| **超长帧负载** | **Long Frame** | **滑动过程中随机出现2-3次超长帧（HEAVY×10）** |

### 负载配置模块

项目使用统一的 `load-config` 模块管理所有负载配置，包括：
- 负载类型常量定义（`LoadType`）
- 负载强度数值配置（`LoadConfig`）
- 任务调度参数（间隔、随机种子等）

各个测试模块通过引用此公共模块来保证负载配置的一致性。

## 项目结构

项目按实现类型分为以下几类模块：

### 📚 共享库模块

| 模块目录 | 说明 |
|---------|------|
| `load-config` | 统一负载模拟配置（11 种负载类型、强度参数、调度配置） |
| `scrolling-common` | 滑动模块公共代码（Beans、接口、自定义控件、工具类、资源） |
| `launch-common` | 启动模块公共代码（3 种 Flavor：Light/Medium/Heavy） |
| `switch-common` | 跳转模块公共代码（10 种负载组合、XML Inflate、自定义 View） |

### 🆕 v1.1.0 更新说明
- **图标升级**: 全新设计的应用图标，采用 3 行式布局，清晰展示模块类型、技术栈和 Flavor。
- **命名优化**: Launch 模块应用名称精简 (例如: `Launch AOSP Light` -> `AOSP-Light`)。
- **视觉层级**: 优化了图标中字体的视觉层级，关键信息更突显。

### 📦 滑动测试模块（标准 Android UI + RenderThread）

| 模块目录 | 说明 |
|---------|------|
| `app` | 原始项目，随机展示的微信朋友圈界面 |
| `scrolling-aosp-performance` | 性能测试版本，支持11种负载类型 |
| `scrolling-aosp-power` | 电量测试版本，固定内容用于精确功耗测试 |
| `scrolling-aosp-picasso` | 使用 Picasso 图片加载库 |
| `scrolling-aosp-customscroller` | 自研 CustomOverScroller + CustomTimelineView |
| `scrolling-aosp-renderstress` | RenderThread 压测版本 |
| `scrolling-aosp-softwarerender` | 软件渲染版本（禁用硬件加速） |
| `scrolling-aosp-douyin` | 抖音风格视频滑动 |
| `scrolling-aosp-video` | 视频 Feed 版本 |
| `scrolling-aosp-ebook` | 电子书阅读器 Demo |
| `scrolling-aosp-purerenderthread` | 纯 RenderThread 列表滑动 |
| `scrolling-aosp-dualwindow` | 双 Window 刷新 Demo |
| `scrolling-aosp-mixedrender` | 混合渲染 Demo |
| `scrolling-compose` | Jetpack Compose 实现版本 |

### 启动性能测试模块

| 模块目录 | 说明 |
|---------|------|
| `launch-aosp` | 标准 Android UI 启动测试 (支持轻/中/重负载) |
| `launch-compose` | Jetpack Compose 启动测试 (模拟状态重组) |
| `launch-webview` | WebView 混合开发启动测试 (模拟 DOM/JSON 压力) |
| `launch-gl` | OpenGL ES 启动测试 (模拟纹理/状态切换压力) |
| `launch-game` | 游戏引擎启动测试 (模拟物理/AI/纹理加载压力) |

### 🔄 应用内跳转性能测试模块 (switch-*)

| 模块目录 | 说明 |
|---------|------|
| `switch-common` | 公共负载模块：真实 XML Inflate、自定义 View、Binder/IO 操作 |
| `switch-aosp` | 标准 Android UI 跳转测试 (10 种负载组合) |
| `switch-flutter` | Flutter 风格 Canvas 渲染跳转测试 (10 种负载组合) |
| `switch-webview` | WebView 混合开发跳转测试 (10 种负载组合) |

**Switch 模块负载说明：**
- **自身负载 (Self Load)**: 无 / 轻 / 中 / 重 - 模拟目标 Activity 的 UI 复杂度
- **背景负载 (Background Load)**: 无 / 中 / 重 - 模拟系统繁忙程度
- **10 种组合**: Pure、Self-Light、Self-Medium、Self-Heavy、Bg-Medium、Bg-Heavy、Light+Med、Med+Med、Heavy+Med、Heavy+Heavy

**特点：**
- 真实的 XML 布局 Inflate（300+ 布局文件）
- 真实的自定义 View 创建（5 种复杂度）
- 真实的 Binder IPC 调用
- 真实的文件 IO 操作
- 伪随机确保测试可重复
- 后台负载在跳转完成 1 秒后自动停止
- 支持第一帧后的延迟负载模拟

### WebView 模块

| 模块目录 | 说明 |
|---------|------|
| `scrolling-webview` | 标准 WebView Functor 实现 |
| `scrolling-webview-surface` | WebView + SurfaceView |
| `scrolling-webview-texture` | WebView + TextureView |
| `scrolling-webview-imagereader` | WebView + ImageReader |

### Map 模块

| 模块目录 | 说明 |
|---------|------|
| `scrolling-surface-map` | SurfaceView 地图 Demo |
| `scrolling-gl-map` | OpenGL ES 2.0 地图 Demo |

## 主页布局统一说明
- 参考 `scrolling-aosp-performance`，所有带主页入口的模块统一为 **无 ActionBar**，并在顶部加入应用信息卡片（40dp 顶部留白，避免被状态栏遮挡）。
- 应用信息模块下方直接衔接各类负载入口，原有负载按钮与卡片布局保持不变。
- 无主页的模块（如 `scrolling-aosp-douyin`、`scrolling-aosp-ebook`）已移除应用信息卡片，保持沉浸式体验。

## 模块详细说明

### 1. 原始项目 (app)

原始的高性能微信朋友圈实现，来自 fork 的项目。这个模块展示了如何高效实现类似微信朋友圈的滑动列表，包含多种性能优化技巧。

### 2. 性能测试模块 (scrolling-aosp-performance)

专门设计用于测试和比较不同负载下的滑动性能表现。支持11种负载模式，在关键代码处添加了 Trace 点，方便使用 Perfetto 等工具进行性能分析和优化。

### 3. 功耗测试模块 (scrolling-aosp-power)

单 Activity 设计，每次进去环境都一模一样，内容固定不变，用于精确的功耗测试。

### 4. WebView测试模块 (scrolling-webview)

使用WebView实现朋友圈界面，用于测试WebView与原生实现在性能方面的差异。支持11种负载级别，实现了JavaScript与Java交互，支持动态加载最多200条朋友圈数据。

### 5. 自定义Scroller测试模块 (scrolling-aosp-customscroller)

- 完全保留 AOSP UI，但移除了 RecyclerView/ListView，采用自研的 `CustomTimelineView` + `CustomOverScroller`
- 通过 Hilt + MVVM + Room 构建数据流，启动即缓存 100 条固定数据
- 支持11种负载类型，便于验证厂商对 `OverScroller` 的自定义优化差异

### 6. RenderThread压测模块 (scrolling-aosp-renderstress)

- 代码骨架与自定义 Scroller 模块一致，同样依赖 `CustomOverScroller`
- 借助 `RenderStressOverlayView` 在滑动事件触发时应用高阶模糊 / Shader 链
- 支持11种负载类型，模拟"UI 线程快、RenderThread 过载"的真实现象

### 7. 软件渲染测试模块 (scrolling-aosp-softwarerender)

使用软件渲染模式（禁用硬件加速）的朋友圈实现：

- **禁用硬件加速**：通过 `android:hardwareAccelerated="false"` 配置
- **只有UI Thread**：没有RenderThread，所有绑制操作在主线程完成
- 支持11种负载类型，适合测试CPU密集型场景

### 8. Compose测试模块 (scrolling-compose)

使用Jetpack Compose开发的朋友圈实现：

- **声明式UI**：使用Kotlin + Compose声明式UI框架
- **LazyColumn**：替代RecyclerView的列表组件
- **Coil图片加载**：Compose友好的图片加载库
- 支持11种负载类型，方便对比框架性能差异

### 9. SurfaceView地图测试模块 (scrolling-surface-map)

模拟高德地图风格的Demo应用：

- **SurfaceView地图**：使用SurfaceView在独立线程渲染地图网格
- **原生控件叠加**：顶部导航栏和底部控制面板使用原生View
- **滚动手势支持**：支持拖拽和惯性滚动
- 支持11种负载类型，测试SurfaceView与原生View混合场景

### 10. 纯RenderThread测试模块 (scrolling-aosp-purerenderthread)

纯RenderThread列表滑动实现：

- **UI Thread零渲染**：主线程只处理触摸事件，不参与任何绘制
- **独立渲染线程**：所有绘制操作在专门的渲染线程完成
- **SurfaceView实现**：利用SurfaceView的独立Surface
- 支持11种负载类型，验证纯渲染线程方案的性能表现

### 11. 双Window测试模块 (scrolling-aosp-dualwindow)

双Window同时刷新渲染：

- **双Window同时存在**：主Window + 悬浮Overlay Window
- **双doFrame回调**：systrace中每帧有2个doFrame
- **双RenderThread**：每帧有2个RenderThread的drawFrame
- **需要悬浮窗权限**：使用WindowManager添加第二个Window
- 支持11种负载类型，测试多Window场景的性能表现

### 12. 混合渲染测试模块 (scrolling-aosp-mixedrender)

混合两种渲染管线的Demo：

- **纯RenderThread动画**：顶部SurfaceView使用独立渲染线程
- **标准UI+RenderThread**：底部RecyclerView使用正常View层级
- **模拟视频覆盖场景**：类似视频播放器叠加在可滑动列表上
- 支持11种负载类型，分析混合渲染的性能特征

### 13. OpenGL地图测试模块 (scrolling-gl-map)

OpenGL ES 2.0地图渲染Demo：

- **GLSurfaceView**：硬件加速的OpenGL渲染
- **地图元素**：网格、道路、建筑物、标记点
- **手势支持**：拖拽平移和捏合缩放
- **原生UI叠加**：搜索栏和控制按钮
- 支持11种负载类型，GPU密集型场景测试

### 14. 抖音风格视频滑动模块 (scrolling-aosp-douyin)

模拟抖音的全屏视频滑动体验：

- **全屏视频播放**：每个视频占满整个屏幕，沉浸式体验
- **自研滚动容器**：`VerticalVideoScroller` + `VideoOverScroller` 实现翻页式滚动
- **速度感知动画**：滑动时间与滑动速度相关，范围 200ms - 600ms
- **智能切换**：滑动过半自动切换到下一个视频
- **完美UI还原**：底部导航栏、右侧互动按钮、左下角信息区域
- 使用 Media3 ExoPlayer 进行视频播放

### 15. 电子书阅读器模块 (scrolling-aosp-ebook)

模拟电子书阅读器的翻页体验：

- **EPUB 支持**：支持标准 EPUB 格式电子书解析
- **触摸翻页**：点击左侧上一页，点击右侧下一页，点击中间显示菜单
- **滑动翻页**：支持左右滑动翻页，带有平滑动画效果
- **阅读菜单**：顶部和底部菜单（模拟），包含目录、亮度、字体、主题、设置等功能
- **进度显示**：底部显示当前页码，支持进度条快速跳转
- **沉浸阅读**：全屏模式，暖黄色护眼背景
- 默认加载《巨婴国》电子书

### 16. 启动性能测试模块 (launch-*)

这是一组专门用于测试不同技术栈 App 启动性能的 Demo，旨在模拟真实的复杂启动场景。

**核心特性：**
- **交错负载模拟 (Interleaved Loading)**: 模拟主线程上碎片化的 CPU、IO、Binder 和 Memory 操作。
- **领域特定高保真负载**:
    - **AOSP**: 模拟 View Inflation (反射/递归) 压力。
    - **Compose**: 模拟复杂 List 数据生成、筛选排序 (ViewModel 逻辑) 及 State Snapshot (重组) 压力。
    - **WebView**: 模拟 DOM Thrashing (布局回流)、大 JSON 解析及复杂 JS 业务逻辑计算压力。
    - **OpenGL**: 模拟 Shader 编译 (多材质)、关卡数据 IO 读取、游戏逻辑初始化 (物理/AI) 及 Texture 上传压力。
- **生命周期感知**: 负载分散在 `Application.onCreate`, `Activity.onCreate`, `onStart` (阻塞等待), `onResume` 等阶段。
- **异步网络模拟**: 使用协程模拟带抖动的网络延迟和渐进式数据加载。
- **多维度负载**: 提供 Light (轻度), Medium (中度), Heavy (重度) 三种 Flavor，分别对应不同的启动耗时目标 (100ms - 3s+)。

## 性能优化策略

在 Android 中，要避免列表卡顿，主要从以下几个角度进行优化：

- 减少布局层级，避免过多的 Item View 的无用布局嵌套
- 滑动时控制图片加载，停止滑动后再加载图片
- 避免在 Adapter 填充数据时做过多计算，复杂计算应在数据准备阶段完成
- 在数据 Bean 中完成数据变换操作，如将 String 转换为 SpannableStringBuilder
- 减少 onMeasure() 和 onLayout() 的调用次数
- 实现 View 对象的缓存，减少 View 的创建

## 如何使用

1. 运行 `app` 模块查看原始的高性能朋友圈实现
2. 运行 `scrolling-aosp-performance` 模块进行性能测试：
   - 选择11种负载级别中的任意一种
   - 使用 Perfetto 或其他性能分析工具收集数据
   - 分析 Trace 结果进行性能优化
3. 运行 `scrolling-aosp-power` 模块测试功耗表现
4. 运行 `scrolling-webview` 模块测试WebView性能
5. 运行 `scrolling-aosp-customscroller` 模块体验自定义滚动容器
6. 运行 `scrolling-aosp-renderstress` 模块验证 RenderThread 负载

## 性能测试对比

通过对比不同实现方式和不同负载级别下的性能表现，可以得到以下结论：

1. 原生实现在各种负载条件下都表现优异
2. WebView实现在轻负载条件下表现接近原生，但随着负载增加，性能下降更为明显
3. 在处理大量数据时，动态加载机制可以有效提升用户体验
4. 功耗测试显示，优化的滑动实现能够显著延长电池寿命

## 特别鸣谢

感谢原项目作者 KCrason（其 GitHub 主页链接已失效）的杰出工作和 [razerdp](https://github.com/razerdp) 提供的 View 缓存思路。本项目在原有基础上进行了扩展，增加了专门的性能、功耗和WebView测试模块。

## 未来计划

未来可能会继续更新该项目，包括但不限于：
- 添加更多性能测试指标
- 改进功耗测试精度
- 实现表情匹配
- 实现电话号码匹配等功能
- 增加更多实现版本

欢迎 Star 和贡献！

## 📱 下载应用

### 🚀 自动构建版本
每次代码更新后，GitHub Actions会自动构建最新版本的APK文件。你可以在[Releases页面](https://github.com/Gracker/Friends-Circle-Demo-Apks-For-Power-and-Performance-Test/releases)下载：

**滑动测试模块：**
- **app** - 主应用模块
- **scrolling-aosp-performance** - 性能测试模块 (11种负载)
- **scrolling-aosp-power** - 功耗测试模块
- **scrolling-aosp-picasso** - Picasso图片加载
- **scrolling-aosp-customscroller** - 自定义Scroller测试模块 (11种负载)
- **scrolling-aosp-renderstress** - RenderThread压测模块 (11种负载)
- **scrolling-aosp-softwarerender** - 软件渲染测试模块 (11种负载)
- **scrolling-aosp-douyin** - 抖音风格视频滑动模块
- **scrolling-aosp-video** - 视频Feed模块
- **scrolling-aosp-ebook** - 电子书阅读器模块
- **scrolling-aosp-purerenderthread** - 纯RenderThread测试模块 (11种负载)
- **scrolling-aosp-dualwindow** - 双Window刷新测试模块 (11种负载)
- **scrolling-aosp-mixedrender** - 混合渲染测试模块 (11种负载)
- **scrolling-compose** - Jetpack Compose测试模块 (11种负载)

**启动性能模块：**
- **launch-aosp** - 标准 View 体系 (轻/中/重负载)
- **launch-compose** - Jetpack Compose (轻/中/重负载)
- **launch-webview** - WebView 混合开发 (轻/中/重负载)
- **launch-gl** - OpenGL ES (轻/中/重负载)
- **launch-game** - 游戏引擎 (轻/中/重负载)

**应用内跳转性能模块 (Switch)：**
- **switch-aosp** - 标准 Android UI 跳转测试 (10 种负载)
- **switch-flutter** - Flutter 风格渲染跳转测试 (10 种负载)
- **switch-webview** - WebView 混合开发跳转测试 (10 种负载)

**WebView 模块：**
- **scrolling-webview** - WebView Functor测试模块 (10种负载)
- **scrolling-webview-surface** - WebView+SurfaceView
- **scrolling-webview-texture** - WebView+TextureView
- **scrolling-webview-imagereader** - WebView+ImageReader

**Map 模块：**
- **scrolling-surface-map** - SurfaceView地图测试模块 (10种负载)
- **scrolling-gl-map** - OpenGL地图测试模块 (10种负载)

### 📋 版本说明
- **Debug版本**: 包含调试信息，可直接安装使用
- **Release版本**: 优化版本，需要签名后才能发布

> 💡 **提示**: Debug版本适合体验和测试，Release版本适合正式使用

## 🔧 开发者指南

### 自动化构建和发布
本项目配置了完整的CI/CD流程：

- **自动构建**: 每次push到master分支自动构建所有模块
- **自动发布**: 构建成功后自动创建GitHub Release
- **手动发布**: 支持手动触发带版本号的正式发布

详细配置说明请查看 [docs/review-build-system.md](docs/review-build-system.md)

### 签名配置
如需配置APK签名，请参考 [docs/refactoring-plan.md](docs/refactoring-plan.md)


# 项目截图
![main_page.jpg](pic/main_page.jpg)
![friends_1.jpg](pic/friends_1.jpg)
![friends_2.jpg](pic/friends_2.jpg)
![trace.png](pic/trace.png)
