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

**Slide**: 15 - Flutter SurfaceView Pipeline
**Filename**: 15-slide-flutter-surfaceview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Flutter SurfaceView Pipeline (Impeller/BLAST)

**Key Content**:

**核心架构**: Dart Runner → Raster Thread → BLAST → SF (独立 Layer)

**阶段一 - Dart Runner (UI)**:
- Build: Widget.build() → Element Tree
- Layout: RenderObject.performLayout()
- Paint: RenderObject.paint() → Layer Tree

**阶段二 - Raster Thread**:
- LayerTree Processing
- Impeller Rasterize (Vulkan/Metal)
- vkQueuePresentKHR / eglSwapBuffers

**技术要点**:
- 零拷贝直送 SF
- Impeller AOT Shader (无首帧卡顿)
- ⚠️ PlatformView 会触发降级
- 不经过 App RenderThread

**Visual Description**:
Flutter 渲染时序图:

1. Vsync → Main Thread → Engine.ScheduleFrame()
2. Dart Runner:
   - Build → Layout → Paint
   - → Submit LayerTree
3. Raster Thread:
   - dequeueBuffer → acquireFence
   - Impeller GPU Rasterize
   - queueBuffer(releaseFence)
4. BLAST Adapter:
   - Acquire → Transaction(Buffer)
5. SF (Wait releaseFence) → latchBuffer → HWC

底部标注: "完全不经过 Android App RenderThread"

右侧警告: "PlatformView 存在时自动降级"

**Narrative Goal**: 展示 Flutter 高性能 SurfaceView 模式的工作流程

---

Please use nano banana pro to generate the slide image based on the content provided above.
