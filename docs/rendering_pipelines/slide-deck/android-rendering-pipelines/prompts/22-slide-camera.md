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

**Slide**: 22 - Camera2 HAL3 Pipeline
**Filename**: 22-slide-camera.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Camera Rendering Pipeline (Camera2 & HAL3)

**Key Content**:

**核心架构**: CameraService → HAL3/ISP → Multi-Surface (零拷贝)

**关键组件**:
- CameraService: 系统服务 (资源管理)
- HAL3/ISP: 硬件图像信号处理器
- 多消费者: Preview / Record / Analysis

**多流输出**:
1. Preview → SurfaceView → SF → Display
2. Record → MediaCodec → H.264/265
3. Analysis → ImageReader → App (CV/AI)

**技术要点**:
- 多流并发 (Multi-Stream)
- 零拷贝 GraphicBuffer
- ZSL (Zero Shutter Lag)
- Buffer 生命周期管理

**Visual Description**:
一产多销流程图:

顶部 - 配置阶段:
```
App → createCaptureSession(Preview, Record, Analysis)
App → setRepeatingRequest()
```

中部 - HAL 生产 (单一来源):
```
         ┌──────────────────────┐
         │  Camera HAL / ISP    │
         │  Sensor → ISP        │
         └──────────────────────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
```

下部 - 多消费者 (并行输出):
```
   Preview        Record        Analysis
      │              │              │
 SurfaceView   MediaCodec    ImageReader
      │              │              │
      ▼              ▼              ▼
     SF          Encoder        App AI
```

右侧 - ZSL 说明:
- "环形缓冲区"
- "按快门 → 捞最近一帧"

**Narrative Goal**: 展示 Camera 的多流并发架构

---

Please use nano banana pro to generate the slide image based on the content provided above.
