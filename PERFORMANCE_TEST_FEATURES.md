# WeChat Friends Circle Performance Test App - 新增功能文档

## 概述
本次更新为性能测试应用增加了多种新的负载测试模式，采用固定的、有实际意义的计算任务，确保测试结果的可重复性和真实性。

## 新增活动类型

### 1. 最轻负载模式 (MinimalLoadActivity)
- **特点**: 完全没有额外的计算负载，作为最轻的基准测试用例
- **用途**: 提供最小开销的测试环境，用于对比其他负载模式的性能影响
- **实现**: 适配器设置 `LOAD_TYPE_MINIMAL = -1`，不执行任何额外计算

### 2. 帧间负载模式 - 有实际意义的计算任务
在两个 doFrame 调用之间执行固定的、有意义的计算负载，而不是基于时间的循环：

#### LightLoadBetweenFramesActivity - 轻负载
- **执行概率**: 30% 概率在帧间执行负载任务
- **任务内容**:
  - **数学计算**: 50次数学函数计算 (sin, cos, sqrt, log)
  - **矩阵运算**: 2x2矩阵乘法
  - **图像处理**: 绘制10个基本图形
  - **字符串处理**: 创建和处理20个字符串元素
- **防优化措施**: 使用 volatile 变量存储所有计算结果

#### MediumLoadBetweenFramesActivity - 中等负载  
- **执行概率**: 50% 概率在帧间执行负载任务
- **任务内容**:
  - **复杂数学**: 100次复杂数学函数计算 (sin, cos, pow, log, exp, atan2, sinh, cosh)
  - **矩阵运算**: 3x3矩阵乘法
  - **图像处理**: 绘制25个复杂图形 (圆形、矩形、线条)
  - **数据处理**: 100元素数组的快速排序和二分查找
  - **字符串处理**: 50个复杂字符串的创建、转换和分割

#### HeavyLoadBetweenFramesActivity - 高负载
- **执行概率**: 70% 概率在帧间执行负载任务
- **任务内容**:
  - **高级数学**: 200次复杂数学计算，包含贝塞尔函数和伽马函数近似
  - **大型矩阵**: 5x5矩阵乘法和行列式计算
  - **密集图像处理**: 50个复杂多层图形 (同心圆、椭圆、弧形)
  - **算法运算**: 200元素的归并排序、堆排序和多次搜索
  - **递归计算**: 第30个斐波那契数和15的阶乘
  - **高级字符串**: 100个元素的字符串处理和编码转换

**重要特性**：
- **确定性行为**: 使用固定种子的随机数生成器确保每次运行完全一致
- **有意义的计算**: 所有计算都是实际的算法和数学运算，不是空循环
- **防编译器优化**: 使用 volatile 变量存储结果并在后续逻辑中使用
- **真实负载**: 模拟真实应用中的数学运算、图像处理、数据排序等任务

### 3. 混合负载模式 - 三种级别的混合负载
同时包含 doFrame 期间的负载和帧间负载，提供更全面的性能测试场景：

#### LightMixedLoadActivity - 轻混合负载
- **doFrame 负载**: 使用确定性随机数(1-5)控制执行频率，执行轻负载任务 (200次数学运算)
- **帧间负载**: 30%概率执行轻负载帧间任务（与原帧间轻负载一致）
- **组合特点**: 帧内轻负载 + 帧间轻负载 = 混合轻负载

#### MediumMixedLoadActivity - 中混合负载  
- **doFrame 负载**: 使用确定性随机数(1-5)控制执行频率，执行中负载任务 (400次复杂数学运算)
- **帧间负载**: 50%概率执行增强中负载帧间任务（计算量翻倍）
- **组合特点**: 帧内中负载 + 增强帧间中负载 = 混合中负载

#### HeavyMixedLoadActivity - 高混合负载
- **doFrame 负载**: 使用确定性随机数(1-5)控制执行频率，执行高负载任务 (600次复杂数学运算)  
- **帧间负载**: 70%概率执行增强高负载帧间任务（计算量翻倍）
- **组合特点**: 帧内高负载 + 增强帧间高负载 = 混合高负载

## UI 界面更新

### 主界面布局
- 按功能分组显示不同的负载类型
- 使用不同颜色区分各种负载模式：
  - 绿色：最轻负载
  - 蓝色系：原始负载类型
  - 蓝绿色系：帧间负载类型  
  - 红色：混合负载

### 按钮说明
```
最轻负载 (Minimal)           - 启动最轻负载测试
轻负载 (帧内)                - 启动原始轻负载测试
中负载 (帧内)                - 启动原始中等负载测试  
高负载 (帧内)                - 启动原始高负载测试
轻负载 (帧间)               - 启动帧间轻负载测试
中等负载 (帧间)             - 启动帧间中等负载测试
高负载 (帧间)               - 启动帧间高负载测试
轻负载 (混合)               - 启动轻混合负载测试
中负载 (混合)               - 启动中混合负载测试
高负载 (混合)               - 启动高混合负载测试
```

## 命令行支持

### ADB 命令直接启动
支持通过 adb 命令直接启动不同的负载测试，方便 CI 自动化测试：

```bash
# 基本命令格式
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type <类型>

# 具体命令示例
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type minimal
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type light  
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type medium
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type heavy
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type light_between_frames
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type medium_between_frames  
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type heavy_between_frames
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type light_mixed
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type medium_mixed
adb shell am start -n com.example.wechatfriendforperformance/.PerformanceMainActivity --es activity_type heavy_mixed
```

### 便利脚本
提供 `launch_performance_test.sh` 脚本简化命令行操作：

```bash
# 使用脚本启动
./launch_performance_test.sh minimal         # 启动最轻负载测试
./launch_performance_test.sh heavy           # 启动高负载测试  
./launch_performance_test.sh light_mixed     # 启动轻混合负载测试
./launch_performance_test.sh medium_mixed    # 启动中混合负载测试
./launch_performance_test.sh heavy_mixed     # 启动高混合负载测试
./launch_performance_test.sh help            # 显示帮助信息
```

## CI 集成建议

### 测试脚本示例
```bash
#!/bin/bash
# 性能测试 CI 脚本示例

echo "开始性能测试..."

# 测试各种负载模式
test_cases=("minimal" "light" "medium" "heavy" "light_between_frames" "medium_between_frames" "heavy_between_frames" "light_mixed" "medium_mixed" "heavy_mixed")

for test_case in "${test_cases[@]}"; do
    echo "测试 $test_case 模式..."
    ./launch_performance_test.sh "$test_case"
    
    # 等待应用启动并运行一段时间
    sleep 30
    
    # 这里可以添加性能数据收集逻辑
    # 例如：收集帧率、CPU使用率、GPU使用率、功耗等数据
    
    # 停止应用
    adb shell am force-stop com.example.wechatfriendforperformance
    sleep 5
done

echo "性能测试完成"
```

## 技术实现细节

### 数据一致性保证
- 每次进入活动前都会调用 `PerformanceDataCenter.getInstance().clearCachedData()`
- 确保每次测试都使用相同的数据和 UI 配置
- 保证测试结果的可重复性和准确性

### 负载实现方式
- **doFrame 期间负载**: 使用 `Choreographer.FrameCallback` 在帧渲染期间执行计算
- **帧间负载**: 使用 `Handler.post()` 在 doFrame 完成后执行计算，采用确定性随机控制  
- **混合负载**: 结合两种方式，提供更复杂的测试场景

#### 帧间负载的改进实现
- **确定性随机**: 使用固定种子的 `Random` 对象确保每次运行的行为完全一致
- **概率控制**: 不同负载级别使用不同的执行概率（轻载30%、中载50%、重载70%）
- **有意义的任务**: 执行真实的算法和数学运算，包括：
  - 数学函数计算 (三角函数、指数、对数、双曲函数)
  - 矩阵运算 (乘法、行列式、LU分解)
  - 排序算法 (快速排序、归并排序、堆排序)
  - 搜索算法 (二分查找)
  - 递归计算 (斐波那契数、阶乘)
  - 图像处理 (几何图形绘制、多层效果)
  - 字符串处理 (构建、转换、分割、编码)
- **防编译器优化**: 所有计算结果存储在 volatile 变量中并在后续逻辑中使用

#### 性能监控特性
- 所有负载任务都使用 `Trace.beginSection()` 和 `Trace.endSection()` 标记
- 实时输出任务执行耗时日志，便于性能分析
- 支持通过 systrace 工具进行详细的性能追踪

### 资源管理
- 所有负载计算都使用 `Trace.beginSection()` 和 `Trace.endSection()` 包围
- 在 `onDestroy()` 中正确释放 Bitmap 和其他资源
- 在 `onPause()` 中暂停负载计算，在 `onResume()` 中恢复

## 测试建议

### 测试场景
1. **基准测试**: 使用最轻负载模式建立基准数据
2. **渐进测试**: 从轻到重依次测试各种负载模式
3. **对比测试**: 对比 doFrame 负载和帧间负载的性能差异
4. **极限测试**: 使用混合负载模式测试系统极限性能

### 监控指标
- 帧率 (FPS)
- CPU 使用率
- GPU 使用率  
- 内存使用量
- 功耗数据
- 电池温度

通过这些新增功能，您可以更全面地测试和分析 Android 设备的性能表现。