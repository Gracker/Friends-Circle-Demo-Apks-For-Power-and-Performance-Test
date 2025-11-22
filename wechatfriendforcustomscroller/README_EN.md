# Custom Scroller Friend Circle Module

*阅读[中文版](README.md)*

This module removes RecyclerView/ListView/OverScroller entirely and rebuilds the timeline with our own `CustomTimelineView` + `CustomOverScroller`. It mirrors the AOSP UI but behaves like custom implementations used in Weibo/QQ so you can measure “no OEM optimization” scenarios precisely.

## Highlights
- ⚙️ **Full custom scrolling stack** – hand-written ViewGroup + scroller, manual velocity tracking, inertial decay and edge clamping
- 🧠 **MVVM + Hilt + Room** – `CustomScrollViewModel` orchestrates a Room-backed cache so every load profile always returns the same 100 items
- 🔋 **Three load profiles** – light / medium / heavy combine text/image binding, math workloads and bitmap drawing to stress CPU & GPU separately
- ♻️ **Deterministic data generation** – reuses the original data center with fixed seeds so comparisons with other APKs stay fair

## Architecture
| Layer | Component | Notes |
| --- | --- | --- |
| UI | `CustomScrollMainActivity`, `CustomScrollFeedActivity` | Load selector + scrolling page |
| View | `CustomTimelineView`, `CustomOverScroller`, `FriendCircleItemRenderer` | Custom ViewGroup, fling logic, binding code |
| ViewModel | `CustomScrollViewModel` | Exposes `LiveData<CustomScrollUiState>` via Hilt |
| Repository | `FriendCircleRepository` | Generates data on first launch, persists to Room afterwards |
| Data | `FriendCircleDatabase`, `FriendCircleDao`, `FriendCircleEntity` | Room schema with JSON payload per item |

## Usage
1. Build with `./gradlew :wechatfriendforcustomscroller:assembleDebug`
2. Install `apk/wechatfriendforcustomscroller-debug.apk`
3. Pick light / medium / heavy on the landing page
4. Scroll the feed and capture traces with Perfetto / Android Studio Profiler

## Custom Scrolling Details
- `CustomTimelineView` measures/layouts every item once, then repositions them by adjusting an internal `scrollOffset`
- `CustomOverScroller` implements friction, velocity decay and clamping without touching framework Scroller/OverScroller
- Load simulation happens inside `FriendCircleItemRenderer`, injecting CPU math loops and bitmap drawing to emulate real workloads

## Relation to the Main Project
`wechatfriendforcustomscroller` is part of the [High Performance WeChat Friend Circle Demo](../README_EN.md). It shares assets/data with the performance module so that results remain comparable, while offering a brand-new scrolling stack for OEM-level investigations.