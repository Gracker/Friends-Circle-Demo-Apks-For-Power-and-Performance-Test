#!/usr/bin/env python3
import os
import shutil

def copy_icons():
    """将生成的图标复制到Android项目的正确位置"""

    # 图标源目录
    source_dir = "game_icons"

    # Android项目目录
    android_dirs = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    # 游戏项目路径
    games = [
        {
            "name": "light",
            "package": "launch-game/src/main/res"
        },
        {
            "name": "medium",
            "package": "launch-game/src/main/res"
        },
        {
            "name": "heavy",
            "package": "launch-game/src/main/res"
        }
    ]

    for game in games:
        print(f"\nCopying icons for {game['name'].upper()} Load Game...")

        for dpi, size in android_dirs.items():
            # 创建目标目录
            target_dir = os.path.join(game["package"], dpi)
            os.makedirs(target_dir, exist_ok=True)

            # 复制主图标
            main_icon = os.path.join(source_dir, f"ic_launcher_{game['name']}_{size}x{size}.png")
            if os.path.exists(main_icon):
                target_file = os.path.join(target_dir, f"ic_launcher.png")
                shutil.copy2(main_icon, target_file)
                print(f"  Copied: ic_launcher.png -> {dpi}")

            # 复制round图标（适配器图标）
            round_icon = os.path.join(source_dir, f"ic_launcher_{game['name']}_round_{size}x{size}.png")
            if os.path.exists(round_icon):
                target_file = os.path.join(target_dir, f"ic_launcher_round.png")
                shutil.copy2(round_icon, target_file)
                print(f"  Copied: ic_launcher_round.png -> {dpi}")

if __name__ == "__main__":
    copy_icons()
    print("\nIcon copying completed!")
    print("\nNow update each AndroidManifest.xml to use the correct icon name")
    print("For example, use android:icon=\"@mipmap/ic_launcher\" in the manifest")