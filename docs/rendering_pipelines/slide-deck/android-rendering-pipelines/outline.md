# Android Rendering Pipelines - Slide Deck Outline

## Metadata

- **Topic**: Android Rendering Pipelines 技术文档
- **Style**: blueprint (grid + cool + technical + balanced)
- **Audience**: 专业开发者 (技术深度)
- **Language**: 中文
- **Total Slides**: 23

## Style Instructions

Precise technical blueprint style with professional analytical visual presentation.

**Background**: Blueprint Off-White (#FAF8F5) with subtle grid overlay
**Typography**: Neue Haas Grotesk Display Pro (headlines), Tiempos Text (body)
**Color Palette**:
- Primary Text: Deep Slate #334155
- Primary Accent: Engineering Blue #2563EB
- Secondary Accent: Navy Blue #1E3A5F
- Tertiary: Light Blue #BFDBFE
- Warning: Amber #F59E0B

**Visual Elements**:
- Precise lines with consistent stroke weights
- Technical schematics and clean vector graphics
- Connection lines use straight lines or 90-degree angles only
- Grid alignment for all elements
- Geometric precision for all shapes

---

## Slides

### Slide 1: Cover
- **Title**: Android Rendering Pipelines
- **Subtitle**: 23 种渲染管线深度解析
- **Layout**: title-hero
- **Visual**: 抽象的 Android 图形系统层次图，展示 App → Framework → HAL → Display 的流程

---

### Slide 2: Standard BLAST Pipeline
- **Title**: Standard AOSP Rendering Pipeline (BLAST)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: UI Thread → RenderThread → BLASTBufferQueue → SurfaceFlinger → HWC
  - **生产者**: UI Thread (DisplayList) + RenderThread (GPU Commands)
  - **消费者**: SurfaceFlinger
  - **关键线程**: UI Thread, RenderThread, SF Main
- **技术要点**:
  - Android 12+ BLAST 取代传统 BufferQueue
  - Triple Buffering 支持
  - FrameTimeline Jank Detection
  - 原子 Transaction 提交
- **Diagram**: 时序图展示 Vsync → UI → RT → BLAST → SF → HWC 流程

---

### Slide 3: Software Rendering Pipeline
- **Title**: Software Rendering Pipeline
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: UI Thread → Skia CPU → lockCanvas/unlockCanvasAndPost → BLAST → SF
  - **生产者**: UI Thread (Skia CPU Rasterization)
  - **消费者**: SurfaceFlinger
- **技术要点**:
  - 纯 CPU 光栅化 (Skia)
  - lockCanvas → CPU Draw → unlockCanvasAndPost
  - 支持 Dirty Rect 部分更新
  - 性能瓶颈: CPU 带宽
- **Diagram**: 简化时序图展示 CPU 光栅化流程

---

### Slide 4: Mixed/Hybrid Pipeline
- **Title**: Android View Mixed Pipeline (Hybrid Composition)
- **Layout**: split-comparison
- **Content**:
  - **核心架构**: 并行双管线
  - **Pipeline A**: UI Thread → RenderThread → Layer 0 (App Window)
  - **Pipeline B**: Producer Thread → Layer -1 (SurfaceView)
- **技术要点**:
  - 打洞机制 (Hole Punching)
  - 双管线并行不阻塞
  - BLAST Sync 解决同步问题
  - UI 卡顿不影响视频流畅
- **Diagram**: 并行流程图展示双管线同时工作

---

### Slide 5: Multi-Window Pipeline
- **Title**: Multi-Window AOSP Rendering Pipeline
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: 多窗口串行争抢 UI Thread + RenderThread
  - 多个 ViewRootImpl 共享单一 Choreographer
  - 单一 RenderThread 串行处理多窗口
- **技术要点**:
  - Dialog/分屏/悬浮窗场景
  - UI Thread 串行 performTraversals
  - RenderThread 串行 DrawFrame
  - 优化建议: 合并窗口、减少层级
- **Diagram**: 串行处理时序图

---

### Slide 6: PIP & Freeform Pipeline
- **Title**: PIP & Freeform Window Rendering
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Task Layer → Activity Layer → App Surface
  - WMS 管理窗口动画
  - BLAST Sync 保证 resize 原子性
- **技术要点**:
  - 画中画: 独立 Task Layer
  - Freeform: 动态 resize
  - BLAST Sync 解决"黑边"问题
  - WMS 协调 Window Bounds + Buffer
- **Diagram**: 层级结构图 + resize 同步时序

---

### Slide 7: SurfaceView Direct Pipeline
- **Title**: SurfaceView Rendering Pipeline (Direct Producer)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Producer Thread → BLASTBufferQueue → SF (独立 Layer)
  - **生产者**: Video/Game Producer Thread
  - **消费者**: SurfaceFlinger (非 App 进程)
- **技术要点**:
  - 打洞机制 (Z=-1)
  - 零拷贝直送 SF
  - BLAST Sync 完美同步
  - ANR 不影响视频播放
- **Diagram**: Z-Order 层级图 + 数据流向

---

### Slide 8: TextureView Copy Pipeline
- **Title**: TextureView Rendering Pipeline (App-side Composition)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Producer → SurfaceTexture → App Main → App RenderThread → SF
  - **生产者**: Decoder/Camera Producer
  - **中转站**: SurfaceTexture (OES Texture)
  - **消费者**: App RenderThread
- **技术要点**:
  - onFrameAvailable 回调切主线程
  - updateTexImage 纹理转录
  - 双重绘制开销
  - 受主线程卡顿影响
- **Diagram**: 纹理搬运流程图

---

### Slide 9: SurfaceControl NDK API
- **Title**: SurfaceControl API Deep Dive (NDK)
- **Layout**: code-diagram
- **Content**:
  - **核心架构**: ASurfaceControl + ASurfaceTransaction → SF
  - **ASurfaceControl**: Layer 句柄
  - **ASurfaceTransaction**: 原子操作集
  - **AHardwareBuffer**: Buffer 容器
- **技术要点**:
  - Android 10+ NDK API
  - 原子 Transaction 操作
  - Frame Timeline API (Android 11+)
  - 支持动态帧率控制
- **Diagram**: API 调用流程图

---

### Slide 10: OpenGL ES Pipeline
- **Title**: OpenGL ES Rendering Pipeline (GL Thread)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: GLThread → EGL → GPU → BLASTBufferQueue → SF
  - **EGL**: Native Window Bridge
  - **GLThread**: 独立渲染线程
  - **Fence**: 同步原语
- **技术要点**:
  - eglSwapBuffers 关键提交点
  - Acquire/Release Fence 机制
  - Triple Buffering
  - Android 15+ 强制 ANGLE
- **Diagram**: GL 渲染循环时序图

---

### Slide 11: Vulkan Native Pipeline
- **Title**: Vulkan Native Rendering Pipeline
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: App → VkSwapchain → GPU Queue → BLAST → SF
  - **VkSurfaceKHR**: Android Surface 对应
  - **VkSwapchainKHR**: 交换链
  - **VkSemaphore**: GPU 同步
- **技术要点**:
  - 显式控制一切
  - Presentation Mode: FIFO/MAILBOX/IMMEDIATE
  - Swappy Frame Pacing
  - VPA (Vulkan Profiles for Android)
- **Diagram**: Vulkan 提交时序图

---

### Slide 12: ANGLE Translation Pipeline
- **Title**: ANGLE Rendering Pipeline (GLES-over-Vulkan)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: App GLES → ANGLE Translator → Vulkan Driver → GPU
  - **ANGLE Translator**: GLES→Vulkan
  - **SPIR-V Compiler**: Shader 翻译
  - **Vulkan Driver**: 底层执行
- **技术要点**:
  - Android 15+ 强制默认
  - 解决驱动碎片化
  - ~5-10% CPU 翻译开销
  - 统一调试工具链
- **Diagram**: 翻译层架构图

---

### Slide 13: HardwareBufferRenderer Pipeline
- **Title**: HardwareBufferRenderer Pipeline (Android 14+)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: RecordingCanvas → HardwareBufferRenderer → GPU → HardwareBuffer → SF
  - **HardwareBufferRenderer**: GPU 离屏渲染
  - **RenderNode**: 绘制指令树
  - **HardwareBuffer**: GPU 可写 Buffer
- **技术要点**:
  - Android 14+ 新 API
  - 零拷贝 GPU 直写
  - 支持 HDR/RGBA_F16
  - 显式 Fence 控制
- **Diagram**: GPU 离屏渲染流程图

---

### Slide 14: Flutter Architecture Index
- **Title**: Flutter Rendering Architecture (Index)
- **Layout**: overview-grid
- **Content**:
  - **核心架构**: 3.29+ Merged Platform Model
  - **Dart Runner**: UI 任务 (跑在 Main Thread)
  - **Raster Thread**: 光栅化 (Impeller)
- **技术要点**:
  - Flutter 3.29+ Merged Model
  - Impeller 渲染引擎 (Vulkan/Metal)
  - SurfaceView 模式 vs TextureView 模式
  - PlatformView 自动降级
- **Diagram**: Flutter 线程模型架构图

---

### Slide 15: Flutter SurfaceView Pipeline
- **Title**: Flutter SurfaceView Pipeline (Impeller/BLAST)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Dart Runner → Raster Thread → BLAST → SF (独立 Layer)
  - **Dart Runner**: Build/Layout/Paint → LayerTree
  - **Raster Thread**: Impeller Vulkan Rasterize
- **技术要点**:
  - 零拷贝直送 SF
  - Impeller AOT Shader (无首帧卡顿)
  - PlatformView 会触发降级
  - 不经过 App RenderThread
- **Diagram**: Flutter 渲染时序图

---

### Slide 16: Flutter TextureView Pipeline
- **Title**: Flutter TextureView Pipeline (PlatformView)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Raster → SurfaceTexture → Main → Android RT → SF
  - **SurfaceTexture**: 纹理中转
  - **Android RenderThread**: 最终合成
- **技术要点**:
  - PlatformView 触发降级
  - 双重绘制开销
  - Flutter 3.29+ 消除锁竞争
  - 仍受 Vsync 延迟影响
- **Diagram**: 降级模式时序图

---

### Slide 17: WebView GL Functor Pipeline
- **Title**: WebView GL Functor Pipeline (Standard/Shared)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Renderer Process → DrawFunctor → App RenderThread
  - **AwContents**: Chromium 核心对象
  - **DrawGL Functor**: 注入 App RenderThread
  - **共享 EGLContext**
- **技术要点**:
  - 代码注入模式
  - 共享 GL Context
  - 网页复杂会拖慢 App
  - Hardware Draw Functor (Android 10+)
- **Diagram**: Functor 注入流程图

---

### Slide 18: WebView SurfaceView Wrapper Pipeline
- **Title**: WebView SurfaceView Wrapper Pipeline (App-Side/Video)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: WebChromeClient → App SurfaceView → MediaPlayer → SF
  - **onShowCustomView**: WebView 回调
  - **MediaPlayer**: 视频解码
  - **SurfaceHolder**: Buffer 容器
- **技术要点**:
  - 全屏视频播放场景
  - WebView 仅做信令通道
  - App 进程托管 Surface
  - 性能等同原生 SurfaceView
- **Diagram**: 全屏视频流程图

---

### Slide 19: WebView SurfaceControl/Viz Pipeline
- **Title**: WebView SurfaceControl Pipeline (Viz/OOP-R)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Viz Thread → ASurfaceControl → SF (独立 Layer)
  - **Viz Thread**: Chromium GPU 进程
  - **ASurfaceControl**: 独立 Layer
  - **Hole Punching**: App 透明洞
- **技术要点**:
  - Vulkan/OOP-R 启用时
  - 完全绕过 App RenderThread
  - 网页卡死不影响 App UI
  - 视频可走 HWC Overlay
- **Diagram**: 独立合成架构图

---

### Slide 20: WebView Custom TextureView Pipeline
- **Title**: WebView Custom TextureView Pipeline (Domestic/SDK)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: SDK Kernel → SurfaceTexture → App RT → SF
  - 定制 Chromium 内核 (X5/UC)
  - **SurfaceTexture**: 纹理搬运
- **技术要点**:
  - 国内 SDK 常见模式
  - 纹理搬运而非代码注入
  - 兼容复杂 View 层级
  - 性能开销大 (多一次 Copy)
- **Diagram**: 国内 SDK 渲染流程图

---

### Slide 21: Video Overlay HWC Pipeline
- **Title**: Video Overlay Pipeline (MediaCodec → HWC)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Decoder → Surface → HWC Layer → Display (Bypass GPU)
  - **MediaCodec**: 视频解码
  - **HWC Hardware Plane**: 硬件图层
  - **Secure Memory**: DRM 保护
- **技术要点**:
  - 最省电最高效视频播放
  - GPU 完全不参与
  - DRM L1 唯一路径 (Secure Memory)
  - YUV 原生支持
- **Diagram**: Overlay vs GPU 合成对比图

---

### Slide 22: Camera2 HAL3 Pipeline
- **Title**: Camera Rendering Pipeline (Camera2 & HAL3)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: CameraService → HAL3/ISP → Multi-Surface (零拷贝)
  - **CameraService**: 系统服务
  - **HAL3/ISP**: 硬件图像处理
  - **多消费者**: Preview/Record/Analysis
- **技术要点**:
  - 多流并发 (Multi-Stream)
  - 零拷贝 GraphicBuffer
  - ZSL (Zero Shutter Lag)
  - Buffer 生命周期管理
- **Diagram**: 一产多销流程图

---

### Slide 23: Game Engine Pipeline
- **Title**: Game Engine Rendering Pipeline (Unity/Unreal)
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: Logic Thread → Render Thread → GPU → BLAST → SF
  - **Logic Thread (UnityMain)**: Update/Physics/AI
  - **Render Thread (UnityGfx)**: DrawCall 提交
  - **Swappy**: 帧节奏库
- **技术要点**:
  - 双线程流水线
  - 多线程 Command Buffer
  - Swappy Frame Pacing
  - Batching 减少 DrawCall
- **Diagram**: 游戏引擎双线程流水线图

---

### Slide 24: VRR Pipeline (Bonus)
- **Title**: Variable Refresh Rate (VRR) Pipeline
- **Layout**: diagram-centered
- **Content**:
  - **核心架构**: App setFrameRate → SF → VSync Generator → HWC LTPO Panel
  - **VSync Generator**: 动态周期
  - **DisplayManager**: 帧率协调
  - **LTPO Panel**: 硬件支持
- **技术要点**:
  - Android 11+ 基础 API
  - Android 16 Enhanced ARR
  - 动态 VSync 周期 (1Hz~120Hz)
  - setFrameRateCategory 简化 API
- **Diagram**: 动态帧率调度时序图

---

## Summary

23 页技术幻灯片，覆盖 Android 渲染管线全貌：
1. **Android View 系列** (5页): Standard BLAST, Software, Mixed, Multi-Window, PIP/Freeform
2. **Surface 组件系列** (3页): SurfaceView, TextureView, SurfaceControl NDK
3. **Graphics API 系列** (4页): OpenGL ES, Vulkan, ANGLE, HardwareBufferRenderer
4. **Flutter 系列** (3页): Architecture, SurfaceView, TextureView
5. **WebView 系列** (4页): GL Functor, SurfaceView Wrapper, SurfaceControl/Viz, Custom TextureView
6. **多媒体 & 特殊场景** (4页): Video Overlay, Camera, Game Engine, VRR
