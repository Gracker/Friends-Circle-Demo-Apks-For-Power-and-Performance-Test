# RenderThread 压测朋友圈模块

*Read this in [English](README_EN.md)*

本模块与 `wechatfriendforcustomscroller` 共享同一套 MVVM + Hilt + Room 架构与 `CustomOverScroller` 滚动栈，但专注于“UI 线程很轻、RenderThread 非常忙”的极端场景。通过渲染特效覆盖层 `RenderStressOverlayView`，可以在滑动过程中持续触发高半径模糊 + Shader 合成，帮助定位 OEM 自研渲染管线的瓶颈。

## 模块亮点
- ⚙️ **滚动栈保持一致**：依旧采用 `CustomTimelineView` + `CustomOverScroller`，方便与其他 APK 进行 A/B 对比。
- 🧠 **数据层完全复用**：`CustomScrollViewModel` + `FriendCircleRepository` + Room/Room Dao，100 条固定数据确保可复现。
- 🌫 **RenderThread 专用负载**：`RenderStressOverlayView` 在 Medium/Heavy 档位下自动叠加多层模糊、Shader、波浪渐变，UI 线程仅下发指令，主要耗时落在 RenderThread。
- 🧾 **可观测的 Trace 事件**：在 `DispatchDraw` 与模糊链路中加入 `TraceSection`，配合 Perfetto / FrameMetrics 可直接看到 RenderThread backlog 超过 vsync 的情况。

## 架构概览
| 层 | 组件 | 说明 |
| --- | --- | --- |
| UI | `CustomScrollMainActivity` / `CustomScrollFeedActivity` | 选择轻/中/重负载并展示列表 |
| View | `CustomTimelineView` + `RenderStressOverlayView` | 列表负责手势/惯性，Overlay 负责渲染特效 |
| 数据 | `CustomScrollDataGenerator` + Room | 生成 deterministic 朋友圈数据，缓存到本地 |
| 控制 | `LoadProfile` | 统一描述轻/中/重负载，供滚动栈与 Overlay 共享 |

## 运行指南
1. `./gradlew :wechatfriendforrenderstress:assembleDebug`
2. 安装 `apk/wechatfriendforrenderstress-debug.apk` 并选择 Light / Medium / Heavy。
3. 滑动列表，同时在 Perfetto 中关注 `RenderThread`、`SurfaceFlinger` vsync 报告，Medium/Heavy 档位会出现明显的渲染耗时。
4. 如需复位数据，清空 App 数据或重新安装即可。

## 负载策略
- **Light**：仅展示 UI，无额外 RenderEffect。
- **Medium**：Overlay 启动中等半径模糊 + 渐变波浪，每帧轻量扰动，RenderThread 挂载 30~40px blur。
- **Heavy**：叠加 100px+ 模糊与多层合成，波浪幅度更大，极易导致 RenderThread 超出单帧预算。

## 与主项目的关系
- 与 `wechatfriendforcustomscroller` 保持一套数据 & 资源，方便只替换渲染路径进行对比。
- 产出的 APK 默认同步到仓库根目录 `apk/`，配合其它模块可构建全链路性能矩阵。