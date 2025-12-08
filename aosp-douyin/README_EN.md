# TikTok-Style Video Scroll Demo

*阅读中文版本 [README.md](README.md)*

This module implements TikTok-style full-screen video scrolling using custom `VerticalVideoScroller` + `VideoOverScroller` for vertical paging effect.

## Module Highlights

- 🎬 **Full-Screen Video**: Each video fills the entire screen for immersive experience
- 📜 **Custom Scroll Container**: `VerticalVideoScroller` implements paging scroll with custom touch and fling logic
- ⏱️ **Velocity-Aware Animation**: Scroll duration ranges from 200ms - 600ms based on scroll velocity, matching TikTok's feel
- 🎯 **Smart Switching**: Auto-switch to next video when scrolled past half
- 🎨 **Perfect UI Recreation**: Bottom navigation, right-side action buttons, bottom-left info area all match TikTok style
- 🎵 **ExoPlayer**: Uses Media3 ExoPlayer for video playback with auto-loop

## Core Components

### VideoOverScroller

Custom scroller designed for video paging:
- Duration range: 200ms - 600ms
- Velocity threshold: 500 - 8000 px/s
- Uses `DecelerateInterpolator` for natural deceleration

### VerticalVideoScroller

Full-screen paging scroll container:
- Extends `ViewGroup` with fully custom layout logic
- Self-managed `VelocityTracker`, `touchSlop`, fling animation
- Supports page change callbacks for video play/pause control

## UI Description

### Main Interface
- **Top**: Tab bar (Follow, City, Group Buy, Mall, Recommend)
- **Center**: Full-screen video playback area
- **Right Side**: Avatar+Follow, Like, Comment, Favorite, Share, Music Disc
- **Bottom Left**: Author name, video description, tags
- **Bottom**: Navigation bar (Home, Friends, +, Messages, Me)

### Interaction
- **Swipe Up**: Switch to next video
- **Swipe Down**: Switch to previous video
- Auto-switch when scrolled past half screen
- Fast swipe also triggers switch

## Technical Implementation

### Scroll Duration Calculation
```java
// Calculate scroll duration based on velocity
// Faster velocity = shorter duration; slower = longer
private int calculateDuration(int velocity) {
    if (velocity <= MIN_VELOCITY) {
        return MAX_SCROLL_DURATION; // 600ms
    }
    if (velocity >= MAX_VELOCITY) {
        return MIN_SCROLL_DURATION; // 200ms
    }
    // Linear interpolation
    float ratio = (float) (velocity - MIN_VELOCITY) / (MAX_VELOCITY - MIN_VELOCITY);
    return (int) (MAX_SCROLL_DURATION - ratio * (MAX_SCROLL_DURATION - MIN_SCROLL_DURATION));
}
```

## Running Guide

1. Build with `./gradlew :wechatfriendfordouyin:assembleDebug`
2. Install and open the app, first video plays automatically
3. Swipe up/down to switch videos
4. Observe scroll animation smoothness and timing

## Dependencies

- **Media3 ExoPlayer**: Video playback
- **Glide**: Image loading
- **Material Design**: UI components

