# 编译指南 / Build Guide

## 🚀 快速开始

### 📦 下载预编译APK
可以直接从 `apk-released/` 目录下载最新的APK文件：
- `app-release-v1.0.0.apk` - 主应用
- `wechatfriendforperformance-release-v1.0.0.apk` - 性能测试版本
- `wechatfriendforpower-release-v1.0.0.apk` - 电量测试版本
- `wechatfriendforwebview-release-v1.0.0.apk` - WebView版本

### 🔨 从源码编译

#### 环境要求
- Android Studio Arctic Fox (2020.3.1) 或更高版本
- JDK 11 或更高版本
- Android SDK 34
- Gradle 8.x

#### 编译步骤

##### 1. Debug版本（无需签名配置）
```bash
# 克隆项目
git clone <repository-url>
cd HighPerformanceFriendsCircle

# 编译Debug版本
./gradlew assembleDebug

# APK位置
# app/build/outputs/apk/debug/app-debug.apk
# wechatfriendforperformance/build/outputs/apk/debug/wechatfriendforperformance-debug.apk
# wechatfriendforpower/build/outputs/apk/debug/wechatfriendforpower-debug.apk
# wechatfriendforwebview/build/outputs/apk/debug/wechatfriendforwebview-debug.apk
```

##### 2. Release版本（需要签名配置）

**方法1: 使用自己的签名**
```bash
# 1. 生成密钥库（如果没有）
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

# 2. 创建配置文件
cp keystore.properties.example keystore.properties

# 3. 编辑配置文件
# keystore_file=path/to/your/keystore
# keystore_password=your_password
# key_alias=your_alias
# key_password=your_key_password

# 4. 编译Release版本
./gradlew assembleRelease
```

**方法2: 使用构建脚本（推荐）**
```bash
# 配置keystore.properties后运行
./build_release.sh
```

## ⚠️ 重要说明

### 不影响编译的忽略文件
以下文件被Git忽略，但**不会影响**其他人编译：

✅ **安全忽略（必须的）**
- `keystore.properties` - 每个开发者需要自己配置
- `*.keystore`, `*.jks` - 签名密钥库，应该各自生成
- `local.properties` - 本地SDK路径，Android Studio会自动生成

✅ **构建产物（自动生成）**
- `/build`, `/*/build` - Gradle构建目录
- `*.log` - 日志文件
- `install_all_apks.sh` - 构建脚本自动生成

✅ **IDE配置（个人偏好）**
- `.idea/workspace.xml` - 个人工作区配置
- `.idea/gradle.xml` - Gradle配置（会自动重新生成）
- `.DS_Store` - macOS系统文件

✅ **其他开发工具文件**
- Lint报告、测试输出等

### 需要手动配置的文件

#### 1. `keystore.properties` (Release版本必需)
```properties
keystore_file=path/to/your/keystore
keystore_password=your_password
key_alias=your_alias
key_password=your_key_password
```

#### 2. `local.properties` (Android Studio自动生成)
```properties
sdk.dir=/path/to/Android/Sdk
```

## 🔧 故障排除

### 常见问题

#### 1. "SDK location not found"
**解决方案**: Android Studio会自动创建 `local.properties` 文件，或手动创建并指定SDK路径。

#### 2. "keystore.properties not found" (仅Release版本)
**解决方案**: 
- Debug版本：不需要此文件
- Release版本：复制 `keystore.properties.example` 并配置

#### 3. "compileSdk version mismatch"
**解决方案**: 确保安装了Android SDK 34。

### 验证编译环境
```bash
# 检查Java版本
java -version

# 检查Gradle版本
./gradlew --version

# 清理并测试编译
./gradlew clean
./gradlew assembleDebug
```

## 📱 安装APK

### 使用ADB
```bash
# 安装单个APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 批量安装（如果使用了构建脚本）
./install_all_apks.sh
```

### 直接安装
将APK文件传输到Android设备并直接安装。

## 🤝 贡献代码

1. Fork此项目
2. 创建功能分支
3. 提交更改（注意不要提交密钥库文件）
4. 推送到分支
5. 创建Pull Request

---

**💡 提示**: 如果遇到编译问题，请先尝试 `./gradlew clean`，然后重新编译。