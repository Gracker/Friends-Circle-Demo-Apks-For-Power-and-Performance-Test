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

**Slide**: 13 - HardwareBufferRenderer Pipeline
**Filename**: 13-slide-hardwarebufferrenderer.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: HardwareBufferRenderer Pipeline (Android 14+)

**Key Content**:

**核心架构**: RecordingCanvas → HardwareBufferRenderer → GPU → HardwareBuffer → SF

**关键组件**:
- HardwareBufferRenderer: GPU 离屏渲染器
- RenderNode: 绘制指令树
- HardwareBuffer: GPU 可直写 Buffer

**vs lockCanvas() 对比**:
| 特性 | lockCanvas | HBR |
|------|------------|-----|
| 渲染引擎 | CPU (Skia) | GPU |
| 内存拷贝 | 2x | 零拷贝 |
| HDR 支持 | ❌ | ✅ |
| Fence 控制 | 隐式 | 显式 |

**技术要点**:
- Android 14+ 新 API
- 零拷贝 GPU 直写
- 支持 HDR/RGBA_F16
- 显式 Fence 控制

**Visual Description**:
GPU 离屏渲染流程图:

1. App → obtainRenderRequest()
2. draw(RenderNode) → Record Commands
3. HardwareBufferRenderer → GPU Rasterize
4. → HardwareBuffer (GPU 直写)
5. App → setBuffer(HardwareBuffer, Fence) → BLAST
6. BLAST → Transaction → SF → HWC

左侧对比框:
- lockCanvas: CPU 渲染 → 拷贝 → Buffer
- HBR: GPU 渲染 → 直写 Buffer

**Narrative Goal**: 展示 Android 14 新 API 的高效离屏渲染能力

---

Please use nano banana pro to generate the slide image based on the content provided above.
