# GeckoView SurfaceTexture 版朋友圈

## 项目简介

本项目使用 Mozilla GeckoView (Firefox 引擎) 实现 WebView 朋友圈，采用 **TextureView/SurfaceTexture** 作为渲染载体。

## 渲染模式

### SurfaceTexture 模式特点

- **渲染路径**: GeckoView → SurfaceTexture → App RenderThread → SurfaceFlinger
- **合成方式**: SurfaceTexture 作为 GL 纹理被应用的 RenderThread 合成
- **经过**: 应用的 UI Thread + RenderThread
- **类似应用**: 国内很多浏览器

### 技术实现

1. 创建 TextureView 并设置 SurfaceTextureListener
2. 当 SurfaceTexture 可用时，从 SurfaceTexture 创建 Surface
3. 使用 `GeckoDisplay.surfaceChanged()` 将渲染输出绑定到该 Surface
4. GeckoView 渲染到 SurfaceTexture，由应用的 RenderThread 作为纹理合成

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
./gradlew :wechatfriendforwebviewtexture:assembleDebug

# APK 输出位置
wechatfriendforwebviewtexture/build/outputs/apk/debug/wechatfriendforwebviewtexture-debug.apk
```

## 依赖

- GeckoView 133.0.20241202233018 (arm64-v8a)
- AndroidX AppCompat
- Material Components

## 性能分析

使用 Perfetto 可以观察到：
- GeckoView 渲染输出到 SurfaceTexture
- 应用的 RenderThread 负责将 SurfaceTexture 作为纹理进行合成
- 合成路径经过应用的 UI Thread + RenderThread

## 注意事项

- 需要 arm64-v8a 架构的设备
- GeckoView 首次启动需要一定时间初始化
- 建议使用 Android 8.0 (API 26) 及以上版本

