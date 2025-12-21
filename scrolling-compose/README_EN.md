# Compose Friend Circle Test Module

This module is developed using **Jetpack Compose**, implementing the same Friend Circle interface and load logic as the traditional View version.

## Compose vs Traditional View

### Jetpack Compose (This Module)
- **Declarative UI Framework**: Describe UI through composable functions
- **Kotlin Development Language**: Modern Kotlin syntax
- **State-driven Rendering**: Automatic recomposition mechanism
- **No XML Layouts**: Code is UI

### Traditional View (wechatfriendforperformance)
- **Imperative UI Framework**: Manipulate Views through XML and Java/Kotlin
- **RecyclerView + Adapter Pattern**
- **Manual State Management**

## Technical Implementation

### Core Components
- `LazyColumn`: List component replacing RecyclerView
- `Coil`: Compose-friendly image loading library
- `Navigation Compose`: Navigation component
- `Material3`: Material Design 3 component library

### Load Simulation
Maintains the same load logic as the traditional View version:
- Uses `Canvas` and `Paint` for drawing operations
- Uses `Handler` for scheduled task dispatching
- Uses `@Volatile` to prevent compiler optimization

## Use Cases

1. **Framework Comparison Testing**: Compare rendering performance between Compose and traditional View
2. **Modern Development Experience**: Experience declarative UI development
3. **Power Analysis**: Analyze battery impact of different UI frameworks
4. **Learning Reference**: Sample code for Compose performance testing app

## Features

- **Multi-level Load Testing**: Same load configurations as traditional View version
- **Identical UI**: Replicate WeChat Moments interface as closely as possible
- **Same Load Logic**: Uses identical computational load and data generation logic

## How to Use

1. Install this APK on test device
2. Select the load level to test from main interface
3. Observe UI performance while scrolling the list
4. Collect performance data using Perfetto or similar tools
5. Compare with traditional View version

## ADB Commands

```bash
# Start light load test
adb shell am start -n com.example.wechatfriendforcompose/.MainActivity --es activity_type light

# Start mixed load test
adb shell am start -n com.example.wechatfriendforcompose/.MainActivity --es activity_type light_mixed
```

## Project Structure

```
wechatfriendforcompose/
├── src/main/java/com/example/wechatfriendforcompose/
│   ├── MainActivity.kt              # Main Activity
│   ├── ComposeApplication.kt        # Application class
│   ├── config/
│   │   └── LoadConfig.kt            # Load configuration
│   ├── data/
│   │   ├── Models.kt                # Data models
│   │   ├── LoadType.kt              # Load type enum
│   │   ├── ComposeConstants.kt      # Constants
│   │   └── ComposeDataCenter.kt     # Data center
│   └── ui/
│       ├── theme/
│       │   └── Theme.kt             # Theme configuration
│       ├── navigation/
│       │   └── Navigation.kt        # Navigation configuration
│       ├── screens/
│       │   ├── MainScreen.kt        # Main screen
│       │   └── FriendCircleScreen.kt # Friend circle list
│       └── components/
│           └── NineGridImages.kt    # Nine grid images component
└── build.gradle                      # Build configuration
```

## Relationship with Main Project

This is a submodule of the [High Performance WeChat Moments Test Demo](../README.md), focused on Compose framework performance testing.


