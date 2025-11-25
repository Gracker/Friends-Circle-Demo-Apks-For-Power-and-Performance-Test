# Compose 朋友圈测试模块

本模块使用 **Jetpack Compose** 开发，实现与传统View版本相同的朋友圈界面和负载逻辑。

*Read this in [English](README_EN.md)*

## Compose vs 传统View

### Jetpack Compose（本模块）
- **声明式UI框架**：通过组合函数描述UI
- **Kotlin开发语言**：现代化的Kotlin语法
- **状态驱动渲染**：自动重组机制
- **无XML布局**：代码即UI

### 传统View（wechatfriendforperformance）
- **命令式UI框架**：通过XML和Java/Kotlin操作View
- **RecyclerView + Adapter模式**
- **手动状态管理**

## 技术实现

### 核心组件
- `LazyColumn`: 替代RecyclerView的列表组件
- `Coil`: Compose友好的图片加载库
- `Navigation Compose`: 导航组件
- `Material3`: Material Design 3组件库

### 负载模拟
保持与传统View版本相同的负载逻辑：
- 使用`Canvas`和`Paint`进行绘制操作
- 使用`Handler`进行定时任务调度
- 使用`@Volatile`防止编译器优化

## 使用场景

1. **框架对比测试**：对比Compose与传统View的渲染性能
2. **现代化开发体验**：体验声明式UI的开发模式
3. **功耗分析**：分析不同UI框架对电池的影响
4. **学习参考**：Compose性能测试应用的示例代码

## 功能特点

- **多层次负载测试**：与传统View版本相同的负载配置
- **UI完全一致**：尽可能还原微信朋友圈界面
- **负载逻辑相同**：使用相同的计算负载和数据生成逻辑

## 如何使用

1. 安装此APK到测试设备
2. 从主界面选择要测试的负载级别
3. 在滑动列表时观察UI性能表现
4. 使用Perfetto等工具收集性能数据
5. 对比与传统View版本的差异

## ADB命令启动

```bash
# 启动轻负载测试
adb shell am start -n com.example.wechatfriendforcompose/.MainActivity --es activity_type light

# 启动混合负载测试
adb shell am start -n com.example.wechatfriendforcompose/.MainActivity --es activity_type light_mixed
```

## 项目结构

```
wechatfriendforcompose/
├── src/main/java/com/example/wechatfriendforcompose/
│   ├── MainActivity.kt              # 主Activity
│   ├── ComposeApplication.kt        # Application类
│   ├── config/
│   │   └── LoadConfig.kt            # 负载配置
│   ├── data/
│   │   ├── Models.kt                # 数据模型
│   │   ├── LoadType.kt              # 负载类型枚举
│   │   ├── ComposeConstants.kt      # 常量
│   │   └── ComposeDataCenter.kt     # 数据中心
│   └── ui/
│       ├── theme/
│       │   └── Theme.kt             # 主题配置
│       ├── navigation/
│       │   └── Navigation.kt        # 导航配置
│       ├── screens/
│       │   ├── MainScreen.kt        # 主界面
│       │   └── FriendCircleScreen.kt # 朋友圈列表
│       └── components/
│           └── NineGridImages.kt    # 九宫格图片组件
└── build.gradle                      # 构建配置
```

## 与主项目的关系

这是[高性能微信朋友圈测试Demo](../README.md)的子模块，专注于Compose框架性能测试。


