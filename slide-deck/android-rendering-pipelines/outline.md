# Slide Deck Outline: Android Rendering Pipelines

- **Topic**: Android Rendering Pipelines
- **Style**: Blueprint (technical, grid, cool, geometric, balanced)
- **Audience**: Experts/Professionals
- **Language**: zh
- **Total Slides**: 18

## Style Instructions
- **Texture**: Subtle grid background, technical blueprint aesthetic.
- **Mood**: Cool blue and gray tones, professional and analytical.
- **Typography**: Geometric sans-serif (e.g., Roboto or Inter) for a modern, precise look.
- **Visuals**: Use diagrams, flowcharts, and technical schematics. Avoid stock photos; use vector-style system diagrams.

---

## Slide 1: Cover
- **Title**: Android 渲染管线全景图
- **Subtitle**: 从 VSync 到屏幕扫描：深度解析现代 Android 绘图架构
- **Type**: Cover
- **Layout**: Title-Hero

## Slide 2: Android 渲染架构简史
- **Content**: 
    - BufferQueue 时代 (Android 9-)
    - BLAST 架构引入 (Android 10-14)
    - 现代架构：BLAST + ANGLE + VPA (Android 15+)
- **Layout**: Timeline-Milestones

## Slide 3: Android 16 (Baklava) 核心变更
- **Content**: 
    - 增强型 ARR (Adaptive Refresh Rate)
    - 运行时颜色滤镜 (RuntimeColorFilter)
    - GPU 系统调用过滤
- **Layout**: Key-Features-Grid

## Slide 4: 渲染管线全景架构 (General)
- **Content**: 
    - 生产者 (App/Camera/WebView)
    - 中介 (BufferQueue/BLAST)
    - 消费者 (SurfaceFlinger/HWC)
- **Layout**: Diagram-Flow

## Slide 5: Standard View 管线
- **Content**: 
    - UI Thread: Measure/Layout/Draw
    - RenderThread: Sync/Issue Commands
    - GPU Drawing: Skia-GLES/Vulkan
- **Layout**: Three-Column-Process

## Slide 6: BLAST Buffer Queue 深度解析
- **Content**: 
    - 废弃传统 BufferQueue
    - SurfaceControl Transaction 异步提交
    - 降低延迟与功耗的关键
- **Layout**: Technical-Comparison

## Slide 7: WebView 渲染架构 (Process Isolation)
- **Content**: 
    - App Process (UI/RT)
    - Chromium Render Process (Compositor/Raster)
    - GPU Process (Viz/SurfaceControl)
- **Layout**: Component-Map

## Slide 8: WebView Pipelines (Four Modes)
- **Content**: 
    - GL Functor (Sync Waiting)
    - SurfaceView Wrapper (Video)
    - SurfaceControl (Direct/Hole Punch)
    - Custom TextureView (Copy)
- **Layout**: Matrix-Table

## Slide 9: Flutter 3.29+ 架构演进
- **Content**: 
    - Impeller 渲染引擎默认开启
    - 线程合并 (UI & Platform Thread Merging)
    - 解决长周期任务阻塞
- **Layout**: Architecture-Focus

## Slide 10: SurfaceView vs TextureView
- **Content**: 
    - SurfaceView: 独立 Surface, 零拷贝, 无主线程依赖
    - TextureView: SurfaceTexture, App 侧合成, 灵活但性能损耗
- **Layout**: Pros-Cons-Comparison

## Slide 11: OpenGL ES 与 ANGLE 翻译层
- **Content**: 
    - Android 15 强制采用 ANGLE (GLES-over-Vulkan)
    - 标准化硬件路径
    - 性能与兼容性的平衡
- **Layout**: Layer-Diagram

## Slide 12: Vulkan Native 管线
- **Content**: 
    - Vulkan Profiles (VPA)
    - 开发者可控性更高的 GPU 资源管理
    - BLAST 事务同步
- **Layout**: Code-Level-Schematic

## Slide 13: 动态刷新率 (VRR) 管线
- **Content**: 
    - 从 60Hz 到 120Hz/ARR
    - FrameTimeline 追踪帧起止
    - 硬件合成器 (HWC) 的动态切换
- **Layout**: Chart-Performance

## Slide 14: 相机渲染管线 (Camera Pipeline)
- **Content**: 
    - Camera2 API 与 HAL3 多流并发
    - ZSL (Zero Shutter Lag) 架构
    - GPU 实时处理链接
- **Layout**: Data-Flow-Diagram

## Slide 15: Hardware Buffer Renderer (Android 14+)
- **Content**: 
    - 替代 Canvas (Software) 的现代方案
    - 直接操控 HardwareBuffer
    - 高性能 UI 控件的福音
- **Layout**: Feature-Highlight

## Slide 16: 性能分析实战 (How to trace?)
- **Content**: 
    - Perfetto: 追踪 drawFrame 与 SF 交互
    - dumpsys SurfaceFlinger: 检查 Buffer 状态
    - Choreographer 挂钩点监控
- **Layout**: Checklist-Action

## Slide 17: 未来演进与工程师视角
- **Content**: 
    - WebView SC 深度模拟需求
    - Flutter 负载对等测试
    - 纯 Vulkan 路径的全覆盖
- **Layout**: Summary-Points

## Slide 18: 结语
- **Title**: 掌控每一帧
- **Subtitle**: 深入管线，优化无界
- **Type**: Back-Cover
- **Layout**: Title-Hero
