# 软件渲染朋友圈测试模块

本模块是专门用于测试**软件渲染**（禁用硬件加速）模式下Android滑动性能的应用。

*Read this in [English](README_EN.md)*

## 软件渲染 vs 硬件渲染

### 硬件渲染（默认）
- 使用 **UI Thread** + **RenderThread** 双线程架构
- GPU 加速绑定渲染
- 现代 Android 设备的默认渲染模式

### 软件渲染（本模块）
- 仅使用 **UI Thread** 进行渲染
- **没有 RenderThread**
- 所有绑制操作在主线程完成
- 适合测试 CPU 密集型场景

## 技术实现

通过在 `AndroidManifest.xml` 中设置 `android:hardwareAccelerated="false"` 禁用硬件加速：

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

## 使用场景

1. **性能对比测试**：对比软件渲染和硬件渲染的帧率差异
2. **CPU 压力测试**：测试在没有 GPU 加速情况下的 CPU 负载
3. **兼容性测试**：测试在不支持硬件加速设备上的表现
4. **功耗分析**：分析不同渲染模式对电池的影响

## 功能特点

- **多层次负载测试**：与硬件渲染版本相同的负载配置
- **UI 完全一致**：与 wechatfriendforperformance 模块界面完全相同
- **负载逻辑相同**：使用相同的计算负载和数据生成逻辑

## 如何使用

1. 安装此 APK 到测试设备
2. 从主界面选择要测试的负载级别
3. 在滑动列表时观察UI性能表现
4. 使用 Perfetto 等工具收集性能数据
5. 对比与硬件渲染版本的差异

## ADB 命令启动

```bash
# 启动轻负载测试
adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light

# 启动混合负载测试
adb shell am start -n com.example.wechatfriendforsoftwarerender/.SoftwareRenderMainActivity --es activity_type light_mixed
```

## 预期性能差异

在软件渲染模式下，预期会看到：
- 帧率降低（尤其在高负载场景）
- UI Thread 占用时间增加
- 无 RenderThread 相关的 Trace 信息
- CPU 使用率可能更高

## 与主项目的关系

这是[高性能微信朋友圈测试Demo](../README.md)的子模块，专注于软件渲染性能测试。


