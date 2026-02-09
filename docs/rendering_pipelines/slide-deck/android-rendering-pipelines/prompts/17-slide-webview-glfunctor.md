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

**Slide**: 17 - WebView GL Functor Pipeline
**Filename**: 17-slide-webview-glfunctor.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: WebView GL Functor Pipeline (Standard/Shared)

**Key Content**:

**核心架构**: Renderer Process → DrawFunctor → App RenderThread

**关键组件**:
- AwContents: Chromium 核心对象
- DrawGL Functor: 代码注入点
- 共享 EGLContext

**工作原理**:
1. UI Thread: Record DisplayList (DrawFunctorOp 占位符)
2. RenderThread: 执行到占位符时调用 WebView 代码
3. WebView: 用 App 的 GL Context 执行渲染

**技术要点**:
- 代码注入模式 (Functor Injection)
- 共享 GL Context
- ⚠️ 网页复杂会拖慢 App
- Hardware Draw Functor (Android 10+)

**Visual Description**:
Functor 注入流程图:

1. Vsync → UI Thread
   - Build DisplayList
   - 插入 DrawFunctorOp (WebView 占位符)
   - → SyncFrameState

2. RenderThread:
   - DrawFrame 开始
   - ... 普通 View 绘制 ...
   - **Invoke Functor** ← 关键点

3. WebView (Chromium) - 用蓝色框突出:
   - Execute GL Commands
   - 使用 App 的 EGLContext
   - ⚠️ 可能很慢

4. RenderThread 继续:
   - queueBuffer → SF

底部警告: "网页 JS 卡顿 → App 整体掉帧"

**Narrative Goal**: 说明 WebView 标准模式下与 App 的深度耦合

---

Please use nano banana pro to generate the slide image based on the content provided above.
