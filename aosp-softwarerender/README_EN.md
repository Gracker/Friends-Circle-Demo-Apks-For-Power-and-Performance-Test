# Software Render Friend Circle Test Module

This module is designed to test Android scrolling performance under **software rendering** (hardware acceleration disabled) mode.

## Software Rendering vs Hardware Rendering

### Hardware Rendering (Default)
- Uses **UI Thread** + **RenderThread** dual-thread architecture
- GPU accelerated rendering
- Default rendering mode for modern Android devices

### Software Rendering (This Module)
- Only uses **UI Thread** for rendering
- **No RenderThread**
- All drawing operations completed on main thread
- Suitable for testing CPU-intensive scenarios

## Technical Implementation

Hardware acceleration is disabled by setting `android:hardwareAccelerated="false"` in `AndroidManifest.xml`:

```xml
<application
    android:hardwareAccelerated="false">
    
    <activity
        android:name=".SoftwareRenderMainActivity"
        android:hardwareAccelerated="false">
        ...
    </activity>
</application>
```

## Use Cases

1. **Performance Comparison**: Compare frame rate differences between software and hardware rendering
2. **CPU Stress Testing**: Test CPU load without GPU acceleration
3. **Compatibility Testing**: Test performance on devices that don't support hardware acceleration
4. **Power Analysis**: Analyze battery impact of different rendering modes

## Features

- **Multi-level Load Testing**: Same load configurations as hardware rendering version
- **Identical UI**: Completely same interface as wechatfriendforperformance module
- **Same Load Logic**: Uses identical computational load and data generation logic

## How to Use

1. Install this APK on test device
2. Select the load level to test from main interface
3. Observe UI performance while scrolling the list
4. Collect performance data using Perfetto or similar tools
5. Compare with hardware rendering version

## ADB Commands

```bash
# Start light load test
adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light

# Start mixed load test
adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light_mixed
```

## Expected Performance Differences

In software rendering mode, expect to see:
- Lower frame rates (especially in high load scenarios)
- Increased UI Thread time
- No RenderThread related Trace information
- Potentially higher CPU usage

## Relationship with Main Project

This is a submodule of the [High Performance WeChat Moments Test Demo](../README.md), focused on software rendering performance testing.


