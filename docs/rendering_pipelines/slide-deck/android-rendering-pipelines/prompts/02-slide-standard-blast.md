Create a presentation slide image following these guidelines:

## Image Specifications

- **Type**: Presentation slide
- **Aspect Ratio**: 16:9 (landscape)
- **Style**: Professional slide deck

## Core Persona: The Architect

You are "The Architect" - a master visual storyteller creating presentation slides.

## Core Principles

- Technical blueprint aesthetic - NO realistic or photographic elements
- NO slide numbers, page numbers, footers, headers, or logos
- Clean, uncluttered layouts with clear visual hierarchy
- Each slide conveys ONE clear message

## Language

- Use Chinese (中文) for all text elements

---

## STYLE_INSTRUCTIONS

**Style**: blueprint (grid + cool + technical + balanced)

**Background**: Blueprint Off-White (#FAF8F5) with subtle grid overlay

**Typography**: Neue Haas Grotesk Display Pro (headlines), Tiempos Text (body)

**Color Palette**:
- Primary Text: Deep Slate #334155
- Primary Accent: Engineering Blue #2563EB
- Secondary Accent: Navy Blue #1E3A5F
- Tertiary: Light Blue #BFDBFE

**Visual Elements**:
- Precise lines with consistent stroke weights
- Technical schematics and clean vector graphics
- Connection lines use straight lines or 90-degree angles only
- Grid alignment for all elements

---

## SLIDE CONTENT

**Slide**: 02 - Standard BLAST Pipeline
**Filename**: 02-slide-standard-blast.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Standard AOSP Rendering Pipeline (BLAST)

**Key Content**:

**核心架构**:
UI Thread → RenderThread → BLASTBufferQueue → SurfaceFlinger → HWC

**组件说明**:
- 生产者: UI Thread (DisplayList) + RenderThread (GPU Commands)
- 消费者: SurfaceFlinger
- 关键线程: UI Thread, RenderThread, SF Main

**技术要点** (4个要点):
- Android 12+ BLAST 取代传统 BufferQueue
- Triple Buffering 支持
- FrameTimeline Jank Detection
- 原子 Transaction 提交

**Visual Description**:
时序图风格的流程图，从左到右展示：
1. Vsync 信号 → UI Thread (Record DisplayList)
2. UI Thread → Sync → RenderThread
3. RenderThread: dequeueBuffer → GPU Draw → queueBuffer
4. BLASTAdapter → Transaction → SurfaceFlinger
5. SurfaceFlinger → HWC → Display

使用蓝色箭头表示数据流，每个阶段用矩形框表示，关键同步点用菱形标记

**Narrative Goal**: 展示 Android 标准硬件加速渲染的完整链路

---

Please use nano banana pro to generate the slide image based on the content provided above.
