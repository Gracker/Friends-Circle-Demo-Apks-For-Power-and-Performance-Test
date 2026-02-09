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
- Secondary Accent: Navy Blue #1E3A5F

---

## SLIDE CONTENT

**Slide**: 14 - Flutter Architecture Index
**Filename**: 14-slide-flutter-architecture.png
**Type**: Content
**Layout**: overview-grid

**Headline**: Flutter Rendering Architecture (Index)

**Key Content**:

**核心架构**: Flutter 3.29+ Merged Platform Model

**版本演进对比**:
| 特性 | Flutter 3.19 (Legacy) | Flutter 3.29+ (Modern) |
|------|----------------------|------------------------|
| 渲染引擎 | Skia | Impeller (Vulkan/Metal) |
| 线程模型 | 独立 1.ui Thread | Merged Model (UI 跑在 Main) |
| Trace 特征 | MessageLoop::Run | Looper::pollOnce |

**关键线程**:
- Dart Runner: UI 任务 (Build/Layout/Paint)
- Raster Thread: 光栅化 (Impeller)

**两种模式**:
- SurfaceView 模式: 高性能默认
- TextureView 模式: PlatformView 降级

**Visual Description**:
Flutter 线程模型架构图:

上半部分 - 线程结构:
```
┌─────────────────────────────┐
│     Unified Main Looper     │
│  ┌───────┐  ┌────────────┐  │
│  │ Main  │──│ Dart Runner│  │
│  │Thread │  │   (UI)     │  │
│  └───────┘  └────────────┘  │
└─────────────────────────────┘
           │ LayerTree
           ▼
┌─────────────────────────────┐
│      Raster Thread          │
│      (Impeller GPU)         │
└─────────────────────────────┘
           │
           ▼
         BLAST → SF
```

下半部分 - 两种模式对比:
- SurfaceView: 直送 SF (高性能)
- TextureView: 经过 Android RT (有开销)

**Narrative Goal**: 概述 Flutter 3.29+ 的现代渲染架构

---

Please use nano banana pro to generate the slide image based on the content provided above.
