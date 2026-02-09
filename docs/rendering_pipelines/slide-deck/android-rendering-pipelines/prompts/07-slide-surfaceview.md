Create a presentation slide image following these guidelines:

## Image Specifications

- **Type**: Presentation slide
- **Aspect Ratio**: 16:9 (landscape)
- **Style**: Professional slide deck - blueprint technical style

## Language

- Use Chinese (中文) for all text elements

---

## STYLE_INSTRUCTIONS

**Style**: blueprint (grid + cool + technical + balanced)

**Background**: Blueprint Off-White (#FAF8F5) with subtle grid overlay

**Color Palette**:
- Primary Text: Deep Slate #334155
- Primary Accent: Engineering Blue #2563EB
- Tertiary: Light Blue #BFDBFE

---

## SLIDE CONTENT

**Slide**: 07 - SurfaceView Direct Pipeline
**Filename**: 07-slide-surfaceview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: SurfaceView Rendering Pipeline (Direct Producer)

**Key Content**:

**核心架构**: Producer Thread → BLASTBufferQueue → SF (独立 Layer)

**组件说明**:
- 生产者: Video/Game Producer Thread
- 消费者: SurfaceFlinger (非 App 进程)

**技术要点**:
- 打洞机制 (Z=-1)
- 零拷贝直送 SF
- BLAST Sync 完美同步
- ANR 不影响视频播放

**Visual Description**:
组合图展示：

左侧 - Z-Order 层级示意:
- Layer 0: App Window (带透明洞，用虚线框表示)
- Layer -1: SurfaceView Content (填充颜色表示视频)
- 箭头指示 "打洞穿透"

右侧 - 数据流时序:
1. Producer Thread → dequeue → Draw (EGL/Vulkan) → queue
2. BLASTAdapter → Transaction(Layer=-1) → SF
3. App UI → Transaction(Layer=0, 透明洞)
4. SF → Composite Layers → HWC

底部标注: "完全不经过 App 主线程/RenderThread"

**Narrative Goal**: 说明 SurfaceView 的高效直接渲染机制

---

Please use nano banana pro to generate the slide image based on the content provided above.
