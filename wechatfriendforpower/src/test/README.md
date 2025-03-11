# 朋友圈功耗测试模块单元测试

本目录包含了针对`wechatfriendforpower`模块的单元测试，主要测试功耗监控和朋友圈UI展示等核心功能。

## 测试内容

测试套件包含以下核心测试类：

1. **PowerTestUtil** - 测试工具类，用于生成测试数据和模拟对象
2. **BeanTest** - 数据模型测试（UserBean、PraiseBean、CommentBean等）
3. **PowerUtilsTest** - 工具类测试
4. **PowerSpanUtilsTest** - 文本样式工具类测试
5. **PowerDataGeneratorTest** - 数据生成器测试
6. **PowerMonitorUtilsTest** - 电量监控工具类测试
7. **PowerAdapterTest** - 适配器测试
8. **PowerActivityTest** - 主Activity测试

## 运行测试

### 通过Android Studio运行

1. 打开Android Studio中的项目
2. 右键点击测试类或测试方法
3. 选择"Run"运行测试

### 通过命令行运行

执行以下命令运行所有测试：

```
./gradlew :wechatfriendforpower:testDebugUnitTest
```

运行特定测试类：

```
./gradlew :wechatfriendforpower:testDebugUnitTest --tests "com.android.wechatfriendforpower.PowerActivityTest"
```

## 测试覆盖范围

测试主要覆盖以下功能点：

1. **数据模型** - 验证Bean类的基本功能
2. **适配器功能** - 测试RecyclerView适配器的数据展示和交互
3. **电池监控** - 测试电量监控和功耗计算功能
4. **UI组件** - 测试主活动页的UI交互
5. **工具类** - 测试各种辅助工具类的功能

## 测试依赖

测试使用以下主要工具：

- JUnit 4 - 测试框架
- Mockito - 模拟对象框架
- Robolectric - Android环境模拟框架

## 注意事项

1. 部分UI测试需要使用Robolectric运行，这些测试会模拟Android环境
2. 电池相关功能的测试主要是接口和计算逻辑测试，真实的电池监控需要在实机上进行
3. 磁盘IO操作（如日志写入）在测试中被模拟，实际功能验证需要集成测试 