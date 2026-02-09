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

**Slide**: 18 - WebView SurfaceView Wrapper Pipeline
**Filename**: 18-slide-webview-surfaceview.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: WebView SurfaceView Wrapper Pipeline (App-Side/Video)

**Key Content**:

**核心架构**: WebChromeClient → App SurfaceView → MediaPlayer → SF

**典型场景**: 网页全屏视频播放

**关键组件**:
- onShowCustomView: WebView 回调
- MediaPlayer/ExoPlayer: 视频解码
- SurfaceHolder: Buffer 容器

**工作流程**:
1. 用户点击全屏
2. WebView → onShowCustomView(SurfaceView)
3. App 添加 SurfaceView Layer
4. MediaPlayer 解码 → queueBuffer

**技术要点**:
- 全屏视频播放场景
- WebView 仅做信令通道
- App 进程托管 Surface
- 性能等同原生 SurfaceView

**Visual Description**:
全屏视频流程图:

1. 用户触发:
   - User Click "Fullscreen" Button

2. WebView 回调:
   - WebChromeClient.onShowCustomView(View)
   - View = FrameLayout containing SurfaceView

3. App 处理:
   - Add SurfaceView to Window
   - → Transaction (Add Layer) → SF

4. 视频播放 (独立于 WebView):
   - MediaPlayer / ExoPlayer
   - Decode Frame → queueBuffer
   - → BLAST → SF → HWC

底部标注:
- "WebView 仅做信令，不参与渲染"
- "性能 = 原生 SurfaceView"

**Narrative Goal**: 说明 WebView 全屏视频的高效实现方式

---

Please use nano banana pro to generate the slide image based on the content provided above.
