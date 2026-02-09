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
- Warning: Amber #F59E0B

---

## SLIDE CONTENT

**Slide**: 16 - Flutter TextureView Pipeline
**Filename**: 16-slide-flutter-textureview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Flutter TextureView Pipeline (PlatformView)

**Key Content**:

**核心架构**: Raster → SurfaceTexture → Main → Android RT → SF

**触发条件**: 使用 PlatformView (如 Google Maps, WebView)

**关键组件**:
- SurfaceTexture: 纹理中转站
- Android RenderThread: 最终合成者

**性能影响**:
- ⚠️ PlatformView 触发降级
- ⚠️ 双重绘制开销
- Flutter 3.29+ 消除锁竞争
- 仍受 Vsync 延迟影响

**Visual Description**:
降级模式时序图:

1. Raster Thread:
   - Impeller Rasterize
   - queueBuffer(Frame N) → SurfaceTexture

2. SurfaceTexture → onFrameAvailable() → Main Thread

3. Main Thread → invalidate()

4. Android Vsync → RenderThread:
   - updateTexImage() ⚠️ (Copy)
   - Draw View Hierarchy (含 Flutter Texture)
   - queueBuffer(App Window)

5. SF → Composite → HWC

用琥珀色标注:
- "Main Thread Roundtrip"
- "Double Draw" (Flutter 画一次 + Android 画一次)

底部对比: SurfaceView (1次绘制) vs TextureView (2次绘制)

**Narrative Goal**: 说明 PlatformView 导致的性能降级

---

Please use nano banana pro to generate the slide image based on the content provided above.
