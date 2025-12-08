# WeChat Friend Circle Demo

## 📊 Project Status

[![Android CI/CD](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml/badge.svg)](https://github.com/Gracker/HighPerformanceFriendsCircle/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/Gracker/HighPerformanceFriendsCircle?label=Release&color=brightgreen)](https://github.com/Gracker/HighPerformanceFriendsCircle/releases)
[![API Level](https://img.shields.io/badge/API-21%2B-blue.svg)](https://android-arsenal.com/api?level=21)
[![Gradle](https://img.shields.io/badge/Gradle-8.2.2-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/Gracker/HighPerformanceFriendsCircle/total?label=Downloads&color=orange)](https://github.com/Gracker/HighPerformanceFriendsCircle/releases)
[![Stars](https://img.shields.io/github/stars/Gracker/HighPerformanceFriendsCircle?label=Stars&color=yellow)](https://github.com/Gracker/HighPerformanceFriendsCircle/stargazers)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

This project is a performance testing platform based on WeChat Friend Circle UI, designed to study Android scrolling performance and power consumption. The project now ships with multiple specialized APKs that cover CPU-heavy, RenderThread-heavy, power-sensitive and WebView scenarios.

*Read this in [Chinese](README.md)*

## Load Types

All test modules support 10 load types covering different performance testing scenarios:

| Type | Load Name | Description |
|------|-----------|-------------|
| Minimal | Minimal Load | No additional load |
| In-Frame Light | In-Frame Light Load | Light computation within each frame |
| In-Frame Medium | In-Frame Medium Load | Medium computation within each frame |
| In-Frame Heavy | In-Frame Heavy Load | Heavy computation within each frame |
| Between-Frame Light | Between-Frame Light Load | Light tasks between frames |
| Between-Frame Medium | Between-Frame Medium Load | Medium tasks between frames |
| Between-Frame Heavy | Between-Frame Heavy Load | Heavy tasks between frames |
| Mixed Light | Mixed Light Load | Both in-frame and between-frame light loads |
| Mixed Medium | Mixed Medium Load | Both in-frame and between-frame medium loads |
| Mixed Heavy | Mixed Heavy Load | Both in-frame and between-frame heavy loads |

## Project Structure

The project is organized by implementation type:

### AOSP Modules (Standard Android UI + RenderThread)

| Module Directory | Description |
|-----------------|-------------|
| `app` | Original project with random Friend Circle display |
| `aosp-performance` | Performance testing with 10 load types |
| `aosp-power` | Power consumption testing with fixed content |
| `aosp-picasso` | Using Picasso image loading library |
| `aosp-customscroller` | Custom CustomOverScroller + CustomTimelineView |
| `aosp-renderstress` | RenderThread stress testing |
| `aosp-softwarerender` | Software rendering (hardware acceleration disabled) |
| `aosp-douyin` | Douyin-style video scrolling |
| `aosp-video` | Video Feed version |
| `aosp-purerenderthread` | Pure RenderThread list scrolling |
| `aosp-dualwindow` | Dual Window rendering demo |
| `aosp-mixedrender` | Mixed rendering demo |

### Compose Module

| Module Directory | Description |
|-----------------|-------------|
| `compose` | Jetpack Compose implementation |

### WebView Modules

| Module Directory | Description |
|-----------------|-------------|
| `webview` | Standard WebView implementation |
| `webview-surface` | WebView + SurfaceView |
| `webview-texture` | WebView + TextureView |
| `webview-imagereader` | WebView + ImageReader |

### Surface & GL Modules

| Module Directory | Description |
|-----------------|-------------|
| `surface-map` | SurfaceView Map Demo |
| `gl-map` | OpenGL ES 2.0 Map Demo |

## Module Details

### 1. Original Project (app)

The original high-performance WeChat Friend Circle implementation, from the forked project. This module demonstrates how to efficiently implement a scrolling list similar to WeChat Friend Circle, including various performance optimization techniques.

### 2. Performance Testing Module (aosp-performance)

Specifically designed to test and compare scrolling performance under different loads. Supports 10 load modes with Trace points at key code locations for performance analysis using tools like Perfetto.

### 3. Power Consumption Testing Module (aosp-power)

Single Activity design with fixed environment and content for precise power consumption testing.

### 4. WebView Testing Module (webview)

Implements the Friend Circle interface using WebView to test performance differences between WebView and native implementation. Supports 10 load levels with JavaScript-Java interaction and dynamic loading of up to 200 items.

### 5. Custom Scroller Module (aosp-customscroller)

- Replaces RecyclerView/ListView with custom `CustomTimelineView` + `CustomOverScroller`
- Built with Hilt + MVVM + Room data pipeline
- Supports 10 load types for evaluating OEM optimizations

### 6. RenderThread Stress Module (aosp-renderstress)

- Based on custom scroller architecture with `CustomOverScroller`
- `RenderStressOverlayView` applies blur/shader chains during scrolling
- Supports 10 load types for GPU/vsync analysis

### 7. Software Rendering Module (aosp-softwarerender)

Software rendering mode implementation (hardware acceleration disabled):

- **Hardware Acceleration Disabled**: Configured via `android:hardwareAccelerated="false"`
- **UI Thread Only**: No RenderThread, all rendering on main thread
- Supports 10 load types for CPU-intensive testing

### 8. Compose Module (compose)

Jetpack Compose implementation:

- **Declarative UI**: Built with Kotlin + Compose
- **LazyColumn**: Replaces RecyclerView
- **Coil Image Loading**: Compose-friendly image loading
- Supports 10 load types for framework performance comparison

### 9. SurfaceView Map Module (surface-map)

Amap-style map demo application:

- **SurfaceView Map**: Renders map grid on separate thread using SurfaceView
- **Native Control Overlay**: Top navigation bar and bottom control panel use native Views
- **Scroll Gesture Support**: Supports drag and fling scrolling
- Supports 10 load types for testing SurfaceView + native View mixed scenarios

### 10. Pure RenderThread Module (aosp-purerenderthread)

Pure RenderThread list scrolling implementation:

- **Zero UI Thread Rendering**: Main thread only handles touch events, no drawing
- **Dedicated Render Thread**: All drawing operations on separate rendering thread
- **SurfaceView Implementation**: Utilizes SurfaceView's independent Surface
- Supports 10 load types for validating pure render thread performance

### 11. Dual Window Module (aosp-dualwindow)

Dual window rendering demonstration:

- **Two Simultaneous Windows**: Main window + overlay window
- **Dual doFrame Callbacks**: 2 doFrame per vsync in systrace
- **Dual RenderThread**: 2 RenderThread drawFrame per vsync
- **Overlay Permission Required**: Uses WindowManager for second window
- Supports 10 load types for testing multi-window scenarios

### 12. Mixed Rendering Module (aosp-mixedrender)

Mixed rendering combining two pipelines:

- **Pure RenderThread Animation**: SurfaceView with dedicated render thread (top)
- **Standard UI+RenderThread**: RecyclerView with normal View hierarchy (bottom)
- **Simulates Video Overlay**: Like video player overlay on scrollable list
- Supports 10 load types for analyzing mixed rendering performance

### 13. OpenGL Map Module (gl-map)

OpenGL ES 2.0 map rendering demo:

- **GLSurfaceView**: Hardware-accelerated OpenGL rendering
- **Map Features**: Grid, roads, buildings, and markers
- **Gesture Support**: Pan and pinch-to-zoom
- **Native UI Overlays**: Search bar and control buttons
- Supports 10 load types for GPU-intensive testing

### 14. Douyin-style Video Module (aosp-douyin)

Simulates Douyin's full-screen video scrolling experience:

- **Full-screen Video**: Each video occupies the entire screen for immersive experience
- **Custom Scroll Container**: `VerticalVideoScroller` + `VideoOverScroller` for page-style scrolling
- **Speed-aware Animation**: Scroll duration ranges from 200ms to 600ms based on velocity
- **Smart Switching**: Auto-switch when scrolled past midpoint
- **Perfect UI Recreation**: Bottom navigation, right-side interaction buttons, bottom-left info area
- Uses Media3 ExoPlayer for video playback

## Performance Optimization Strategies

In Android, to avoid list stuttering, optimize from the following aspects:

- Reduce layout hierarchy, avoid excessive nesting of Item Views
- Control image loading during scrolling, load images after scrolling stops
- Avoid excessive computation when filling data in Adapter
- Complete data transformation in data Beans
- Reduce calls to onMeasure() and onLayout()
- Implement View object caching

## How to Use

1. Run the `app` module to view the original high-performance Friend Circle implementation
2. Run the `aosp-performance` module for performance testing:
   - Select any of the 10 load levels
   - Use Perfetto or other performance analysis tools
   - Analyze Trace results for optimization
3. Run the `aosp-power` module to test power consumption
4. Run the `webview` module to test WebView performance
5. Run the `aosp-customscroller` module to evaluate custom scroller
6. Run the `aosp-renderstress` module for RenderThread analysis

## Performance Test Comparison

By comparing performance under different implementation methods and load levels:

1. Native implementation performs excellently under all conditions
2. WebView performance degrades more noticeably as load increases
3. Dynamic loading improves user experience with large datasets
4. Optimized scrolling significantly extends battery life

## Special Thanks

Thanks to [KCrason](https://github.com/KCrason) for the original project and [razerdp](https://github.com/razerdp) for View caching concepts.

## Future Plans

- Adding more performance test metrics
- Improving power consumption test accuracy
- Implementing emoji matching
- Adding more implementation versions

Star and contributions welcome!

## Screenshots

![main_page.jpg](pic/main_page.jpg)
![friends_1.jpg](pic/friends_1.jpg)
![friends_2.jpg](pic/friends_2.jpg)
![trace.png](pic/trace.png)
