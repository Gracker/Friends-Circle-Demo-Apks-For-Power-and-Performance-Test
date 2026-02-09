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

**Slide**: 21 - Video Overlay HWC Pipeline
**Filename**: 21-slide-video-overlay.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Video Overlay Pipeline (MediaCodec → HWC)

**Key Content**:

**核心架构**: Decoder → Surface → HWC Layer → Display (Bypass GPU)

**关键组件**:
- MediaCodec: 视频解码器
- HWC Hardware Plane: 硬件图层
- Secure Memory: DRM 保护内存

**路径对比**:
| 路径 | 流程 | GPU 参与 |
|------|------|----------|
| GPU Path | Decoder → Texture → GPU → FB → Display | ✅ 参与 |
| Overlay Path | Decoder → Surface → HWC → Display | ❌ 绕过 |

**技术要点**:
- 最省电最高效视频播放
- GPU 完全不参与
- DRM L1 唯一路径 (Secure Memory)
- YUV 原生支持 (省去 YUV→RGB 转换)

**Visual Description**:
Overlay vs GPU 合成对比图:

上半部分 - GPU Path (TextureView):
```
Decoder → SurfaceTexture → GPU Shader (Sample)
        → FrameBuffer → Display
        [GPU 参与，耗电]
```

下半部分 - Overlay Path (SurfaceView + HWC):
```
Decoder → Surface → HWC Layer → Display
        [GPU 绕过，省电]
```

右侧决策流程:
1. SF 检查: "HWC 有空闲硬件图层?"
2. 有 → HWC_COMPOSITION (Overlay) ✅
3. 无 → GLES_COMPOSITION (GPU 回退) ⚠️

DRM 说明框:
- "Widevine L1: 必须使用 Overlay"
- "Secure Memory: GPU 无法读取"

**Narrative Goal**: 展示最高效的视频播放路径

---

Please use nano banana pro to generate the slide image based on the content provided above.
