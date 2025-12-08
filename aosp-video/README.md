# 视频朋友圈模块

本模块是基于 `wechatfriendforperformance` 项目扩展而来，添加了视频播放支持，用于测试Android列表中视频滑动播放的性能和用户体验。

*Read this in [English](README_EN.md)*

## 功能特点

- **视频与图片混合展示**：
  - 列表中伪随机出现视频内容（使用固定种子，确保每次进入视频位置一致）
  - 视频类型的item显示单个视频，上方有文字描述，下方有评论区
  - 图片类型的item保持原有的九宫格图片展示

- **智能视频播放控制**：
  - **滑动过程中不播放**：优化滑动性能，避免滑动时视频加载影响流畅度
  - **滑动停止后自动播放**：停止滑动后，自动播放可视区域内可见比例最高的视频
  - **离开视野自动暂停**：视频滑出用户视野后自动暂停，并显示暂停图标
  - **点击切换播放/暂停**：用户可以点击视频区域手动控制播放状态

- **10种负载级别**：
  - **最轻负载 (Minimal)**：无额外计算负载
  - **帧内负载 (In-Frame)**：
    - 轻负载：基础计算任务
    - 中负载：中等计算任务
    - 高负载：密集计算任务
  - **帧间负载 (Between-Frame)**：
    - 轻负载：30%概率执行，200次循环计算
    - 中负载：50%概率执行，400次循环计算
    - 高负载：70%概率执行，800次循环计算
  - **混合负载 (Mixed)**：
    - 轻负载混合：doFrame 1000 + 帧间 2400
    - 中负载混合：doFrame 2000 + 帧间 1600
    - 高负载混合：doFrame 4000 + 帧间 1067

## 技术实现

### 视频播放器
- 使用 `androidx.media3:media3-exoplayer` 进行视频播放
- 支持循环播放模式
- 无控制器UI，简洁的播放界面

### 视频位置算法
```java
// 使用固定种子的随机数，确保每次进入时视频位置一致
private static final long VIDEO_POSITION_SEED = 12345L;

// 大约每5-8个item中有一个视频
int nextVideoOffset = 5 + videoRandom.nextInt(4);
```

### 可见性检测
- 计算item的可见比例
- 可见比例超过50%才会自动播放
- 可见比例低于30%时自动暂停

### 负载配置中心
所有负载参数统一在 `LoadConfig` 类中管理：
- 任务间隔：16-83ms (1-5帧)
- 固定随机种子确保测试可重现

## 如何使用

1. 从主界面选择要测试的负载级别
2. 滑动列表查看内容
3. 停止滑动后，可见的视频会自动播放
4. 点击视频可以手动控制播放/暂停
5. 使用Android Profiler或Perfetto收集性能数据

## 项目结构

```
wechatfriendforvideo/
├── src/main/
│   ├── java/com/example/wechatfriendforvideo/
│   │   ├── adapters/
│   │   │   ├── NineImageAdapter.java
│   │   │   └── VideoFriendCircleAdapter.java
│   │   ├── beans/
│   │   │   └── ...
│   │   ├── config/
│   │   │   └── LoadConfig.java
│   │   ├── utils/
│   │   │   └── VideoSpanUtils.java
│   │   ├── widgets/
│   │   │   └── NineGridView.java
│   │   ├── BaseVideoActivity.java           # 基类Activity
│   │   ├── MinimalLoadVideoActivity.java    # 最轻负载
│   │   ├── LightLoadVideoActivity.java      # 轻负载(帧内)
│   │   ├── MediumLoadVideoActivity.java     # 中负载(帧内)
│   │   ├── HeavyLoadVideoActivity.java      # 高负载(帧内)
│   │   ├── LightLoadBetweenFramesVideoActivity.java   # 轻负载(帧间)
│   │   ├── MediumLoadBetweenFramesVideoActivity.java  # 中负载(帧间)
│   │   ├── HeavyLoadBetweenFramesVideoActivity.java   # 高负载(帧间)
│   │   ├── LightMixedLoadVideoActivity.java  # 轻负载(混合)
│   │   ├── MediumMixedLoadVideoActivity.java # 中负载(混合)
│   │   ├── HeavyMixedLoadVideoActivity.java  # 高负载(混合)
│   │   ├── VideoDataCenter.java
│   │   ├── VideoConstants.java
│   │   └── VideoMainActivity.java
│   └── res/
│       ├── layout/
│       │   ├── item_friend_circle_image.xml
│       │   ├── item_friend_circle_video.xml
│       │   └── ...
│       └── raw/
│           └── video1-8.mp4
└── build.gradle
```

## 依赖项

```gradle
// ExoPlayer for video playback
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'androidx.media3:media3-ui:1.2.1'
implementation 'androidx.media3:media3-common:1.2.1'
```

## 安装方法

APK文件位于项目根目录的 `apk/` 文件夹下：
- `wechatfriendforvideo-debug.apk`

使用adb命令安装：
```bash
adb install apk/wechatfriendforvideo-debug.apk
```

## 与主项目的关系

这是[高性能微信朋友圈测试Demo](../README.md)的子模块，基于性能测试模块扩展，专注于视频播放功能的测试。项目保持了与原性能测试模块相同的10种负载级别配置。
