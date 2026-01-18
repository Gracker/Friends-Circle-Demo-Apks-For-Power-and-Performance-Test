# 📱 自动化性能测试套件

一套完整的 Android 性能自动化测试方案，覆盖滑动测试、启动测试、Switch 跳转测试三大场景。

## 🚀 快速开始

### 1. 连接设备
确保 Android 设备已通过 USB 连接，并开启 USB 调试。

```bash
# 检查设备连接
adb devices
```

### 2. 一键执行测试

```bash
# 快速测试（推荐首次使用）
./scripts/run_all_tests.sh --quick

# 完整测试（所有应用、所有负载类型）
./scripts/run_all_tests.sh --full

# 跳过安装，只运行测试
./scripts/run_all_tests.sh --skip-install --quick
```

### 3. 查看报告
测试完成后，报告将自动生成在 `results/` 目录：
- `report_YYYYMMDD_HHMMSS.md` - Markdown 格式
- `report_YYYYMMDD_HHMMSS.html` - HTML 格式（可在浏览器打开）
- `report_YYYYMMDD_HHMMSS.json` - JSON 格式（便于程序处理）

---

## 📂 目录结构

```
automation-test/
├── config/
│   ├── apk_registry.json    # APK 注册表
│   └── test_config.json     # 测试配置
├── scripts/
│   ├── install_apks.sh      # APK 安装脚本
│   ├── run_scrolling_test.py# 滑动测试
│   ├── run_launch_test.py   # 启动测试
│   ├── run_switch_test.py   # Switch 测试
│   └── run_all_tests.sh     # 一键执行
├── lib/
│   ├── utils.py             # 通用工具库
│   └── report_generator.py  # 报告生成器
├── results/                  # 测试结果
└── README.md
```

---

## 🔧 分步执行

### 安装 APK

```bash
# 安装所有 APK
./scripts/install_apks.sh --all

# 只安装滑动测试 APK
./scripts/install_apks.sh --scrolling

# 快速模式（核心 APK）
./scripts/install_apks.sh --quick
```

### 滑动测试

```bash
# 使用默认配置运行
python3 scripts/run_scrolling_test.py

# 测试指定应用
python3 scripts/run_scrolling_test.py --apps aosp-performance compose

# 测试指定负载类型
python3 scripts/run_scrolling_test.py --load-types minimal medium heavy

# 设置滑动次数
python3 scripts/run_scrolling_test.py --swipe-count 30
```

### 启动测试

```bash
# 使用默认配置运行
python3 scripts/run_launch_test.py

# 测试指定应用
python3 scripts/run_launch_test.py --apps launch-aosp launch-compose

# 测试指定 flavor
python3 scripts/run_launch_test.py --flavors light heavy

# 设置迭代次数
python3 scripts/run_launch_test.py --iterations 10
```

### Switch 测试

```bash
# 使用默认配置运行
python3 scripts/run_switch_test.py

# 测试指定应用
python3 scripts/run_switch_test.py --apps switch-aosp

# 测试指定负载组合
python3 scripts/run_switch_test.py --combinations pure self_heavy heavy_heavy
```

---

## ⚙️ 配置说明

### test_config.json

```json
{
  "scrolling_test": {
    "swipe_count": 20,          // 滑动次数
    "test_apps": ["aosp-performance", "compose"],
    "test_load_types": ["minimal", "medium", "heavy"]
  },
  "launch_test": {
    "iterations": 5,            // 每个应用测试次数
    "clear_cache_before_test": false
  },
  "switch_test": {
    "iterations": 3
  },
  "thresholds": {
    "janky_percent": {
      "excellent": 1,
      "good": 5,
      "acceptable": 10
    }
  }
}
```

---

## 📊 评分标准

| 等级 | Janky% (滑动) | 启动时间 | Switch 时间 |
|------|---------------|----------|-------------|
| A+   | ≤1%          | ≤300ms   | ≤100ms      |
| A    | ≤5%          | ≤500ms   | ≤200ms      |
| B    | ≤10%         | ≤1000ms  | ≤500ms      |
| C    | ≤20%         | ≤2000ms  | ≤1000ms     |
| D    | >20%         | >2000ms  | >1000ms     |

---

## 🛠️ 常见问题

### Q: 测试过程中设备断开连接怎么办？
A: 重新连接设备，使用 `--skip-install` 选项继续测试。

### Q: 如何只重新生成报告？
A: 运行 `./scripts/run_all_tests.sh --report-only`

### Q: 测试结果不准确？
A: 确保测试时：
- 设备屏幕常亮
- 关闭其他后台应用
- 设备电量充足（>30%）
- 设备未处于省电模式
