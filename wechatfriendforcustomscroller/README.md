# 自定义 Scroller 朋友圈测试模块

*Read this in [English](README_EN.md)*

本模块完全移除了 RecyclerView/ListView/OverScroller，改以自研 `CustomTimelineView` + `CustomOverScroller` 模拟微博、QQ 等 App 的私有滚动栈，便于在 OEM 未做额外优化的情况下复现真实滑动、功耗与抖动表现。

## 模块亮点
- ⚙️ **自研滚动容器**：`CustomTimelineView` 负责测量/布局全部 item，`CustomOverScroller` 手写惯性衰减、越界钳制，不再依赖系统 OverScroller
- 🧱 **稳定 UI**：UI 结构与 AOSP 朋友圈保持一致（含顶部 header），可与其他 APK 做一一对照
- 🧠 **MVVM + Hilt + Room**：`CustomScrollViewModel` + `FriendCircleRepository` + `Room` 缓存 100 条固定数据，配合 `CustomScrollDataGenerator` 保证三档负载可复现
- 🔋 **轻/中/重三档压力**：在渲染、CPU 与 GPU 三个层面模拟真实波动，方便 Perfetto/ADB Systrace 抓取
- 🧼 **可扩展的负载模拟**：`FriendCircleItemRenderer` 在绑定阶段注入额外的 bitmap 绘制和数学计算，可自定义负载策略

## 架构概览
| 层 | 组件 | 说明 |
| --- | --- | --- |
| UI | `CustomScrollMainActivity` / `CustomScrollFeedActivity` | 负责轻/中/重入口与滚动界面展示 |
| View | `CustomTimelineView` / `CustomOverScroller` / `FriendCircleItemRenderer` | 纯手写 ViewGroup、Scroller 与绑定逻辑 |
| VM | `CustomScrollViewModel` | 通过 Hilt 注入仓库，暴露 `LiveData<CustomScrollUiState>` |
| Repo | `FriendCircleRepository` | 首次启动生成数据并写入 Room，后续直接从本地读取 |
| Data | `FriendCircleDatabase` / `FriendCircleDao` / `FriendCircleEntity` | Room schema，使用 Gson 将复杂朋友圈结构序列化 |

## 运行指南
1. 通过 `./gradlew :wechatfriendforcustomscroller:assembleDebug` 生成 `apk/wechatfriendforcustomscroller-debug.apk`
2. 安装后在主界面选择 **轻 / 中 / 重** 任意档位
3. 在 `CustomTimelineView` 中滑动，使用 Perfetto / FrameLayout 帧分析工具观察自研滚动栈表现
4. 若需要重新生成固定数据，重装 App 或在设置中清空数据即可

## 自定义 Scroller 细节
- `CustomTimelineView`：
  - 预加载全部 item，统一测量、布局并通过 `scrollOffset` 控制位置
  - 覆写触摸与惯性逻辑，完全自托管 `VelocityTracker`、`touchSlop`、`fling`
  - 通过 `CustomOverScroller` 运行 60fps 的惯性动画，并允许注入 header
- `CustomOverScroller`：
  - 手动维护速度/位移/衰减，避免 OEM 对系统 OverScroller 的魔改
  - 支持越界钳制与低速截止，方便与系统实现对照
- 负载策略：
  - 轻载：仅做必要的文本/图片绑定
  - 中载：附加 4k 次数学运算，逼真模拟 CPU 抢占
  - 重载：在中载基础上再执行 bitmap 绘制，压榨 GPU/内存带宽

## 与主项目的关系
- `wechatfriendforcustomscroller` 与 `wechatfriendforperformance` 一样复用同一批资源、数据生成策略，方便横向对比
- 该模块生成的 APK 默认存放在仓库根目录 `apk/` 中，可直接 push 到设备测试