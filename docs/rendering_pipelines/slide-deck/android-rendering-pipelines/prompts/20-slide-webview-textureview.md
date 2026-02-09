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

**Slide**: 20 - WebView Custom TextureView Pipeline
**Filename**: 20-slide-webview-textureview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: WebView Custom TextureView Pipeline (Domestic/SDK)

**Key Content**:

**核心架构**: SDK Kernel → SurfaceTexture → App RT → SF

**常见场景**: 国内第三方 WebView SDK (腾讯 X5, UC 内核)

**关键组件**:
- 定制 Chromium 内核
- SurfaceTexture: 纹理搬运 (非代码注入)

**为什么使用**:
- 复杂 View 层级嵌入 (ListView/RecyclerView)
- 需要动画变换 (旋转/透明度/圆角)
- 修复原生 WebView 兼容性问题

**技术要点**:
- 纹理搬运模式 (vs GL Functor 的代码注入)
- 兼容复杂 View 层级
- ⚠️ 性能开销大 (多一次 Copy)

**Visual Description**:
国内 SDK 渲染流程图:

1. SDK Kernel (定制 Chromium):
   - Rasterize 网页内容
   - → SurfaceTexture (queueBuffer)

2. SurfaceTexture:
   - onFrameAvailable() → Main Thread

3. Main Thread:
   - invalidate()
   - 等待 Android Vsync

4. Android RenderThread:
   - updateTexImage() ⚠️ Copy
   - Draw Texture as View
   - queueBuffer → SF

对比框:
- GL Functor: 代码注入，共享 Context
- TextureView: 纹理搬运，独立 Context

底部标注:
- 优点: "兼容性极好，可做动画"
- 缺点: "多一次 Copy，内存占用高"

**Narrative Goal**: 说明国内 SDK 常用的 TextureView 模式

---

Please use nano banana pro to generate the slide image based on the content provided above.
