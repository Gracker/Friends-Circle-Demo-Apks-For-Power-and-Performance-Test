# 微信朋友圈测试 Demo

## 📊 项目状态

[![Android CI/CD](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml/badge.svg)](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/Gracker/HighPerformanceFriendsCircle?label=Release&color=brightgreen)](https://github.com/Gracker/HighPerformanceFriendsCircle/releases)
[![API Level](https://img.shields.io/badge/API-21%2B-blue.svg)](https://android-arsenal.com/api?level=21)
[![Gradle](https://img.shields.io/badge/Gradle-8.2.2-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/Gracker/HighPerformanceFriendsCircle/total?label=Downloads&color=orange)](https://github.com/Gracker/HighPerformanceFriendsCircle/releases)
[![Stars](https://img.shields.io/github/stars/Gracker/HighPerformanceFriendsCircle?label=Stars&color=yellow)](https://github.com/Gracker/HighPerformanceFriendsCircle/stargazers)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

本项目是一个基于微信朋友圈 UI 的性能测试平台，旨在研究 Android 滑动性能和功耗表现。项目包含多个主要模块，分别用于不同方面的测试和研究。

*Read this in [English](README_EN.md)*

## 负载类型说明

所有测试模块都支持10种负载类型，覆盖不同的性能测试场景：

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

## APK 说明

1. **app-release**: 原项目 App，进去后是一个随机展示的微信朋友圈界面，仅作保留使用。
2. **wechatfriendforperformance-release**: 用来测试性能的 App，使用标准的 AOSP 实现。支持10种负载类型。
3. **wechatfriendforpower-release**: 原项目 App 魔改，进去后是一个固定显示内容的微信朋友圈界面，用来测试固定环境下的性能或功耗。
4. **wechatfriendforwebview-release**: 用来测试性能的 App，使用标准的 WebView 实现。支持10种负载类型。
5. **wechatfriendforcustomscroller-debug**: 全新自研滚动容器版本，使用 `CustomOverScroller` + 自定义 `CustomTimelineView`，支持10种负载类型。
6. **wechatfriendforrenderstress-debug**: 基于自研列表的 RenderThread 压测版本，支持10种负载类型。
7. **wechatfriendforsoftwarerender-debug**: 软件渲染版本，禁用硬件加速，只有UI Thread，支持10种负载类型。
8. **wechatfriendforcompose-debug**: Jetpack Compose版本，使用Kotlin + Compose声明式UI框架开发，支持10种负载类型。
9. **wechatfriendforsurfacemap-debug**: 高德地图风格Demo，使用SurfaceView实现地图滚动，上下有原生View控件，支持10种负载类型。
10. **wechatfriendforpurerenderthread-debug**: 纯RenderThread列表滑动，UI Thread不参与渲染，所有绘制在独立渲染线程完成，支持10种负载类型。
11. **wechatfriendfordualwindow-debug**: 双Window刷新Demo，每帧有2个doFrame和2个RenderThread的drawFrame，支持10种负载类型。
12. **wechatfriendformixedrender-debug**: 混合渲染Demo，同时有纯RenderThread动画和标准UI+RenderThread渲染，支持10种负载类型。
13. **wechatfriendforglmap-debug**: OpenGL ES 2.0地图Demo，类似高德/谷歌地图，支持10种负载类型。

## 项目结构

本项目包含以下主要模块:

### 1. 原始项目 (app)

原始的高性能微信朋友圈实现，来自 fork 的项目。这个模块展示了如何高效实现类似微信朋友圈的滑动列表，包含多种性能优化技巧。

### 2. 性能测试模块 (wechatfriendforperformance)

专门设计用于测试和比较不同负载下的滑动性能表现。支持10种负载模式，在关键代码处添加了 Trace 点，方便使用 Perfetto 等工具进行性能分析和优化。

### 3. 功耗测试模块 (wechatfriendforpower)

单 Activity 设计，每次进去环境都一模一样，内容固定不变，用于精确的功耗测试。

### 4. WebView测试模块 (wechatfriendforwebview)

使用WebView实现朋友圈界面，用于测试WebView与原生实现在性能方面的差异。支持10种负载级别，实现了JavaScript与Java交互，支持动态加载最多200条朋友圈数据。

### 5. 自定义Scroller测试模块 (wechatfriendforcustomscroller)

- 完全保留 AOSP UI，但移除了 RecyclerView/ListView，采用自研的 `CustomTimelineView` + `CustomOverScroller`
- 通过 Hilt + MVVM + Room 构建数据流，启动即缓存 100 条固定数据
- 支持10种负载类型，便于验证厂商对 `OverScroller` 的自定义优化差异

### 6. RenderThread压测模块 (wechatfriendforrenderstress)

- 代码骨架与自定义 Scroller 模块一致，同样依赖 `CustomOverScroller`
- 借助 `RenderStressOverlayView` 在滑动事件触发时应用高阶模糊 / Shader 链
- 支持10种负载类型，模拟"UI 线程快、RenderThread 过载"的真实现象

### 7. 软件渲染测试模块 (wechatfriendforsoftwarerender)

使用软件渲染模式（禁用硬件加速）的朋友圈实现：

- **禁用硬件加速**：通过 `android:hardwareAccelerated="false"` 配置
- **只有UI Thread**：没有RenderThread，所有绑制操作在主线程完成
- 支持10种负载类型，适合测试CPU密集型场景

### 8. Compose测试模块 (wechatfriendforcompose)

使用Jetpack Compose开发的朋友圈实现：

- **声明式UI**：使用Kotlin + Compose声明式UI框架
- **LazyColumn**：替代RecyclerView的列表组件
- **Coil图片加载**：Compose友好的图片加载库
- 支持10种负载类型，方便对比框架性能差异

### 9. SurfaceView地图测试模块 (wechatfriendforsurfacemap)

模拟高德地图风格的Demo应用：

- **SurfaceView地图**：使用SurfaceView在独立线程渲染地图网格
- **原生控件叠加**：顶部导航栏和底部控制面板使用原生View
- **滚动手势支持**：支持拖拽和惯性滚动
- 支持10种负载类型，测试SurfaceView与原生View混合场景

### 10. 纯RenderThread测试模块 (wechatfriendforpurerenderthread)

纯RenderThread列表滑动实现：

- **UI Thread零渲染**：主线程只处理触摸事件，不参与任何绘制
- **独立渲染线程**：所有绘制操作在专门的渲染线程完成
- **SurfaceView实现**：利用SurfaceView的独立Surface
- 支持10种负载类型，验证纯渲染线程方案的性能表现

### 11. 双Window测试模块 (wechatfriendfordualwindow)

双Window同时刷新渲染：

- **双Window同时存在**：主Window + 悬浮Overlay Window
- **双doFrame回调**：systrace中每帧有2个doFrame
- **双RenderThread**：每帧有2个RenderThread的drawFrame
- **需要悬浮窗权限**：使用WindowManager添加第二个Window
- 支持10种负载类型，测试多Window场景的性能表现

### 12. 混合渲染测试模块 (wechatfriendformixedrender)

混合两种渲染管线的Demo：

- **纯RenderThread动画**：顶部SurfaceView使用独立渲染线程
- **标准UI+RenderThread**：底部RecyclerView使用正常View层级
- **模拟视频覆盖场景**：类似视频播放器叠加在可滑动列表上
- 支持10种负载类型，分析混合渲染的性能特征

### 13. OpenGL地图测试模块 (wechatfriendforglmap)

OpenGL ES 2.0地图渲染Demo：

- **GLSurfaceView**：硬件加速的OpenGL渲染
- **地图元素**：网格、道路、建筑物、标记点
- **手势支持**：拖拽平移和捏合缩放
- **原生UI叠加**：搜索栏和控制按钮
- 支持10种负载类型，GPU密集型场景测试

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
2. 运行 `wechatfriendforperformance` 模块进行性能测试：
   - 选择10种负载级别中的任意一种
   - 使用 Perfetto 或其他性能分析工具收集数据
   - 分析 Trace 结果进行性能优化
3. 运行 `wechatfriendforpower` 模块测试功耗表现
4. 运行 `wechatfriendforwebview` 模块测试WebView性能
5. 运行 `wechatfriendforcustomscroller` 模块体验自定义滚动容器
6. 运行 `wechatfriendforrenderstress` 模块验证 RenderThread 负载

## 性能测试对比

通过对比不同实现方式和不同负载级别下的性能表现，可以得到以下结论：

1. 原生实现在各种负载条件下都表现优异
2. WebView实现在轻负载条件下表现接近原生，但随着负载增加，性能下降更为明显
3. 在处理大量数据时，动态加载机制可以有效提升用户体验
4. 功耗测试显示，优化的滑动实现能够显著延长电池寿命

## 特别鸣谢

感谢原项目作者 [KCrason](https://github.com/KCrason) 的杰出工作和 [razerdp](https://github.com/razerdp) 提供的 View 缓存思路。本项目在原有基础上进行了扩展，增加了专门的性能、功耗和WebView测试模块。

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
每次代码更新后，GitHub Actions会自动构建最新版本的APK文件。你可以在[Releases页面](../../releases)下载：

- **HighPerformanceFriendsCircle-debug** - 主应用模块
- **WeChatFriendForPerformance-debug** - 性能测试模块 (10种负载)
- **WeChatFriendForPower-debug** - 功耗测试模块  
- **WeChatFriendForWebView-debug** - WebView测试模块 (10种负载)
- **WeChatFriendForCustomScroller-debug** - 自定义Scroller测试模块 (10种负载)
- **WeChatFriendForRenderStress-debug** - RenderThread压测模块 (10种负载)
- **WeChatFriendForSoftwareRender-debug** - 软件渲染测试模块 (10种负载)
- **WeChatFriendForCompose-debug** - Compose测试模块 (10种负载)
- **WeChatFriendForSurfaceMap-debug** - SurfaceView地图测试模块 (10种负载)
- **WeChatFriendForPureRenderThread-debug** - 纯RenderThread测试模块 (10种负载)
- **WeChatFriendForDualWindow-debug** - 双Window刷新测试模块 (10种负载)
- **WeChatFriendForMixedRender-debug** - 混合渲染测试模块 (10种负载)
- **WeChatFriendForGLMap-debug** - OpenGL地图测试模块 (10种负载)

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

详细配置说明请查看 [RELEASE_SETUP.md](RELEASE_SETUP.md)

### 签名配置
如需配置APK签名，请参考 [SIGNING_CONFIG_EXAMPLE.md](SIGNING_CONFIG_EXAMPLE.md)


# 项目截图
![main_page.jpg](pic/main_page.jpg)
![friends_1.jpg](pic/friends_1.jpg)
![friends_2.jpg](pic/friends_2.jpg)
![trace.png](pic/trace.png)
