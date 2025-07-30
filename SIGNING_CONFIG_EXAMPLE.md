# 🔐 签名配置示例

如果你想让GitHub Actions自动签名你的APK，可以参考以下配置：

## app模块签名配置示例

在`app/build.gradle`中添加：

```gradle
android {
    signingConfigs {
        release {
            if (System.getenv('KEYSTORE_PASSWORD') != null) {
                storeFile file('../release.keystore')
                storePassword System.getenv('KEYSTORE_PASSWORD')
                keyAlias System.getenv('KEY_ALIAS')
                keyPassword System.getenv('KEY_PASSWORD')
                v1SigningEnabled true
                v2SigningEnabled true
            }
        }
    }
    
    buildTypes {
        release {
            if (System.getenv('KEYSTORE_PASSWORD') != null) {
                signingConfig signingConfigs.release
            }
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 其他模块配置

对于`wechatfriendforperformance`、`wechatfriendforpower`、`wechatfriendforwebview`模块，可以添加类似的配置。

## 本地测试签名

如果你想在本地测试签名配置：

```bash
# 设置环境变量
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

# 构建签名版本
./gradlew assembleRelease
```

## 生成签名密钥

如果你还没有签名密钥，可以生成一个：

```bash
keytool -genkey -v -keystore release.keystore -alias your_key_alias -keyalg RSA -keysize 2048 -validity 10000
```

记住保存好以下信息：
- keystore密码
- key别名（alias）
- key密码

这些信息需要添加到GitHub Secrets中。