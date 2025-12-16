#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFont
import math

def create_gradient_background(width, height, color1, color2):
    """创建渐变背景"""
    image = Image.new('RGB', (width, height))
    draw = ImageDraw.Draw(image)

    for y in range(height):
        ratio = y / height
        r = int(color1[0] * (1 - ratio) + color2[0] * ratio)
        g = int(color1[1] * (1 - ratio) + color2[1] * ratio)
        b = int(color1[2] * (1 - ratio) + color2[2] * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    return image

def create_game_icon(title, subtitle, main_color, output_path, size=512):
    """创建游戏图标"""
    # 创建画布
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # 渐变背景
    color1 = tuple(int(c * 0.3) for c in main_color)
    color2 = tuple(int(c * 0.8) for c in main_color)
    bg = create_gradient_background(size, size, color1, color2)
    image.paste(bg)

    # 添加圆形遮罩
    margin = 20
    draw.ellipse([margin, margin, size - margin, size - margin],
                 fill=tuple(int(c * 0.5) for c in main_color))

    # 绘制游戏手柄图标
    center = size // 2
    controller_size = size // 3

    # 手柄主体
    controller_color = (255, 255, 255, 200)
    # 左摇杆
    draw.ellipse([center - controller_size//2 - 20, center - 10,
                  center - controller_size//2 + 20, center + 10],
                  fill=controller_color)
    # 右按钮组
    btn_size = 15
    draw.ellipse([center + controller_size//2 - 20, center - 25,
                  center + controller_size//2 - 5, center - 5],
                  fill=controller_color)  # Y按钮
    draw.ellipse([center + controller_size//2 + 5, center - 10,
                  center + controller_size//2 + 25, center + 10],
                  fill=controller_color)  # B按钮
    draw.ellipse([center + controller_size//2 - 20, center + 15,
                  center + controller_size//2 - 5, center + 35],
                  fill=controller_color)  # A按钮
    draw.ellipse([center + controller_size//2 - 45, center - 10,
                  center + controller_size//2 - 25, center + 10],
                  fill=controller_color)  # X按钮

    # 添加标题文字
    try:
        # 尝试使用更大的字体
        font_title = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", size // 6)
        font_subtitle = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", size // 10)
    except:
        # 如果系统字体不可用，使用默认字体
        font_title = ImageFont.load_default()
        font_subtitle = ImageFont.load_default()

    # 绘制文字
    draw.text((center, size * 0.65), title, fill="white",
              font=font_title, anchor="mm")
    draw.text((center, size * 0.78), subtitle, fill=(255, 255, 255, 200),
              font=font_subtitle, anchor="mm")

    # 添加发光效果
    for i in range(3):
        overlay = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        overlay_draw = ImageDraw.Draw(overlay)
        alpha = 30 - i * 10
        overlay_draw.ellipse([margin - i*5, margin - i*5,
                              size - margin + i*5, size - margin + i*5],
                             outline=(*main_color, alpha), width=3)
        image = Image.alpha_composite(image, overlay)

    return image

def resize_icon(image, sizes, output_dir, name):
    """生成不同尺寸的图标"""
    for size in sizes:
        resized = image.resize((size, size), Image.Resampling.LANCZOS)
        # 保存为PNG格式
        output_file = os.path.join(output_dir, f"{name}_{size}x{size}.png")
        resized.save(output_file, "PNG")
        print(f"Generated: {output_file}")

def main():
    # 输出目录
    output_dir = "game_icons"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # 需要生成的尺寸
    sizes = [72, 96, 128, 144, 152, 192, 256, 384, 512]

    # Light Load Game - 绿色主题
    print("Generating Light Load Game icons...")
    light_icon = create_game_icon(
        "LIGHT",
        "Performance Test",
        (76, 175, 80),  # 绿色
        os.path.join(output_dir, "light_icon_temp.png")
    )
    resize_icon(light_icon, sizes, output_dir, "ic_launcher_light")

    # Medium Load Game - 橙色主题
    print("Generating Medium Load Game icons...")
    medium_icon = create_game_icon(
        "MEDIUM",
        "Performance Test",
        (255, 152, 0),  # 橙色
        os.path.join(output_dir, "medium_icon_temp.png")
    )
    resize_icon(medium_icon, sizes, output_dir, "ic_launcher_medium")

    # Heavy Load Game - 红色主题
    print("Generating Heavy Load Game icons...")
    heavy_icon = create_game_icon(
        "HEAVY",
        "Performance Test",
        (244, 67, 54),  # 红色
        os.path.join(output_dir, "heavy_icon_temp.png")
    )
    resize_icon(heavy_icon, sizes, output_dir, "ic_launcher_heavy")

    # 创建launcher icon (适配器图标)
    print("\nGenerating launcher icons...")
    launcher_sizes = [48, 72, 96, 144, 192]
    launcher_bg = Image.new('RGB', (192, 192), (240, 240, 240))

    for game_name, color, prefix in [
        ("Light", (76, 175, 80), "light"),
        ("Medium", (255, 152, 0), "medium"),
        ("Heavy", (244, 67, 54), "heavy")
    ]:
        # 在白色背景上绘制游戏logo
        draw = ImageDraw.Draw(launcher_bg)
        center = 96
        # 绘制圆形背景
        draw.ellipse([24, 24, 168, 168], fill=color)
        # 绘制文字
        try:
            font = ImageFont.truetype("/System/Library/Fonts/Arial.ttf", 48)
        except:
            font = ImageFont.load_default()
        draw.text((center, center), game_name[0], fill="white",
                  font=font, anchor="mm")

        # 保存launcher图标
        for size in launcher_sizes:
            if size == 192:
                launcher_resized = launcher_bg
            else:
                launcher_resized = launcher_bg.resize((size, size), Image.Resampling.LANCZOS)
            launcher_resized.save(
                os.path.join(output_dir, f"ic_launcher_{prefix}_round_{size}x{size}.png"),
                "PNG"
            )

    print("\nIcon generation completed!")
    print(f"Icons saved to: {os.path.abspath(output_dir)}")
    print("\nAdd these to your app's res/mipmap-* directories:")
    print("- ic_launcher_light.png (and other sizes)")
    print("- ic_launcher_medium.png (and other sizes)")
    print("- ic_launcher_heavy.png (and other sizes)")

if __name__ == "__main__":
    main()