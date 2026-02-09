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

**Slide**: 11 - Vulkan Native Pipeline
**Filename**: 11-slide-vulkan.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Vulkan Native Rendering Pipeline

**Key Content**:

**核心架构**: App → VkSwapchain → GPU Queue → BLAST → SF

**关键组件**:
- VkSurfaceKHR: Android Surface 对应
- VkSwapchainKHR: 交换链 (Buffer 管理)
- VkSemaphore: GPU 同步原语

**Presentation Modes**:
| Mode | VSync | 延迟 | 撕裂 |
|------|-------|------|------|
| FIFO | 严格 | 高 | 无 |
| MAILBOX | 有 | 低 | 无 |
| IMMEDIATE | 无 | 极低 | 有 |

**技术要点**:
- 显式控制一切
- Swappy Frame Pacing
- VPA (Vulkan Profiles for Android)

**Visual Description**:
Vulkan 提交时序图:

1. vkAcquireNextImageKHR(Semaphore: S_ImgAvail)
   - → 获取 ImageIndex
2. Record CommandBuffer
3. vkQueueSubmit:
   - Wait: S_ImgAvail
   - Signal: S_RenderDone
4. vkQueuePresentKHR:
   - Wait: S_RenderDone
5. Driver Internal: queueBuffer → BLAST → SF

用虚线框表示 GPU 内部工作:
- Wait S_ImgAvail → Execute Draw → Signal S_RenderDone

底部: Swappy 帧节奏示意

**Narrative Goal**: 展示 Vulkan 的显式控制模型和同步机制

---

Please use nano banana pro to generate the slide image based on the content provided above.
