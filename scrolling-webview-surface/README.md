# GeckoView SurfaceView 版朋友圈

## 项目简介

本项目使用 Mozilla GeckoView (Firefox 引擎) 实现 WebView 朋友圈，采用 **SurfaceView** 作为渲染载体。

## 渲染模式

### SurfaceView 模式特点

- **渲染路径**: GeckoView → SurfaceView → SurfaceFlinger
- **合成方式**: 独立 Surface 直接提交到 SurfaceFlinger
- **绕过**: 应用的 UI Thread + RenderThread
- **类似应用**: Chrome App

### 技术实现

1. 创建 SurfaceView 并获取其 SurfaceHolder
2. 创建 GeckoSession 并通过 `acquireDisplay()` 获取 GeckoDisplay
3. 使用 `GeckoDisplay.surfaceChanged()` 将渲染输出绑定到 SurfaceView 的 Surface
4. GeckoView 的渲染结果直接输出到 Surface，由 SurfaceFlinger 合成

## 负载测试

本项目提供 10 种不同负载级别：

| 类别 | 负载类型 | 说明 |
|-----|---------|-----|
| 最轻负载 | 最轻负载 | 不添加任何额外负载 |
| 帧内负载 | 轻/中/重 | 在渲染帧期间执行负载 |
| 帧间负载 | 轻/中/重 | 在帧与帧之间执行负载 |
| 混合负载 | 轻/中/重 | 同时执行帧内和帧间负载 |

## 构建和运行

```bash
# 构建 debug APK
./gradlew :wechatfriendforwebviewsurface:assembleDebug

# APK 输出位置
wechatfriendforwebviewsurface/build/outputs/apk/debug/wechatfriendforwebviewsurface-debug.apk
```

## 依赖

- GeckoView 133.0.20241202233018 (arm64-v8a)
- AndroidX AppCompat
- Material Components

## 性能分析

使用 Perfetto 可以观察到：
- GeckoView 渲染直接输出到独立 Surface
- Surface 合成由 SurfaceFlinger 完成
- 应用的 RenderThread 负载较轻

## 注意事项

- 需要 arm64-v8a 架构的设备
- GeckoView 首次启动需要一定时间初始化
- 建议使用 Android 8.0 (API 26) 及以上版本

