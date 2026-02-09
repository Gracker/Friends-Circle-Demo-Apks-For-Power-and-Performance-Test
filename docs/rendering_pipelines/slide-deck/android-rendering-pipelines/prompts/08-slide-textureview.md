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
- Warning: Amber #F59E0B (用于性能开销标记)

---

## SLIDE CONTENT

**Slide**: 08 - TextureView Copy Pipeline
**Filename**: 08-slide-textureview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: TextureView Rendering Pipeline (App-side Composition)

**Key Content**:

**核心架构**: Producer → SurfaceTexture → App Main → App RenderThread → SF

**组件说明**:
- 生产者: Decoder/Camera Producer
- 中转站: SurfaceTexture (OES Texture)
- 消费者: App RenderThread

**技术要点**:
- onFrameAvailable 回调切主线程
- updateTexImage 纹理转录
- ⚠️ 双重绘制开销
- ⚠️ 受主线程卡顿影响

**Visual Description**:
纹理搬运流程图，强调多次数据转手：

1. Producer → queueBuffer → SurfaceTexture (Internal BQ)
2. SurfaceTexture → onFrameAvailable → Main Thread
3. Main Thread → invalidate → 等待 Vsync
4. Vsync → RenderThread → updateTexImage (⚠️ Copy)
5. RenderThread → Draw UI + Texture → queueBuffer
6. BLAST → SF

用琥珀色标注两处性能开销:
- "主线程回调"
- "纹理拷贝 (Copy)"

底部对比: SurfaceView (直送) vs TextureView (中转)

**Narrative Goal**: 说明 TextureView 的灵活性代价

---

Please use nano banana pro to generate the slide image based on the content provided above.
