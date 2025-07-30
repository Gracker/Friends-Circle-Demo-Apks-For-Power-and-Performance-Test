# 🚀 自动发布设置指南

本文档介绍如何配置GitHub Actions实现自动APK发布功能。

## 📋 功能概述

### 1. 自动构建发布 (android.yml)
- **触发条件**: 每次push到master分支
- **功能**: 自动构建所有模块的debug和release版本，并创建GitHub Release
- **文件命名**: 包含时间戳和commit hash的APK文件
- **发布类型**: 自动发布，包含详细的构建信息

### 2. 手动签名发布 (release.yml)  
- **触发条件**: 手动触发（可在GitHub Actions页面执行）
- **功能**: 创建正式版本发布，支持自定义版本号和发布说明
- **签名支持**: 可配置签名密钥进行正式签名
- **发布类型**: 正式版本发布

## 🔐 配置签名密钥（可选）

如果你想让GitHub Actions自动签名你的APK，需要配置以下密钥：

### 步骤1: 准备签名文件
```bash
# 将你的keystore文件转换为base64
base64 -i your-release-key.keystore -o keystore.txt
```

### 步骤2: 在GitHub仓库中添加Secrets
进入你的GitHub仓库 → Settings → Secrets and variables → Actions，添加以下secrets：

- `KEYSTORE_BASE64`: keystore文件的base64编码内容
- `KEYSTORE_PASSWORD`: keystore的密码
- `KEY_ALIAS`: 签名密钥的别名
- `KEY_PASSWORD`: 签名密钥的密码

### 步骤3: 修改build.gradle添加签名配置

在需要签名的模块的`build.gradle`中添加：

```gradle
android {
    signingConfigs {
        release {
            if (project.hasProperty('KEYSTORE_PASSWORD')) {
                storeFile file('../release.keystore')
                storePassword System.getenv('KEYSTORE_PASSWORD')
                keyAlias System.getenv('KEY_ALIAS')
                keyPassword System.getenv('KEY_PASSWORD')
            }
        }
    }
    
    buildTypes {
        release {
            if (project.hasProperty('KEYSTORE_PASSWORD')) {
                signingConfig signingConfigs.release
            }
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 🎯 使用方法

### 自动发布（推荐用于开发版本）
1. 直接push代码到master分支
2. GitHub Actions会自动构建并创建release
3. 在仓库的Releases页面查看和下载APK

### 手动发布（推荐用于正式版本）
1. 进入仓库的Actions页面
2. 选择"Manual Release with Signing"工作流
3. 点击"Run workflow"
4. 输入版本号和发布说明
5. 点击运行，等待构建完成

## 📁 APK文件说明

### Debug版本
- **用途**: 开发测试使用
- **特点**: 可直接安装，包含调试信息
- **命名**: `[模块名]-debug-[时间戳]-[commit].apk`

### Release版本（无签名）
- **用途**: 发布前的最终版本
- **特点**: 优化过的生产版本，但未签名
- **命名**: `[模块名]-release-unsigned-[时间戳]-[commit].apk`

### Release版本（已签名）
- **用途**: 可以发布到应用商店的版本
- **特点**: 已签名，可直接发布
- **命名**: `[模块名]-[版本号].apk`

## 🔄 现有APK集成

如果你已经有签名的APK文件在`apk-released`文件夹中：

1. 这些文件会自动包含在手动发布的release中
2. 建议将现有APK按照新的命名规范重命名
3. 或者直接使用新的自动化流程替代手动管理

## 📝 最佳实践

### 开发阶段
- 使用自动构建发布查看最新的开发版本
- Debug版本用于日常测试

### 发布阶段  
- 配置签名密钥
- 使用手动发布创建正式版本
- 添加详细的发布说明

### 版本管理
- 使用语义化版本号（如v1.2.0）
- 在发布说明中记录重要变更
- 保持发布历史的清晰性

## 🚨 注意事项

1. **权限要求**: GitHub Actions需要有创建release的权限（默认已有）
2. **存储限制**: GitHub有存储限制，定期清理旧的artifacts
3. **签名安全**: 签名密钥信息妥善保管，不要泄露
4. **构建时间**: 完整构建可能需要几分钟，请耐心等待

## 🆘 常见问题

### Q: 为什么构建失败？
A: 检查Java版本、Gradle配置和依赖版本是否正确

### Q: 如何删除旧的release？
A: 在GitHub的Releases页面手动删除，或设置自动清理策略

### Q: APK为什么不能安装？
A: Debug版本应该可以直接安装，Release版本可能需要签名

### Q: 如何自定义APK名称？
A: 修改GitHub Actions工作流中的重命名脚本

---

🎉 配置完成后，你就拥有了一个完全自动化的Android应用发布流程！