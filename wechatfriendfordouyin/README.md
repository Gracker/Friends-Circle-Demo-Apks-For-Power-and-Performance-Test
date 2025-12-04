# 抖音风格视频滑动Demo

*Read this in [English](README_EN.md)*

本模块实现了类似抖音的全屏视频滑动播放功能，使用完全自研的 `VerticalVideoScroller` + `VideoOverScroller` 实现垂直翻页效果。

## 模块亮点

- 🎬 **全屏视频播放**：每个视频占满整个屏幕，沉浸式体验
- 📜 **自研滚动容器**：`VerticalVideoScroller` 实现翻页式滚动，完全自托管触摸和惯性逻辑
- ⏱️ **速度感知动画**：滑动时间与滑动速度相关，范围 200ms - 600ms，操作手感与抖音一致
- 🎯 **智能切换**：滑动过半自动切换到下一个视频
- 🎨 **完美还原UI**：底部导航栏、右侧互动按钮、左下角信息区域完全还原抖音风格
- 🎵 **ExoPlayer**：使用 Media3 ExoPlayer 进行视频播放，支持自动循环

## 核心组件

### VideoOverScroller

专为视频翻页设计的自定义滚动器：
- 滑动时间范围：200ms - 600ms
- 速度阈值：500 - 8000 px/s
- 使用 `DecelerateInterpolator` 实现自然减速效果

### VerticalVideoScroller

全屏翻页式滚动容器：
- 继承自 `ViewGroup`，完全自定义布局逻辑
- 自托管 `VelocityTracker`、`touchSlop`、惯性动画
- 支持页面切换回调，用于控制视频播放/暂停

## 界面说明

### 主界面
- **顶部**：标签栏（关注、成都、团购、商城、推荐）
- **中间**：全屏视频播放区域
- **右侧**：头像+关注、点赞、评论、收藏、分享、音乐唱片
- **左下角**：作者名称、视频描述、标签
- **底部**：导航栏（首页、朋友、+、消息、我）

### 交互说明
- **上滑**：切换到下一个视频
- **下滑**：切换到上一个视频
- 滑动超过屏幕一半会自动切换
- 快速滑动也会触发切换

## 技术实现

### 滑动时间计算
```java
// 根据滑动速度计算滑动时间
// 速度越快，时间越短；速度越慢，时间越长
private int calculateDuration(int velocity) {
    if (velocity <= MIN_VELOCITY) {
        return MAX_SCROLL_DURATION; // 600ms
    }
    if (velocity >= MAX_VELOCITY) {
        return MIN_SCROLL_DURATION; // 200ms
    }
    // 线性插值
    float ratio = (float) (velocity - MIN_VELOCITY) / (MAX_VELOCITY - MIN_VELOCITY);
    return (int) (MAX_SCROLL_DURATION - ratio * (MAX_SCROLL_DURATION - MIN_SCROLL_DURATION));
}
```

### 页面切换逻辑
1. 计算当前滑动偏移量与页面起始位置的差值
2. 如果差值超过页面高度的50%，切换页面
3. 如果滑动速度超过阈值且方向正确，也触发切换
4. 使用 `VideoOverScroller` 执行平滑动画到目标页面

## 运行指南

1. 通过 `./gradlew :wechatfriendfordouyin:assembleDebug` 生成APK
2. 安装后打开应用，自动播放第一个视频
3. 上下滑动切换不同视频
4. 观察滑动动画的流畅度和时间变化

## 视频资源

模块包含4个示例视频，位于 `res/raw/` 目录：
- `video1.mp4` - 格兰芬多骑行赛事
- `video2.mp4` - 户外风景
- `video3.mp4` - 汽车主题
- `video4.mp4` - 骑行日记

## 依赖说明

- **Media3 ExoPlayer**：用于视频播放
- **Glide**：用于图片加载
- **Material Design**：UI组件库

## 与主项目的关系

本模块是独立的抖音风格Demo，专注于研究：
- 垂直全屏视频切换的滑动体验
- 自定义 Scroller 的实现方式
- 视频播放器与滑动容器的配合

