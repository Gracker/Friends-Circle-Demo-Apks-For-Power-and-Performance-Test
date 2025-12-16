#!/usr/bin/env python3
import os
import shutil
import glob

def clean_duplicate_icons():
    """清理重复的图标并正确设置变体图标"""

    print("Cleaning duplicate icons...")

    # 删除 main/res 中的图标（避免冲突）
    main_res_dirs = glob.glob("launch-game/src/main/res/mipmap-*")
    for dir_path in main_res_dirs:
        if os.path.exists(dir_path):
            for file in os.listdir(dir_path):
                if file.startswith("ic_launcher"):
                    file_path = os.path.join(dir_path, file)
                    os.remove(file_path)
                    print(f"  Removed: {file_path}")

    # 重新正确设置图标
    variants = {
        "light": {"name": "Light", "color": "green"},
        "medium": {"name": "Medium", "color": "orange"},
        "heavy": {"name": "Heavy", "color": "red"}
    }

    source_dir = "game_icons"

    for variant, info in variants.items():
        print(f"\nSetting up {info['name']} icons ({info['color']})...")

        # 清理旧图标
        for dir_path in glob.glob(f"launch-game/src/{variant}/res/mipmap-*"):
            for file in os.listdir(dir_path):
                if file.startswith("ic_launcher"):
                    os.remove(os.path.join(dir_path, file))

        # 复制正确的新图标
        for dir_path in glob.glob(f"launch-game/src/{variant}/res/mipmap-*"):
            dpi = os.path.basename(dir_path)

            # 确定尺寸
            size_map = {
                "mipmap-mdpi": 48,
                "mipmap-hdpi": 72,
                "mipmap-xhdpi": 96,
                "mipmap-xxhdpi": 144,
                "mipmap-xxxhdpi": 192
            }

            if dpi in size_map:
                size = size_map[dpi]

                # 主图标
                src = os.path.join(source_dir, f"ic_launcher_{variant}_{size}x{size}.png")
                dst = os.path.join(dir_path, "ic_launcher.png")
                if os.path.exists(src):
                    shutil.copy2(src, dst)
                    print(f"  ✓ ic_launcher.png -> {dpi}")

                # Round图标
                src_round = os.path.join(source_dir, f"ic_launcher_{variant}_round_{size}x{size}.png")
                dst_round = os.path.join(dir_path, "ic_launcher_round.png")
                if os.path.exists(src_round):
                    shutil.copy2(src_round, dst_round)
                    print(f"  ✓ ic_launcher_round.png -> {dpi}")

if __name__ == "__main__":
    clean_duplicate_icons()
    print("\n✓ Icon setup completed successfully!")