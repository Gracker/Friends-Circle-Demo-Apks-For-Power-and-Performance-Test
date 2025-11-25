# LoadConfig 统一负载配置中心

## 概述

`LoadConfig` 是性能测试模块的统一配置中心，集中管理所有Activity的负载参数，提供科学的负载分级和配置验证功能。

## 设计原理

### 负载分级策略

1. **doFrame负载** - 几何级数增长
   - 轻负载：1000 
   - 中负载：2000 (2x)
   - 高负载：4000 (2x)

2. **帧间负载** - 反比例平衡设计
   - 轻负载：2400
   - 中负载：1600 (↓33%)
   - 高负载：1067 (↓33%)

3. **总负载趋势** - 线性递增
   - 轻负载总和：3400
   - 中负载总和：3600 (↑6%)
   - 高负载总和：5067 (↑41%)

### 科学依据

- **Weber-Fechner定律**：人眼感知呈对数关系，doFrame采用几何级数
- **负载平衡理论**：帧间负载反比例衰减，平衡整体性能
- **Android渲染管线**：基于60fps (16.67ms/帧) 设计任务间隔

## 配置类结构

```java
LoadConfig
├── 任务调度配置
│   ├── MIN_TASK_INTERVAL_MS = 16ms  (1帧)
│   ├── MAX_TASK_INTERVAL_MS = 83ms  (5帧)
│   └── 随机种子配置 (确保可重现性)
├── 单一负载配置
│   ├── LightLoad.TASK_INTENSITY = 150
│   ├── MediumLoad.TASK_INTENSITY = 300  
│   └── HeavyLoad.TASK_INTENSITY = 500
├── 混合负载配置
│   ├── LightMixedLoad   (doFrame:1000, 帧间:2400)
│   ├── MediumMixedLoad  (doFrame:2000, 帧间:1600)
│   └── HeavyMixedLoad   (doFrame:4000, 帧间:1067)
└── 帧间负载配置
    ├── LightLoadBetweenFrames = 120
    ├── MediumLoadBetweenFrames = 400
    └── HeavyLoadBetweenFrames = 800
```

## 使用方法

### 1. 在Activity中引用配置

```java
// 导入配置
import com.example.wechatfriendforperformance.config.LoadConfig;

// 使用混合负载配置
private static final int DOFRAME_INTENSITY = LoadConfig.LightMixedLoad.DOFRAME_TASK_INTENSITY;
private static final int BETWEEN_FRAME_INTENSITY = LoadConfig.LightMixedLoad.BETWEEN_FRAME_TASK_INTENSITY;

// 使用任务间隔配置
private static final int MIN_INTERVAL = LoadConfig.MIN_TASK_INTERVAL_MS;
private static final int MAX_INTERVAL = LoadConfig.MAX_TASK_INTERVAL_MS;

// 使用随机种子
private Random mTaskRandom = new Random(LoadConfig.TASK_INTERVAL_SEED);
```

### 2. 获取配置描述

```java
// 获取负载配置说明
String description = LoadConfig.getLoadConfigDescription("LightMixedLoad");
Log.d(TAG, description); // 输出: "轻负载混合 - doFrame:1000, 帧间:2400"
```

### 3. 验证配置科学性

```java
// 验证配置是否符合科学设计
boolean isValid = LoadConfig.validateLoadConfig();
if (!isValid) {
    Log.e(TAG, "负载配置不符合科学设计原则！");
}
```

## 配置验证

系统会在应用启动时自动验证配置的科学性：

- ✅ **doFrame负载几何增长**：确保 Medium = Light × 2, Heavy = Medium × 2
- ✅ **总负载线性递增**：确保 Light < Medium < Heavy
- ✅ **任务间隔合理性**：1-5帧间隔符合Android渲染周期

## 修改配置

### 调试时临时调整

如需临时调整负载参数进行测试，只需修改 `LoadConfig.java` 中的对应常量：

```java
public static class HeavyMixedLoad {
    public static final int DOFRAME_TASK_INTENSITY = 5000;        // 从4000调整到5000
    public static final int BETWEEN_FRAME_TASK_INTENSITY = 800;   // 从1067调整到800
}
```

### 添加新的负载级别

```java
public static class ExtremeLoad {
    public static final int DOFRAME_TASK_INTENSITY = 8000;
    public static final int BETWEEN_FRAME_TASK_INTENSITY = 500;
}
```

## 优势

1. **集中管理**：所有负载配置统一管理，避免分散硬编码
2. **科学设计**：基于理论设计负载分级，确保测试有效性
3. **易于调试**：一处修改，全局生效，方便性能调优
4. **配置验证**：自动验证配置合理性，防止错误配置
5. **可扩展性**：易于添加新的负载级别和配置项

## 注意事项

- 修改配置后建议运行验证确保科学性
- 配置变更需要重新编译和测试
- 保持配置的相对关系，避免破坏负载分级的科学性













