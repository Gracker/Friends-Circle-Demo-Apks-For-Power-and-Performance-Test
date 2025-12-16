#!/usr/bin/env python3
import os
import shutil

def setup_variant_icons():
    """为不同的构建变体设置不同的图标"""

    base_res_dir = "launch-game/src/main/res"
    variants = ["light", "medium", "heavy"]

    # 颜色配置
    colors = {
        "light": {"name": "Light", "color": (76, 175, 80)},      # 绿色
        "medium": {"name": "Medium", "color": (255, 152, 0)},   # 橙色
        "heavy": {"name": "Heavy", "color": (244, 67, 54)}       # 红色
    }

    # 为每个变体创建单独的图标
    for variant in variants:
        print(f"\nSetting up icons for {colors[variant]['name']} Load Game...")

        # 创建变体特定的资源目录
        variant_res_dir = f"launch-game/src/{variant}/res"
        os.makedirs(variant_res_dir, exist_ok=True)

        # mipmap目录
        dpi_dirs = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi",
                    "mipmap-xxhdpi", "mipmap-xxxhdpi"]

        for dpi in dpi_dirs:
            dpi_path = os.path.join(variant_res_dir, dpi)
            os.makedirs(dpi_path, exist_ok=True)

            # 复制对应的图标
            icon_name = f"ic_launcher_{variant}"
            round_icon_name = f"ic_launcher_{variant}_round"

            # 从game_icons目录复制
            for file in os.listdir("game_icons"):
                if file.startswith(icon_name) and file.endswith(".png"):
                    if "round" in file and round_icon_name in file:
                        target = "ic_launcher_round.png"
                    elif not "round" in file and icon_name in file:
                        target = "ic_launcher.png"
                    else:
                        continue

                    src = os.path.join("game_icons", file)
                    dst = os.path.join(dpi_path, target)
                    shutil.copy2(src, dst)
                    print(f"  {dpi}/{target}")

    # 创建一个README说明如何使用
    readme = """# Game Icons

不同负载版本使用不同的图标颜色：
- **Light Load (轻负载)**: 绿色主题 - 对应 app-debug.apk
- **Medium Load (中负载)**: 橙色主题 - 对应 app-release.apk
- **Heavy Load (重负载)**: 红色主题 - 对应 app-release.apk

图标已放置在各自的构建变体目录中：
- launch-game/src/light/res/
- launch-game/src/medium/res/
- launch-game/src/heavy/res/

构建时会自动使用对应的图标。
"""

    with open("launch-game/ICON_README.md", "w") as f:
        f.write(readme)

    print("\n✓ Icon setup completed!")
    print("\nEach build variant now has its own icons:")
    for variant in variants:
        print(f"  - {colors[variant]['Name']} (/{variant}): {colors[variant]['color']}")

if __name__ == "__main__":
    setup_variant_icons()