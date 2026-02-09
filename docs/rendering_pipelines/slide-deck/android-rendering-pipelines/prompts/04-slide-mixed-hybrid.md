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
- Tertiary: Light Blue #BFDBFE

**Visual Elements**: Precise lines, 90-degree angles, grid alignment, parallel structures

---

## SLIDE CONTENT

**Slide**: 04 - Mixed/Hybrid Pipeline
**Filename**: 04-slide-mixed-hybrid.png
**Type**: Content
**Layout**: split-comparison

**Headline**: Android View Mixed Pipeline (Hybrid Composition)

**Key Content**:

**核心架构**: 并行双管线

**Pipeline A** (View System):
UI Thread → RenderThread → Layer 0 (App Window, 带透明洞)

**Pipeline B** (Media Content):
Producer Thread → SurfaceView → Layer -1 (视频内容)

**技术要点**:
- 打洞机制 (Hole Punching)
- 双管线并行不阻塞
- BLAST Sync 解决同步问题
- UI 卡顿不影响视频流畅

**Visual Description**:
左右对称的并行流程图：

左侧 (Pipeline A - View):
- UI Thread → RenderThread → SF (Layer 0)
- 标注 "透明洞" 区域

右侧 (Pipeline B - Media):
- Video Decoder → SurfaceView → SF (Layer -1)
- 标注视频内容区域

底部合并：
- SurfaceFlinger → HWC Composite
- 展示两个 Layer 叠加的效果

使用不同深浅的蓝色区分两条管线

**Narrative Goal**: 展示混合渲染模式下两条独立管线如何并行工作

---

Please use nano banana pro to generate the slide image based on the content provided above.
