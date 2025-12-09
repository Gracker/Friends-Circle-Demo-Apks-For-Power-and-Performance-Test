#!/usr/bin/env python3
"""
图标生成脚本
根据模块类型生成带有文字标签和不同颜色的应用图标
"""

import os
from PIL import Image, ImageDraw, ImageFont
import math

# 颜色方案（根据实现类型）
COLORS = {
    'aosp': '#2196F3',      # 蓝色 - AOSP模块（包括Compose）
    'webview': '#4CAF50',   # 绿色 - WebView模块
    'map': '#FF9800',       # 橙色 - Map模块（Surface Map + GL Map）
}

# 类型显示名称
TYPE_LABELS = {
    'aosp': 'AOSP',
    'webview': 'WebView',
    'map': 'Map',
}

# 模块配置: (模块目录, 类型, 显示文字列表)
MODULES = [
    ('app', 'aosp', ['App']),
    ('aosp-performance', 'aosp', ['Perf']),
    ('aosp-power', 'aosp', ['Power']),
    ('aosp-picasso', 'aosp', ['Picasso']),
    ('aosp-customscroller', 'aosp', ['Custom', 'Scroller']),
    ('aosp-renderstress', 'aosp', ['Render', 'Stress']),
    ('aosp-softwarerender', 'aosp', ['Software', 'Render']),
    ('aosp-douyin', 'aosp', ['Douyin']),
    ('aosp-video', 'aosp', ['Video']),
    ('aosp-ebook', 'aosp', ['Ebook']),
    ('aosp-purerenderthread', 'aosp', ['Pure', 'Render']),
    ('aosp-dualwindow', 'aosp', ['Dual', 'Window']),
    ('aosp-mixedrender', 'aosp', ['Mixed', 'Render']),
    ('compose', 'aosp', ['Compose']),
    ('webview', 'webview', ['Functor']),
    ('webview-surface', 'webview', ['Surface']),
    ('webview-texture', 'webview', ['Texture']),
    ('webview-imagereader', 'webview', ['ImgReader']),
    ('surface-map', 'map', ['Surface']),
    ('gl-map', 'map', ['GL']),
]

# 图标尺寸配置
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

def hex_to_rgb(hex_color):
    """将十六进制颜色转换为RGB元组"""
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

def get_luminance(rgb):
    """计算颜色亮度"""
    r, g, b = rgb
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255

def get_text_color(bg_rgb):
    """根据背景色选择合适的文字颜色"""
    luminance = get_luminance(bg_rgb)
    if luminance > 0.5:
        return (33, 33, 33)  # 深灰色文字
    else:
        return (255, 255, 255)  # 白色文字

def get_font(font_size):
    """获取字体"""
    font_paths = [
        '/System/Library/Fonts/Helvetica.ttc',
        '/System/Library/Fonts/SFNSDisplay.ttf',
        '/System/Library/Fonts/SFCompact.ttf',
        '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
        '/usr/share/fonts/TTF/DejaVuSans-Bold.ttf',
    ]
    for fp in font_paths:
        if os.path.exists(fp):
            try:
                return ImageFont.truetype(fp, font_size)
            except:
                continue
    return ImageFont.load_default()

def calculate_optimal_font_size(draw, text_lines, max_width, max_height, initial_size):
    """动态计算最优字体大小，确保文字完全显示"""
    font_size = initial_size
    min_font_size = 8
    
    while font_size >= min_font_size:
        font = get_font(font_size)
        
        # 计算所有行的尺寸
        total_height = 0
        max_text_width = 0
        line_spacing = font_size * 0.2
        
        for text in text_lines:
            bbox = draw.textbbox((0, 0), text, font=font)
            text_width = bbox[2] - bbox[0]
            text_height = bbox[3] - bbox[1]
            max_text_width = max(max_text_width, text_width)
            total_height += text_height
        
        total_height += line_spacing * (len(text_lines) - 1)
        
        # 检查是否适合
        if max_text_width <= max_width and total_height <= max_height:
            return font_size, font
        
        font_size -= 1
    
    return min_font_size, get_font(min_font_size)

def create_icon(size, color_hex, text_lines, output_path, type_label):
    """创建方形图标（无边框，顶部显示类型标签）"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    bg_color = hex_to_rgb(color_hex)
    text_color = get_text_color(bg_color)
    
    # 绘制圆角矩形背景（无边距，填满整个图标）
    radius = size * 0.18
    draw.rounded_rectangle(
        [0, 0, size, size],
        radius=radius,
        fill=bg_color
    )
    
    # 计算可用文字区域
    padding = size * 0.06
    available_width = size - padding * 2
    
    # 顶部类型标签（增大字体）
    type_font_size = max(10, int(size * 0.18))
    type_font = get_font(type_font_size)
    type_bbox = draw.textbbox((0, 0), type_label, font=type_font)
    type_width = type_bbox[2] - type_bbox[0]
    type_height = type_bbox[3] - type_bbox[1]
    
    # 主文字区域（类型标签下方）
    top_margin = size * 0.12
    type_y = top_margin
    main_area_top = type_y + type_height + size * 0.04
    main_area_height = size - main_area_top - padding
    
    # 动态计算主文字字体大小（增大字体）
    initial_font_size = int(size * 0.38) if len(text_lines) == 1 else int(size * 0.26)
    font_size, font = calculate_optimal_font_size(
        draw, text_lines, available_width, main_area_height, initial_font_size
    )
    
    # 绘制类型标签（顶部居中）
    type_x = (size - type_width) / 2
    # 类型标签使用半透明效果
    type_color = (*text_color, 200)  # 稍微透明
    draw.text((type_x, type_y), type_label, fill=text_color, font=type_font)
    
    # 计算主文字位置
    line_heights = []
    line_widths = []
    for text in text_lines:
        bbox = draw.textbbox((0, 0), text, font=font)
        line_widths.append(bbox[2] - bbox[0])
        line_heights.append(bbox[3] - bbox[1])
    
    line_spacing = font_size * 0.12
    total_height = sum(line_heights) + line_spacing * (len(text_lines) - 1)
    
    # 绘制主文字（在类型标签下方居中）
    y_offset = main_area_top + (main_area_height - total_height) / 2
    for i, (text, width, height) in enumerate(zip(text_lines, line_widths, line_heights)):
        x = (size - width) / 2
        draw.text((x, y_offset), text, fill=text_color, font=font)
        y_offset += height + line_spacing
    
    img.save(output_path, 'PNG')

def create_round_icon(size, color_hex, text_lines, output_path, type_label):
    """创建圆形图标（无边框，顶部显示类型标签）"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    bg_color = hex_to_rgb(color_hex)
    text_color = get_text_color(bg_color)
    
    # 绘制圆形背景（填满整个图标）
    draw.ellipse([0, 0, size, size], fill=bg_color)
    
    # 计算可用文字区域（圆形内）
    inner_size = size * 0.70
    
    # 顶部类型标签（增大字体）
    type_font_size = max(10, int(size * 0.16))
    type_font = get_font(type_font_size)
    type_bbox = draw.textbbox((0, 0), type_label, font=type_font)
    type_width = type_bbox[2] - type_bbox[0]
    type_height = type_bbox[3] - type_bbox[1]
    
    # 类型标签位置
    top_margin = size * 0.18
    type_y = top_margin
    main_area_top = type_y + type_height + size * 0.03
    main_area_height = size * 0.82 - main_area_top
    
    # 动态计算主文字字体大小（增大字体）
    initial_font_size = int(size * 0.32) if len(text_lines) == 1 else int(size * 0.22)
    font_size, font = calculate_optimal_font_size(
        draw, text_lines, inner_size, main_area_height, initial_font_size
    )
    
    # 绘制类型标签（顶部居中）
    type_x = (size - type_width) / 2
    draw.text((type_x, type_y), type_label, fill=text_color, font=type_font)
    
    # 计算主文字位置
    line_heights = []
    line_widths = []
    for text in text_lines:
        bbox = draw.textbbox((0, 0), text, font=font)
        line_widths.append(bbox[2] - bbox[0])
        line_heights.append(bbox[3] - bbox[1])
    
    line_spacing = font_size * 0.10
    total_height = sum(line_heights) + line_spacing * (len(text_lines) - 1)
    
    # 绘制主文字（在类型标签下方居中）
    y_offset = main_area_top + (main_area_height - total_height) / 2
    for i, (text, width, height) in enumerate(zip(text_lines, line_widths, line_heights)):
        x = (size - width) / 2
        draw.text((x, y_offset), text, fill=text_color, font=font)
        y_offset += height + line_spacing
    
    img.save(output_path, 'PNG')

def create_background_xml(color_hex, output_path):
    """创建 Vector Drawable 背景图标"""
    xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{color_hex}"
        android:pathData="M0,0h108v108h-108z"/>
</vector>
'''
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w') as f:
        f.write(xml_content)

def create_foreground_xml(color_hex, text_lines, output_path):
    """创建 Vector Drawable 前景图标（简化版）"""
    xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- 透明前景，文字通过PNG图标显示 -->
</vector>
'''
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w') as f:
        f.write(xml_content)

def update_colors_xml(color_hex, colors_path):
    """更新 colors.xml 中的图标背景色"""
    import re
    
    os.makedirs(os.path.dirname(colors_path), exist_ok=True)
    
    if os.path.exists(colors_path):
        with open(colors_path, 'r') as f:
            content = f.read()
        
        if 'ic_launcher_background' in content:
            content = re.sub(
                r'<color name="ic_launcher_background">[^<]+</color>',
                f'<color name="ic_launcher_background">{color_hex}</color>',
                content
            )
        else:
            content = content.replace(
                '</resources>', 
                f'    <color name="ic_launcher_background">{color_hex}</color>\n</resources>'
            )
        
        with open(colors_path, 'w') as f:
            f.write(content)
    else:
        colors_xml = f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{color_hex}</color>
</resources>
'''
        with open(colors_path, 'w') as f:
            f.write(colors_xml)

def main():
    base_path = os.path.dirname(os.path.abspath(__file__))
    
    print("开始生成图标...")
    
    for module_dir, module_type, text_lines in MODULES:
        color = COLORS[module_type]
        print(f"  处理模块: {module_dir} ({module_type}) - {text_lines}")
        
        res_path = os.path.join(base_path, module_dir, 'src', 'main', 'res')
        
        if not os.path.exists(res_path):
            print(f"    警告: {res_path} 不存在，跳过")
            continue
        
        # 获取类型标签
        type_label = TYPE_LABELS[module_type]
        
        # 生成各种尺寸的 PNG 图标
        for mipmap_folder, size in ICON_SIZES.items():
            mipmap_path = os.path.join(res_path, mipmap_folder)
            os.makedirs(mipmap_path, exist_ok=True)
            
            # 方形图标
            icon_path = os.path.join(mipmap_path, 'ic_launcher.png')
            create_icon(size, color, text_lines, icon_path, type_label)
            print(f"    创建: {icon_path}")
            
            # 圆形图标
            round_icon_path = os.path.join(mipmap_path, 'ic_launcher_round.png')
            create_round_icon(size, color, text_lines, round_icon_path, type_label)
            print(f"    创建: {round_icon_path}")
        
        # 创建/更新 drawable 目录
        drawable_path = os.path.join(res_path, 'drawable')
        os.makedirs(drawable_path, exist_ok=True)
        
        bg_path = os.path.join(drawable_path, 'ic_launcher_background.xml')
        create_background_xml(color, bg_path)
        print(f"    创建: {bg_path}")
        
        fg_path = os.path.join(drawable_path, 'ic_launcher_foreground.xml')
        create_foreground_xml(color, text_lines, fg_path)
        print(f"    创建: {fg_path}")
        
        # 注意：不创建 mipmap-anydpi-v26 自适应图标，直接使用带文字的 PNG 图标
        # 删除已存在的 mipmap-anydpi-v26 目录（如果有）
        anydpi_path = os.path.join(res_path, 'mipmap-anydpi-v26')
        if os.path.exists(anydpi_path):
            import shutil
            shutil.rmtree(anydpi_path)
            print(f"    删除: {anydpi_path}")
        
        # 删除 drawable-v24 中的 foreground 文件（如果有）
        drawable_v24_fg = os.path.join(res_path, 'drawable-v24', 'ic_launcher_foreground.xml')
        if os.path.exists(drawable_v24_fg):
            os.remove(drawable_v24_fg)
            print(f"    删除: {drawable_v24_fg}")
        
        # 更新 colors.xml
        values_path = os.path.join(res_path, 'values')
        os.makedirs(values_path, exist_ok=True)
        colors_path = os.path.join(values_path, 'colors.xml')
        update_colors_xml(color, colors_path)
        print(f"    更新: {colors_path}")
    
    print("\n图标生成完成！")
    print("\n颜色方案:")
    for type_name, color in COLORS.items():
        print(f"  {type_name}: {color}")

if __name__ == '__main__':
    main()
