# HWUI-Picasso 性能测试应用

## 概述

这是一个基于 `wechatfriendforperformance` 的派生版本，主要区别是将图片加载库从 **Glide** 替换为 **Picasso**。

这个版本主要用于对比不同图片加载库对应用滚动性能的影响。

## 与原项目的区别

| 项目 | wechatfriendforperformance | wechatfriendforpicasso |
|------|----------------------------|------------------------|
| 图片加载库 | Glide 4.16.0 | Picasso 2.8 |
| App 名称 | HWUI-Performance | HWUI-Picasso |
| 包名 | com.example.wechatfriendforperformance | com.example.wechatfriendforpicasso |

## 为什么使用 Picasso？

Picasso 是由 Square 公司开发的图片加载库，具有以下特点：

- **简洁的 API**：API 设计简洁直观
- **体积小**：库体积约 120KB，比 Glide 更轻量
- **无 GIF 支持**：不支持 GIF 动画，如果不需要 GIF 可以减少内存占用
- **自动处理 ImageView 回收**：自动处理 ImageView 的回收问题

## 项目结构

```
wechatfriendforpicasso/
├── src/main/java/com/example/wechatfriendforpicasso/
│   ├── adapters/
│   │   ├── NineImageAdapter.java       # 九宫格图片适配器 (使用 Picasso)
│   │   └── PerformanceFriendCircleAdapter.java  # 朋友圈适配器 (使用 Picasso)
│   ├── utils/
│   │   ├── RoundedCornersTransformation.java  # Picasso 圆角变换
│   │   ├── CircleTransformation.java          # Picasso 圆形变换
│   │   └── PerformanceSpanUtils.java
│   ├── beans/                          # 数据模型
│   ├── config/                         # 负载配置
│   ├── interfaces/                     # 接口定义
│   ├── widgets/                        # 自定义控件
│   └── *Activity.java                  # 各种负载测试 Activity
└── src/main/res/                       # 资源文件
```

## 负载测试模式

应用提供 10 种不同的负载测试模式：

1. **最轻负载 (Minimal Load)** - 无额外计算负载
2. **帧内轻负载 (In-Frame Light Load)** - 渲染帧期间执行轻量计算
3. **帧内中负载 (In-Frame Medium Load)** - 渲染帧期间执行中等计算
4. **帧内重负载 (In-Frame Heavy Load)** - 渲染帧期间执行重量计算
5. **帧间轻负载 (Between-Frame Light Load)** - 帧之间执行轻量计算
6. **帧间中负载 (Between-Frame Medium Load)** - 帧之间执行中等计算
7. **帧间重负载 (Between-Frame Heavy Load)** - 帧之间执行重量计算
8. **混合轻负载 (Mixed Light Load)** - 同时执行帧内和帧间轻量计算
9. **混合中负载 (Mixed Medium Load)** - 同时执行帧内和帧间中等计算
10. **混合重负载 (Mixed Heavy Load)** - 同时执行帧内和帧间重量计算

## 构建

```bash
# 构建 Debug APK
./gradlew :wechatfriendforpicasso:assembleDebug

# 构建 Release APK
./gradlew :wechatfriendforpicasso:assembleRelease
```

## 安装

```bash
# 安装 Debug APK
adb install apk/wechatfriendforpicasso-debug.apk
```

## 使用说明

1. 启动应用后，在主界面选择要测试的负载模式
2. 进入对应的测试页面后，滚动列表观察性能
3. 使用 Perfetto 或其他性能分析工具采集数据
4. 与使用 Glide 的 `wechatfriendforperformance` 版本进行对比分析

## 自定义图片变换

由于 Picasso 没有内置的圆角和圆形变换，项目中实现了以下自定义 Transformation：

### RoundedCornersTransformation
用于实现圆角图片效果：

```java
Picasso.get()
    .load(resourceId)
    .transform(new RoundedCornersTransformation(radiusInPixels))
    .into(imageView);
```

### CircleTransformation
用于实现圆形图片效果：

```java
Picasso.get()
    .load(resourceId)
    .transform(new CircleTransformation())
    .into(imageView);
```

## 版本信息

- **版本**: 1.0.0
- **最低 Android 版本**: Android 7.0 (API 24)
- **目标 Android 版本**: Android 14 (API 34)
- **Picasso 版本**: 2.8

## 许可证

© 2025 Friends Circle Performance Test App
