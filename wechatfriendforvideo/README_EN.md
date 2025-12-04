# Video Friend Circle Module

This module extends the `wechatfriendforperformance` project with video playback support, designed for testing the performance and user experience of video scrolling in Android lists.

## Features

- **Mixed Video and Image Display**:
  - Pseudo-random video content in the list (uses fixed seed to ensure consistent video positions each time)
  - Video items display a single video with text description above and comments below
  - Image items maintain the original nine-grid image display

- **Intelligent Video Playback Control**:
  - **No playback during scrolling**: Optimizes scroll performance by avoiding video loading during scrolling
  - **Auto-play after scrolling stops**: Automatically plays the most visible video in the viewport
  - **Auto-pause when out of view**: Videos automatically pause when scrolled out of view, showing a pause icon
  - **Tap to toggle play/pause**: Users can manually control playback by tapping the video area

- **Resource Management**:
  - Uses ExoPlayer (Media3) for video playback
  - Automatic player lifecycle management to prevent memory leaks
  - Automatic player release when ViewHolder is recycled

## Technical Implementation

### Video Player
- Uses `androidx.media3:media3-exoplayer` for video playback
- Supports loop playback mode
- Clean playback interface without controller UI

### Video Position Algorithm
```java
// Uses fixed seed random number to ensure consistent video positions
private static final long VIDEO_POSITION_SEED = 12345L;

// Approximately one video every 5-8 items
int nextVideoOffset = 5 + videoRandom.nextInt(4);
```

### Visibility Detection
- Calculates item visibility ratio
- Auto-play only when visibility exceeds 50%
- Auto-pause when visibility drops below 30%

## How to Use

1. Click "Enter Video Friend Circle" from the main screen
2. Scroll through the list to view content
3. After scrolling stops, visible videos will auto-play
4. Tap the video to manually control play/pause

## Installation

APK file is located in the `apk/` folder at the project root:
- `wechatfriendforvideo-debug.apk`

Install using adb:
```bash
adb install apk/wechatfriendforvideo-debug.apk
```

## Dependencies

```gradle
// ExoPlayer for video playback
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'androidx.media3:media3-ui:1.2.1'
implementation 'androidx.media3:media3-common:1.2.1'
```

## Relationship with Main Project

This is a submodule of the [High Performance WeChat Friend Circle Test Demo](../README.md), extended from the performance testing module, focusing on video playback functionality testing.

