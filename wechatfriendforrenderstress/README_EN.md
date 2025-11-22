# RenderThread Stress Friend Circle Module

*阅读[中文版](README.md)*

This APK keeps the exact same MVVM + Room + `CustomOverScroller` stack as the custom scroller module, but focuses on reproducing scenarios where the **UI thread stays light** while the **RenderThread/GPU workload explodes**. A translucent `RenderStressOverlayView` injects heavy blur & shader effects whenever the list is actively scrolling or flinging, so Perfetto / FrameMetrics clearly show RenderThread overruns.

## Highlights
- ⚙️ **Same scrolling stack** – `CustomTimelineView` + handwritten `CustomOverScroller`, making A/B comparisons straightforward.
- 🧠 **Shared data pipeline** – deterministic Room cache with 100 feed items, identical to the other modules.
- 🌫 **RenderThread-specific load** – overlay dynamically applies large-radius blur and gradient waves; Medium / Heavy profiles increase shader complexity while keeping UI-thread work minimal.
- 🔎 **Trace-friendly** – overlay drawing sections emit Trace markers, letting you correlate RenderThread stalls, SurfaceFlinger vsync misses, and overlay activity.

## Architecture Overview
| Layer | Component | Notes |
| --- | --- | --- |
| UI | `CustomScrollMainActivity`, `CustomScrollFeedActivity` | Entry screen + feed |
| View | `CustomTimelineView` + `RenderStressOverlayView` | Timeline handles gestures; overlay handles render effects |
| Data | `CustomScrollDataGenerator`, Room, Gson | Same deterministic dataset as other modules |
| Control | `LoadProfile` | Three load tiers shared across timeline & overlay |

## Usage
1. `./gradlew :wechatfriendforrenderstress:assembleDebug`
2. Install `apk/wechatfriendforrenderstress-debug.apk`
3. Select Light / Medium / Heavy, start scrolling, and capture Perfetto traces – Medium/Heavy should show RenderThread spans > 1 vsync.
4. Clear app data or reinstall to regenerate the fixed dataset if needed.

## Load Profiles
- **Light** – overlay disabled; baseline scrolling only.
- **Medium** – modest blur radius plus animated wave gradients; RenderThread workload increases but stays within budget on most devices.
- **Heavy** – 100px+ blur plus stacked gradients; RenderThread & SurfaceFlinger often exceed a single vsync.

## Relation to the Main Project
This module lives beside the [main demo](../README_EN.md) and the custom scroller APK. Because it shares assets/data, you can swap APKs and immediately compare CPU-heavy vs render-heavy scenarios on the same hardware. The resulting APK is also copied to the repo root `apk/` directory for quick deployment.