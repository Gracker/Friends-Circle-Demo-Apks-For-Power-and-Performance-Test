# Slide 1: 01-slide-cover.md
<STYLE_INSTRUCTIONS>
- **Texture**: Subtle grid background, technical blueprint aesthetic.
- **Mood**: Cool blue and gray tones, professional and analytical.
- **Typography**: Geometric sans-serif.
- **Visuals**: Blueprint-style system diagrams.
- **Density**: Balanced.
</STYLE_INSTRUCTIONS>
Slide: 01-slide-cover
Type: Cover
Title: Android 渲染管线全景图
Subtitle: 从 VSync 到屏幕扫描：深度解析现代 Android 绘图架构
Visual: A high-level blueprint diagram of an Android device screen with technical lines and nodes connecting to a central "Graphics Pipeline" core. Use blue-line aesthetic on a dark grid background.

---

# Slide 2: 02-slide-history.md
<STYLE_INSTRUCTIONS>
(Same as slide 1)
</STYLE_INSTRUCTIONS>
Slide: 02-slide-history
Type: Content
Title: Android 渲染架构简史
Content:
- BufferQueue 时代 (Android 9-)
- BLAST 架构引入 (Android 10-14)
- 现代架构：BLAST + ANGLE + VPA (Android 15+)
Visual: A technical timeline with three major nodes, each stylized as a blueprint module. Transitions between them shown with dashed lines and arrows.

---

# Slide 3: 03-slide-android-16.md
<STYLE_INSTRUCTIONS>
(Same as slide 1)
</STYLE_INSTRUCTIONS>
Slide: 03-slide-android-16
Type: Content
Title: Android 16 (Baklava) 核心变更
Content:
- 增强型 ARR (Adaptive Refresh Rate)
- 运行时颜色滤镜 (RuntimeColorFilter)
- GPU 系统调用过滤
Visual: A grid of three blueprint icons representing dynamic waves (ARR), a color prism (Filter), and a shield/lock (Security/Filtering).

---

# Slide 4: 04-slide-overview.md
<STYLE_INSTRUCTIONS>
(Same as slide 1)
</STYLE_INSTRUCTIONS>
Slide: 04-slide-overview
Type: Content
Title: 渲染管线全景架构
Content:
- 生产者 (App/Camera/WebView)
- 中介 (BufferQueue/BLAST)
- 消费者 (SurfaceFlinger/HWC)
Visual: A clear technical flowchart showing the "Producer -> Mediator -> Consumer" path with blueprint labels and connecting lines.

---

# Slide 5: 05-slide-standard-view.md
<STYLE_INSTRUCTIONS>
(Same as slide 1)
</STYLE_INSTRUCTIONS>
Slide: 05-slide-standard-view
Type: Content
Title: Standard View 管线
Content:
- UI Thread: Measure/Layout/Draw
- RenderThread: Sync/Issue Commands
- GPU Drawing: Skia-GLES/Vulkan
Visual: Three vertical columns representing the internal app pipeline. Blueprint-style rectangles with connectivity.

---

# Slide 18: 18-slide-back-cover.md
<STYLE_INSTRUCTIONS>
(Same as slide 1)
</STYLE_INSTRUCTIONS>
Slide: 18-slide-back-cover
Type: Back-Cover
Title: 掌控每一帧
Subtitle: 深入管线，优化无界
Visual: A clean blueprint-style "Closing" graphic, perhaps a stylized "End of Frame" or "Scanline Complete" visual on a dark grid.
