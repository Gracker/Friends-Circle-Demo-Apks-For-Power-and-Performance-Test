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

**Slide**: 09 - SurfaceControl NDK API
**Filename**: 09-slide-surfacecontrol-ndk.png
**Type**: Content
**Layout**: code-diagram

**Headline**: SurfaceControl API Deep Dive (NDK)

**Key Content**:

**核心架构**: ASurfaceControl + ASurfaceTransaction → SF

**关键组件**:
- ASurfaceControl: Layer 句柄
- ASurfaceTransaction: 原子操作集
- AHardwareBuffer: Buffer 容器

**API 流程**:
1. ASurfaceControl_create(parent, "MyOverlay")
2. ASurfaceTransaction_setBuffer(buffer, fence)
3. ASurfaceTransaction_setPosition(x, y)
4. ASurfaceTransaction_setZOrder(10)
5. ASurfaceTransaction_apply() → SF

**技术要点**:
- Android 10+ NDK API
- 原子 Transaction 操作
- Frame Timeline API (Android 11+)
- 支持动态帧率控制

**Visual Description**:
左侧 - API 调用流程图:
- create → setBuffer → setPosition → setZOrder → apply
- 每步用代码风格的方框表示

右侧 - 数据流向:
- App Process: ASurfaceControl + ASurfaceTransaction
- → Binder
- System Process: SurfaceFlinger Layer Tree

底部 - Frame Timeline 示意:
- getNextFrameInfo() → vsyncId
- setFrameTimeline(vsyncId) → 精准着陆

**Narrative Goal**: 展示 SurfaceControl NDK API 的使用方式和能力

---

Please use nano banana pro to generate the slide image based on the content provided above.
