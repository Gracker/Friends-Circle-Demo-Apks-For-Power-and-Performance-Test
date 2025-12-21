# GeckoView ImageReader 版朋友圈

## 项目简介

本项目使用 Mozilla GeckoView (Firefox 引擎) 实现 WebView 朋友圈，采用 **ImageReader** 作为渲染载体。

## 渲染模式

### ImageReader 模式特点

- **渲染路径**: GeckoView → ImageReader → Image → Bitmap → ImageView → App RenderThread → SurfaceFlinger
- **合成方式**: 从 ImageReader 获取图像数据，转换为 Bitmap 后在 ImageView 中显示
- **经过**: 应用的 UI Thread + RenderThread
- **类似应用**: 淘宝天猫页面

### 技术实现

1. 创建 ImageReader 并获取其 Surface
2. 使用 `GeckoDisplay.surfaceChanged()` 将渲染输出绑定到 ImageReader 的 Surface
3. 在 ImageReader 的回调中获取 Image，转换为 Bitmap
4. 在 ImageView 中显示 Bitmap，由应用的 RenderThread 渲染

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
./gradlew :wechatfriendforwebviewimagereader:assembleDebug

# APK 输出位置
wechatfriendforwebviewimagereader/build/outputs/apk/debug/wechatfriendforwebviewimagereader-debug.apk
```

## 依赖

- GeckoView 133.0.20241202233018 (arm64-v8a)
- AndroidX AppCompat
- Material Components

## 性能分析

使用 Perfetto 可以观察到：
- GeckoView 渲染输出到 ImageReader 的 Surface
- ImageReader 回调中进行 Image → Bitmap 转换
- Bitmap 在 ImageView 中显示
- 整个合成路径经过应用的 UI Thread + RenderThread

## 注意事项

- 需要 arm64-v8a 架构的设备
- GeckoView 首次启动需要一定时间初始化
- ImageReader 模式会有额外的内存和 CPU 开销用于图像转换
- 建议使用 Android 8.0 (API 26) 及以上版本

