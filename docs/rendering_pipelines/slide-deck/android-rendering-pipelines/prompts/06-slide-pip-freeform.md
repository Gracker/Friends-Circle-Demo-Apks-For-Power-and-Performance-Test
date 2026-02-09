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

**Slide**: 06 - PIP & Freeform Pipeline
**Filename**: 06-slide-pip-freeform.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: PIP & Freeform Window Rendering

**Key Content**:

**核心架构**: Task Layer → Activity Layer → App Surface

**窗口层级**:
- Display Root
  - Stack / Task Container
    - Window A (Main App)
    - Window B (PIP / Freeform)

**关键机制**:
- WMS 管理窗口动画
- BLAST Sync 保证 resize 原子性

**技术要点**:
- 画中画: 独立 Task Layer
- Freeform: 动态 resize
- BLAST Sync 解决"黑边"问题
- WMS 协调 Window Bounds + Buffer

**Visual Description**:
两部分组合图：

上半部分 - 层级结构树:
- Display Root
  └── Stack/Task Container
      ├── Window A (主应用)
      └── Window B (PIP/Freeform 小窗)

下半部分 - Resize 同步时序:
1. User Drag → WMS → Config Change
2. WMS → SF (Window Bounds = 新尺寸)
3. App → Draw → SF (Buffer)
4. SF waitForSync → Atomic Apply (同时生效)

用绿色勾标记 "BLAST Sync 解决黑边"

**Narrative Goal**: 说明画中画和自由窗口的渲染同步机制

---

Please use nano banana pro to generate the slide image based on the content provided above.
