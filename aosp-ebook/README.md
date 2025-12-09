# 电子书阅读器 Demo

这是一个模拟电子书阅读器的 Demo 应用，提供了基本的 EPUB 电子书阅读功能。

## 功能特性

### 核心功能
- **EPUB 解析**：支持标准 EPUB 格式电子书文件
- **智能分页**：根据屏幕尺寸自动分页显示内容
- **首行缩进**：中文阅读习惯的两字符缩进

### 交互方式
- **点击左侧（1/3）**：翻到上一页
- **点击右侧（1/3）**：翻到下一页
- **点击中间（1/3）**：显示/隐藏阅读菜单
- **左右滑动**：翻页（带平滑动画）

### 阅读菜单（模拟）
顶部菜单：
- 返回按钮
- 书名显示
- 书签按钮

底部菜单：
- 阅读进度条
- 目录
- 亮度调节
- 字体大小
- 主题切换
- 设置

> 注意：菜单功能为模拟实现，点击后仅显示 Toast 提示

### 视觉效果
- 暖黄色护眼背景（#FFF8E1）
- 全屏沉浸式阅读
- 翻页动画效果
- 渐变菜单背景

## 技术实现

### 架构
```
aosp-ebook/
├── src/main/
│   ├── assets/
│   │   └── default_book.epub    # 默认电子书
│   ├── java/.../
│   │   ├── EBookReaderActivity.java  # 主界面
│   │   ├── parser/
│   │   │   ├── EpubParser.java       # EPUB 解析器
│   │   │   └── PageSplitter.java     # 页面分割器
│   │   └── view/
│   │       └── PageView.java         # 自定义阅读视图
│   └── res/
│       ├── layout/
│       ├── drawable/
│       └── values/
```

### 依赖
- JSoup：用于解析 EPUB 中的 HTML/XHTML 内容
- AndroidX Lifecycle：生命周期管理

### EPUB 解析流程
1. 解压 EPUB 文件（ZIP 格式）
2. 解析 `META-INF/container.xml` 获取 OPF 文件路径
3. 解析 OPF 文件获取书籍元数据和章节列表
4. 按 spine 顺序加载所有章节的 HTML 内容
5. 提取纯文本并按屏幕尺寸分页

## 默认电子书

应用默认加载 《巨婴国》（武志红 著），电子书文件位于 `assets/default_book.epub`。

## 使用方式

1. 安装 APK 后启动应用
2. 应用会自动加载默认电子书
3. 使用点击或滑动进行翻页
4. 点击屏幕中间可显示阅读菜单

## 构建

```bash
# 构建 Debug 版本
./gradlew :aosp-ebook:assembleDebug

# APK 输出路径
aosp-ebook/build/outputs/apk/debug/aosp-ebook-debug.apk
```

## 注意事项

- 本 Demo 主要用于演示电子书阅读界面和翻页交互
- 不包含各种负载测试功能
- 菜单功能为模拟实现，不具备实际操作功能

