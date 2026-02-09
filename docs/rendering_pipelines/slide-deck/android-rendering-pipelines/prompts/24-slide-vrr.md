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

**Slide**: 24 - VRR Pipeline
**Filename**: 24-slide-vrr.png
**Type**: Content
**Layout**: diagram-centered

**Headline**: Variable Refresh Rate (VRR) Pipeline

**Key Content**:

**核心架构**: App setFrameRate → SF → VSync Generator → HWC LTPO Panel

**关键组件**:
- VSync Generator: 动态周期生成器
- DisplayManager: 帧率协调
- LTPO Panel: 硬件支持 (1Hz~120Hz)

**VSync 对比**:
| 模式 | VSync 周期 | 特点 |
|------|-----------|------|
| 固定 60Hz | 16.6ms 恒定 | 静态时仍 60Hz |
| VRR | 1ms ~ 100ms 动态 | 静态可降至 1Hz |

**API 演进**:
- Android 11: setFrameRate(120f)
- Android 16: setFrameRateCategory(HIGH_HINT)

**Visual Description**:
动态帧率调度时序图:

上半部分 - 固定 VSync vs VRR:
```
固定 60Hz:
|--16.6ms--|--16.6ms--|--16.6ms--|--16.6ms--|
    F1        F2         F3         F4

VRR 动态:
|--8.3ms--|--8.3ms--|------33ms------|--8.3ms--|
   F1       F2        F3 (静态)        F4
 ^120Hz   ^120Hz      ^30Hz          ^120Hz
```

下半部分 - 调度流程:
1. App → setFrameRate(120fps) / setFrameRateCategory(HIGH)
2. SF → Configure VSync Period (8.3ms)
3. Loop: Vsync → App Draw → SF Commit
4. 检测静态 → SF Extend Period (100ms) → 10Hz
5. 用户滑动 → SF Reduce Period (8.3ms) → 120Hz

底部 API 对比:
- Android 11-15: setFrameRate(fps, compatibility)
- Android 16+: setFrameRateCategory(category) 简化版

**Narrative Goal**: 展示 VRR 的动态帧率调度机制

---

Please use nano banana pro to generate the slide image based on the content provided above.
