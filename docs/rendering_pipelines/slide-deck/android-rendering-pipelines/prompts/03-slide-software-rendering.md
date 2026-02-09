Create a presentation slide image following these guidelines:

## Image Specifications

- **Type**: Presentation slide
- **Aspect Ratio**: 16:9 (landscape)
- **Style**: Professional slide deck - blueprint technical style

## Core Principles

- Technical blueprint aesthetic
- NO slide numbers, page numbers, footers, headers, or logos
- Clean layouts with clear visual hierarchy

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
- Warning: Amber #F59E0B (用于性能瓶颈标记)

**Visual Elements**: Precise lines, 90-degree angles, grid alignment

---

## SLIDE CONTENT

**Slide**: 03 - Software Rendering Pipeline
**Filename**: 03-slide-software-rendering.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Software Rendering Pipeline

**Key Content**:

**核心架构**:
UI Thread → Skia CPU → lockCanvas/unlockCanvasAndPost → BLAST → SF

**组件说明**:
- 生产者: UI Thread (Skia CPU Rasterization)
- 消费者: SurfaceFlinger

**技术要点**:
- 纯 CPU 光栅化 (Skia)
- lockCanvas → CPU Draw → unlockCanvasAndPost
- 支持 Dirty Rect 部分更新
- ⚠️ 性能瓶颈: CPU 带宽

**Visual Description**:
简化的时序图，强调 CPU 密集型操作：
1. UI Thread → lockCanvas (获取 Bitmap)
2. CPU/Skia 光栅化阶段 (用较粗的线框强调)
3. unlockCanvasAndPost
4. BLAST → Transaction → SF → HWC

用琥珀色警告标记标注 "CPU 瓶颈" 区域
与硬件加速管线对比，突出软件渲染的简单但低效特点

**Narrative Goal**: 说明软件渲染的工作原理及其性能局限性

---

Please use nano banana pro to generate the slide image based on the content provided above.
