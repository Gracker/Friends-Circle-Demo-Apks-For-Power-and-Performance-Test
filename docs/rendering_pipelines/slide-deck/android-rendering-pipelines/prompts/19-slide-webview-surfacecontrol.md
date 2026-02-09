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

**Slide**: 19 - WebView SurfaceControl/Viz Pipeline
**Filename**: 19-slide-webview-surfacecontrol.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: WebView SurfaceControl Pipeline (Viz/OOP-R)

**Key Content**:

**核心架构**: Viz Thread → ASurfaceControl → SF (独立 Layer)

**启用条件**: Vulkan 后端 或 OOP-R (Out-of-Process Rasterization)

**关键组件**:
- Viz Thread: Chromium GPU 进程
- ASurfaceControl: 独立 Layer 句柄
- Hole Punching: App 侧透明洞

**性能优势**:
- 完全绕过 App RenderThread
- 网页卡死不影响 App UI
- 视频可走 HWC Overlay

**Visual Description**:
独立合成架构图:

上半部分 - 双层结构:
```
┌─────────────────────────────────────┐
│  App Window (Layer 0)               │
│  ┌─────────────────────────────┐    │
│  │   Transparent Hole          │    │
│  │   (Hole Punching)           │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  WebView Content (Layer -1)         │
│  (Viz Thread 独立渲染)              │
└─────────────────────────────────────┘
```

下半部分 - 时序图:
1. Viz Thread (Chromium GPU Process):
   - Surface Aggregation
   - Draw (Vulkan/GL)
   - queueBuffer → BLAST(Layer=-1)

2. App UI (独立):
   - Transaction(Layer=0, 透明洞)

3. SF:
   - Latch Both Layers
   - HWC Composite (Overlay 优先)

底部标注: "App RenderThread 完全空闲"

**Narrative Goal**: 展示现代 WebView 的独立合成模式

---

Please use nano banana pro to generate the slide image based on the content provided above.
