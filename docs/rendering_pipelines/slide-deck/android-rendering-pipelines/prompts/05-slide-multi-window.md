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
- Warning: Amber #F59E0B (用于串行瓶颈标记)

---

## SLIDE CONTENT

**Slide**: 05 - Multi-Window Pipeline
**Filename**: 05-slide-multi-window.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Multi-Window AOSP Rendering Pipeline

**Key Content**:

**核心架构**: 多窗口串行争抢

**资源争抢**:
- 多个 ViewRootImpl 共享单一 Choreographer
- 单一 RenderThread 串行处理多窗口

**典型场景**:
- Dialog / 分屏 / 悬浮窗

**技术要点**:
- UI Thread 串行 performTraversals
- RenderThread 串行 DrawFrame
- ⚠️ 优化建议: 合并窗口、减少层级

**Visual Description**:
串行时序图，强调排队等待：

1. Vsync → UI Thread
2. UI Thread 串行处理:
   - performTraversals (Window A) → 完成
   - performTraversals (Window B) → 等待后执行
3. RenderThread 串行处理:
   - Draw A → queueBuffer A
   - Draw B → queueBuffer B (排队)
4. SF → Latch A & B → Composite

用琥珀色标注 "串行瓶颈" 和 "等待" 区域
用虚线框表示理想的并行状态（但实际无法实现）

**Narrative Goal**: 说明多窗口场景下的资源争抢问题

---

Please use nano banana pro to generate the slide image based on the content provided above.
