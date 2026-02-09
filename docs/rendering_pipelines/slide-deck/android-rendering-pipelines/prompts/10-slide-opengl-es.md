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

**Slide**: 10 - OpenGL ES Pipeline
**Filename**: 10-slide-opengl-es.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: OpenGL ES Rendering Pipeline (GL Thread)

**Key Content**:

**核心架构**: GLThread → EGL → GPU → BLASTBufferQueue → SF

**关键组件**:
- EGL: Native Window Bridge (连接 GLES 与 Surface)
- GLThread: 独立渲染线程
- Fence: 同步原语 (Acquire/Release)

**渲染循环**:
1. Wait Vsync / requestRender
2. eglMakeCurrent
3. glDraw* (GPU Commands)
4. eglSwapBuffers → queueBuffer

**技术要点**:
- eglSwapBuffers 关键提交点
- Acquire/Release Fence 机制
- Triple Buffering
- ⚠️ Android 15+ 强制 ANGLE

**Visual Description**:
GL 渲染循环时序图:

1. Vsync/requestRender → GLThread 唤醒
2. eglMakeCurrent (绑定 Context)
3. glDrawArrays/glDrawElements (GPU 指令写入 Command Buffer)
4. eglSwapBuffers:
   - Flush GL Commands
   - queueBuffer → BLAST
5. SF → Wait releaseFence → latchBuffer → HWC

右侧小框展示 Fence 机制:
- acquireFence: SF 用完 → App 可写
- releaseFence: GPU 画完 → SF 可读

底部警告: "Android 15+ ANGLE 成为默认"

**Narrative Goal**: 展示 OpenGL ES 渲染循环的完整流程

---

Please use nano banana pro to generate the slide image based on the content provided above.
