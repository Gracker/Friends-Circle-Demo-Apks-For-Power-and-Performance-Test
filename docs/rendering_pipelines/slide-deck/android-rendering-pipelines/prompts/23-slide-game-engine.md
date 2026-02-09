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

**Slide**: 23 - Game Engine Pipeline
**Filename**: 23-slide-game-engine.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Game Engine Rendering Pipeline (Unity/Unreal)

**Key Content**:

**核心架构**: Logic Thread → Render Thread → GPU → BLAST → SF

**关键线程**:
| 线程 | 职责 | 典型标签 |
|------|------|----------|
| UnityMain / Logic | Update/Physics/AI | BaseBehaviour.Update |
| UnityGfx / Render | DrawCall 提交 | Camera.Render |

**渲染流水线**:
1. Logic: Input → AI → Physics → Update Transforms
2. Logic → CommandBuffer (DrawList) → Render
3. Render: Cull → Sort → Set Pass → DrawCall x1000
4. Render → eglSwap/vkPresent → BLAST → SF

**技术要点**:
- 双线程流水线 (Pipelined)
- 多线程 Command Buffer
- Swappy Frame Pacing
- Batching 减少 DrawCall

**Visual Description**:
游戏引擎双线程流水线图:

流水线结构:
```
Frame N:
┌─────────────────┐      ┌─────────────────┐
│  Logic Thread   │─────▶│  Render Thread  │
│  (UnityMain)    │      │  (UnityGfx)     │
│                 │      │                 │
│ Input/AI/Phys   │      │ Cull/Sort/Draw  │
│ Update Transforms│      │ DrawCall x1000  │
└─────────────────┘      └─────────────────┘
                                │
Frame N+1:                      ▼
┌─────────────────┐      eglSwap / vkPresent
│  Logic Thread   │             │
│  (已开始下一帧)  │             ▼
└─────────────────┘        BLAST → SF
```

右侧 - Swappy 帧节奏:
- "解决帧率不稳定"
- "精准 VSync 着陆"

**Narrative Goal**: 展示游戏引擎的并行流水线架构

---

Please use nano banana pro to generate the slide image based on the content provided above.
