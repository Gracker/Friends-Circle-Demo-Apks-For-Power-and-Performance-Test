# 高性能微信朋友圈测试 Demo

本项目是一个基于微信朋友圈 UI 的性能测试平台，旨在研究 Android 滑动性能和功耗表现。项目包含四个主要模块，分别用于不同方面的测试和研究。

*Read this in [English](README_EN.md)*

![main_page.jpg](pic/main_page.jpg)
![friends_1.jpg](pic/friends_1.jpg)
![friends_2.jpg](pic/friends_2.jpg)
![trace.png](pic/trace.png)

## 项目结构

本项目包含四个主要模块:

### 1. 原始项目 (app)

原始的高性能微信朋友圈实现，来自 fork 的项目。这个模块展示了如何高效实现类似微信朋友圈的滑动列表，包含多种性能优化技巧。

### 2. 性能测试模块 (wechatfriendforperformance)

专门设计用于测试和比较不同负载下的滑动性能表现。包含三种负载模式：

- **轻负载测试**: 每帧计算量小，滑动流畅度高
- **中等负载测试**: 每帧计算量中等，滑动有一定压力
- **高负载测试**: 每帧计算量大，滑动压力重

该模块在关键代码处添加了 Trace 点，方便使用 Perfetto 等工具进行性能分析和优化。

### 3. 功耗测试模块 (wechatfriendforpower)

专门设计用于测试不同滑动实现方案对设备功耗的影响。可用于研究优化策略对电池寿命的影响。

### 4. WebView测试模块 (wechatfriendforwebview)

使用WebView实现朋友圈界面，用于测试WebView与原生实现在性能方面的差异。包含三种负载级别：

- **轻负载WebView**: 基础渲染，无额外负载
- **中负载WebView**: 添加适量JavaScript和DOM操作
- **重负载WebView**: 添加密集JavaScript计算和DOM操作

该模块实现了JavaScript与Java交互，支持动态加载最多200条朋友圈数据，解决了滑动到底部闪烁的问题。

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
   - 选择不同的负载级别
   - 使用 Perfetto 或其他性能分析工具收集数据
   - 分析 Trace 结果进行性能优化
3. 运行 `wechatfriendforpower` 模块测试功耗表现
4. 运行 `wechatfriendforwebview` 模块测试WebView性能：
   - 选择不同的负载级别
   - 体验不同负载下WebView的滑动流畅度
   - 滑动到底部测试动态加载功能

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
- 增加Compose实现版本，对比更多技术方案

欢迎 Star 和贡献！

## 项目状态

[![Android CI](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml/badge.svg)](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml)
