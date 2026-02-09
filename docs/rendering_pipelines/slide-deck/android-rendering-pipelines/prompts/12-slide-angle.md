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

**Slide**: 12 - ANGLE Translation Pipeline
**Filename**: 12-slide-angle.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: ANGLE Rendering Pipeline (GLES-over-Vulkan)

**Key Content**:

**核心架构**: App GLES → ANGLE Translator → Vulkan Driver → GPU

**翻译层组件**:
- ANGLE Translator: GLES API → Vulkan API
- SPIR-V Compiler: GLSL Shader → SPIR-V
- State Tracker: GL 状态机模拟

**为什么需要 ANGLE**:
| 问题 | ANGLE 解决方案 |
|------|---------------|
| 驱动碎片化 | 统一翻译层 |
| 兼容性 Bug | Google 维护 |
| 调试困难 | 开源可 Debug |

**技术要点**:
- ⚠️ Android 15+ 强制默认
- ~5-10% CPU 翻译开销
- 统一调试工具链 (RenderDoc)

**Visual Description**:
翻译层架构图:

顶层 - App Layer:
- App 代码: glDrawArrays()

中层 - ANGLE Layer (蓝色框):
- GLES → Vulkan Translator
- State Tracker
- SPIR-V Compiler

底层 - System:
- Vulkan Driver
- GPU

右侧流程:
glDrawArrays → State Validation → vkCmdDraw
eglSwapBuffers → vkQueuePresentKHR
Vulkan Driver → queueBuffer → BLAST → SF

底部警告框: "Android 15+ 强制采用"

**Narrative Goal**: 说明 ANGLE 翻译层的工作原理和必要性

---

Please use nano banana pro to generate the slide image based on the content provided above.
