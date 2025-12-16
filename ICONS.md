# Game Icons

已为三个性能测试游戏版本生成专属图标：

## 图标设计

每个图标都包含以下元素：
- **游戏手柄图标**：白色半透明风格
- **主题色彩**：
  - **Light Load (轻负载)**：绿色主题 (#4CAF50)
  - **Medium Load (中负载)**：橙色主题 (#FF9800)
  - **Heavy Load (重负载)**：红色主题 (#F44336)
- **文字标识**：LIGHT/MEDIUM/HEAVY + Performance Test
- **视觉效果**：渐变背景 + 发光边缘

## 文件结构

```
launch-game/
├── src/
│   ├── light/res/mipmap-*/      # 轻负载版本图标（绿色）
│   │   ├── ic_launcher.png
│   │   └── ic_launcher_round.png
│   ├── medium/res/mipmap-*/     # 中负载版本图标（橙色）
│   │   ├── ic_launcher.png
│   │   └── ic_launcher_round.png
│   └── heavy/res/mipmap-*/      # 重负载版本图标（红色）
│       ├── ic_launcher.png
│       └── ic_launcher_round.png
└── src/main/res/                 # 主目录（已清理）
```

## 支持的分辨率

所有图标都支持 Android 标准分辨率：
- mipmap-mdpi (48x48)
- mipmap-hdpi (72x72)
- mipmap-xhdpi (96x96)
- mipmap-xxhdpi (144x144)
- mipmap-xxxhdpi (192x192)

## APK 对应关系

- **轻负载版本** (`app-debug.apk`): 使用绿色图标
- **中负载版本** (`app-release.apk`): 使用橙色图标
- **重负载版本** (`app-release.apk`): 使用红色图标

## 图标特点

- **现代化设计**：简洁的游戏手柄图案
- **易于识别**：清晰的颜色区分
- **专业外观**：渐变背景和发光效果
- **适配性**：支持所有标准 Android 屏幕密度
- **圆形图标**：支持 Android 8.0+ 的自适应图标

构建时会根据构建变体自动选择对应的图标资源。