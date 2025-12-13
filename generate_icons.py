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
    'launch': '#9C27B0',    # 紫色 - Launch模块
}

# 类型显示名称
# 类型显示名称
TYPE_LABELS = {
    'aosp': 'Scrolling',
    'webview': 'Scrolling',
    'map': 'Scrolling',
    'launch': 'Launch',
}

# 模块配置: (模块目录, 类型, 显示文字列表)
MODULES = [
    ('app', 'aosp', ['AOSP', 'App']),
    ('aosp-performance', 'aosp', ['AOSP', 'Perf']),
    ('aosp-power', 'aosp', ['AOSP', 'Power']),
    ('aosp-picasso', 'aosp', ['AOSP', 'Picasso']),
    ('aosp-customscroller', 'aosp', ['AOSP', 'Custom Scroller']),
    ('aosp-renderstress', 'aosp', ['AOSP', 'Render Stress']),
    ('aosp-softwarerender', 'aosp', ['AOSP', 'Software Render']),
    ('aosp-douyin', 'aosp', ['AOSP', 'Douyin']),
    ('aosp-video', 'aosp', ['AOSP', 'Video']),
    ('aosp-ebook', 'aosp', ['AOSP', 'Ebook']),
    ('aosp-purerenderthread', 'aosp', ['AOSP', 'Pure Render']),
    ('aosp-dualwindow', 'aosp', ['AOSP', 'Dual Window']),
    ('aosp-mixedrender', 'aosp', ['AOSP', 'Mixed Render']),
    ('compose', 'aosp', ['Compose', 'Demo']),
    ('webview', 'webview', ['WebView', 'Functor']),
    ('webview-surface', 'webview', ['WebView', 'Surface']),
    ('webview-texture', 'webview', ['WebView', 'Texture']),
    ('webview-imagereader', 'webview', ['WebView', 'ImgReader']),
    ('surface-map', 'map', ['Map', 'Surface']),
    ('gl-map', 'map', ['Map', 'GL']),
    # Launch modules (flavors handled in loop)
    # entry format: (dir, type, [Line1, Line2]) -> Line 3 will be flavor name
    ('launch-aosp', 'launch', ['Launch', 'AOSP']),
    ('launch-gl', 'launch', ['Launch', 'GL']),
    ('launch-compose', 'launch', ['Launch', 'Compose']),
    ('launch-webview', 'launch', ['Launch', 'WebView']),
]

# Launch Flavors
LAUNCH_FLAVORS = ['light', 'medium', 'heavy']

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

def calculate_max_font_size(draw, text_list, max_width, max_height, initial_size):
    """
    Find the largest font size such that the list of text lines fits within the
    given width and total height.
    """
    font_size = initial_size
    min_font_size = 8
    
    while font_size >= min_font_size:
        font = get_font(font_size)
        
        # Calculate dimensions
        max_line_width = 0
        total_text_height = 0
        line_spacing = font_size * 0.15
        
        valid = True
        for text in text_list:
            bbox = draw.textbbox((0, 0), text, font=font)
            w = bbox[2] - bbox[0]
            h = bbox[3] - bbox[1]
            if w > max_width:
                valid = False
                break
            max_line_width = max(max_line_width, w)
            total_text_height += h
        
        if not valid:
             font_size -= 1
             continue

        total_height = total_text_height + line_spacing * (len(text_list) - 1)
        
        if total_height <= max_height:
            return font_size, font, total_height
        
        font_size -= 1
    
    return min_font_size, get_font(min_font_size), min_font_size * len(text_list) # Approximate

def create_icon_content(draw, size, text_color, text_lines, top_label_text):
    """
    Renders text onto the draw object.
    Layout:
    Header: Top Label + Type -> Anchored Top, Small.
    Detail: Name/Flavor -> Fills remaining space, auto-wraps if needed, max size capped.
    """
    
    # Define Areas
    padding = size * 0.08
    available_width = size - padding * 2
    
    # Split text
    header_lines = [top_label_text, text_lines[0]] 
    detail_raw = text_lines[1] if len(text_lines) > 1 else ""
    
    # 1. Draw Header (Fixed at Top)
    # Target height for header: ~30% of icon
    header_max_h = size * 0.30
    header_font_size, header_font, header_actual_h = calculate_max_font_size(
        draw, header_lines, available_width, header_max_h, int(size * 0.18)
    )
    
    # Draw Header anchored to top margin
    top_margin = size * 0.10
    y = top_margin
    line_spacing = header_font_size * 0.15
    
    for text in header_lines:
        bbox = draw.textbbox((0, 0), text, font=header_font)
        w = bbox[2] - bbox[0]
        h = bbox[3] - bbox[1]
        x = (size - w) / 2
        draw.text((x, y), text, fill=text_color, font=header_font)
        y += h + line_spacing
    
    header_bottom = y
    
    # 2. Draw Detail (Remaining Space)
    if not detail_raw:
        return

    # Usable vertical space for detail
    # From header_bottom to (size - padding)
    # Add some breathing room below header
    detail_top_y = header_bottom + size * 0.02
    detail_max_h = (size - padding) - detail_top_y
    
    if detail_max_h < size * 0.2: 
        # Safety fallback if header took too much space (unlikely with constraints)
        detail_max_h = size * 0.2
        
    # Logic: Try 1 line. If font too small & has spaces, try 2 lines.
    
    # Attempt 1 Line
    # Cap max font size to avoid purely massive text (e.g. "Heavy")
    # User said "Heavy" was too big/not uniform.
    # Let's cap max font at say 35% of size.
    max_font_cap = int(size * 0.35)
    
    font_1, font_obj_1, h_1 = calculate_max_font_size(
        draw, [detail_raw], available_width, detail_max_h, max_font_cap
    )
    
    best_lines = [detail_raw]
    best_font = font_obj_1
    best_h = h_1
    
    # Check if we should wrap
    # Logic: If font is small (< 18% size) AND we have spaces
    if font_1 < size * 0.18 and ' ' in detail_raw:
        # Simple split by middle space
        words = detail_raw.split(' ')
        # Naively try to balance: combine words into 2 lines
        mid = len(words) // 2
        # Try different splits? usually just 1 split is enough for "Render Stress"
        # Just split into half
        if len(words) >= 2:
             # Find split point closest to middle length
             # Simple heuristic: split in half by count, or just join all but last?
             # Let's just do simple first half / second half
             # Actually "Render Stress" -> "Render", "Stress"
             # "Custom Scroller" -> "Custom", "Scroller"
             # "Software Render" -> "Software", "Render"
             # "Pure Render Thread" -> "Pure Render", "Thread"
             
             # Better split: keep adding words to line 1 until len > total/2
             total_len = len(detail_raw)
             current_len = 0
             split_idx = 1
             for i, w in enumerate(words):
                 current_len += len(w)
                 if current_len > total_len / 2:
                     split_idx = max(1, i + 1) # ensure at least 1 word
                     break
             
             # Adjust split index if it puts everything in one line (shouldn't happen if logic right)
             if split_idx >= len(words): split_idx = len(words) - 1
             
             line1 = " ".join(words[:split_idx])
             line2 = " ".join(words[split_idx:])
             wrap_lines = [line1, line2]
             
             font_2, font_obj_2, h_2 = calculate_max_font_size(
                draw, wrap_lines, available_width, detail_max_h, max_font_cap
             )
             
             # If wrapped font is significantly bigger, use it
             if font_2 > font_1 * 1.1:
                 best_lines = wrap_lines
                 best_font = font_obj_2
                 best_h = h_2
                 
    # Draw Best Lines Centered in Remaining Space
    # Center vertically in [detail_top_y, size - padding]
    available_v_center = detail_top_y + detail_max_h / 2
    render_start_y = available_v_center - best_h / 2
    
    y = render_start_y
    # Line spacing for detail block
    # If font is large, spacing doesn't need to be huge
    d_line_spacing = best_font.size * 0.1
    
    for text in best_lines:
        bbox = draw.textbbox((0, 0), text, font=best_font)
        w = bbox[2] - bbox[0]
        h = bbox[3] - bbox[1]
        x = (size - w) / 2
        draw.text((x, y), text, fill=text_color, font=best_font)
        y += h + d_line_spacing


def create_icon(size, color_hex, text_lines, output_path, type_label):
    """方形图标"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    bg_color = hex_to_rgb(color_hex)
    text_color = get_text_color(bg_color)
    
    radius = size * 0.18
    draw.rounded_rectangle([0, 0, size, size], radius=radius, fill=bg_color)
    
    create_icon_content(draw, size, text_color, text_lines, type_label)
    img.save(output_path, 'PNG')

def create_round_icon(size, color_hex, text_lines, output_path, type_label):
    """圆形图标"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    bg_color = hex_to_rgb(color_hex)
    text_color = get_text_color(bg_color)
    
    draw.ellipse([0, 0, size, size], fill=bg_color)
    
    # Inner area for round icon is smaller to avoid clipping
    # We cheat by creating a temporary smaller image/draw surface or just padding more?
    # Simpler: Just pass a smaller size concept to content drawer?
    # Or just let create_icon_content handle a scale factor.
    # For simplicity, we just draw with same logic but assume slightly less width availability if we wanted perfect math,
    # but create_icon_content uses 'padding = size * 0.08'. For round, corners are cut.
    # Let's increase padding effectively by passing a smaller 'effective' size or just drawing normally?
    # Actually, square box inside circle max size is size / sqrt(2) ~= 0.707 size.
    # Let's scale down the effective drawing area.
    
    # But wait, create_icon_content takes 'size' as canvas size and calcs padding.
    # We can't easily scale the canvas.
    # Let's making a crop version.
    # Better: just use larger padding in logic?
    # Re-impl simplified:
    
    # Define Areas
    inner_size = size * 0.72 # Safe square inside circle
    offset = (size - inner_size) / 2
    
    # Create a temporary image for the inner content
    temp_img = Image.new('RGBA', (int(inner_size), int(inner_size)), (0,0,0,0))
    temp_draw = ImageDraw.Draw(temp_img)
    
    create_icon_content(temp_draw, int(inner_size), text_color, text_lines, type_label)
    
    # Paste centered
    img.paste(temp_img, (int(offset), int(offset)), temp_img)
    
    img.save(output_path, 'PNG')

def create_background_xml(color_hex, output_path):
    xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{color_hex}"
        android:pathData="M0,0h108v108h-108z"/>
</vector>'''
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w') as f:
        f.write(xml_content)

def create_foreground_xml(color_hex, text_lines, output_path):
    xml_content = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Transparent foreground -->
</vector>'''
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w') as f:
        f.write(xml_content)

def update_colors_xml(color_hex, colors_path):
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
</resources>'''
        with open(colors_path, 'w') as f:
            f.write(colors_xml)

def process_module(base_path, module_dir, module_type, text_lines, flavor=None):
    """
    Generate icons for a specific module configuration.
    If flavor is provided, writes to src/{flavor}/res.
    """
    color = COLORS[module_type]
    type_label = TYPE_LABELS[module_type]
    
    # Determine output resource directory
    if flavor:
        # e.g. launch-aosp/src/light/res
        res_dir = 'res' # standard
        src_set = flavor
        # Special handling: Gradle expects 'src/light/res' usually.
        # Verify if 'src/main' exists, usually peer.
        res_path = os.path.join(base_path, module_dir, 'src', flavor, 'res')
    else:
        res_path = os.path.join(base_path, module_dir, 'src', 'main', 'res')
    
    print(f"  Generating for {module_dir} ({module_type}) variant={flavor if flavor else 'main'} -> {text_lines}")
    
    # Ensure directory exists (flavors might need creation)
    if not os.path.exists(res_path):
        os.makedirs(res_path, exist_ok=True)

    # Generate PNGs
    for mipmap_folder, size in ICON_SIZES.items():
        mipmap_path = os.path.join(res_path, mipmap_folder)
        os.makedirs(mipmap_path, exist_ok=True)
        
        create_icon(size, color, text_lines, os.path.join(mipmap_path, 'ic_launcher.png'), type_label)
        create_round_icon(size, color, text_lines, os.path.join(mipmap_path, 'ic_launcher_round.png'), type_label)

    # Generate XMLs
    drawable_path = os.path.join(res_path, 'drawable')
    os.makedirs(drawable_path, exist_ok=True)
    create_background_xml(color, os.path.join(drawable_path, 'ic_launcher_background.xml'))
    create_foreground_xml(color, text_lines, os.path.join(drawable_path, 'ic_launcher_foreground.xml'))
    
    # Update Colors
    values_path = os.path.join(res_path, 'values')
    os.makedirs(values_path, exist_ok=True)
    update_colors_xml(color, os.path.join(values_path, 'colors.xml'))
    
    # Clean up adaptive - v26 if exists
    anydpi_path = os.path.join(res_path, 'mipmap-anydpi-v26')
    if os.path.exists(anydpi_path):
        import shutil
        shutil.rmtree(anydpi_path)


def main():
    base_path = os.path.dirname(os.path.abspath(__file__))
    print("开始生成图标...")
    
    for module_dir, module_type, base_text_lines in MODULES:
        
        if module_type == 'launch':
            # Handle Flavors for Launch Modules
            for flavor in LAUNCH_FLAVORS:
                # Text: [Type, Flavor] e.g. ["AOSP", "Light"]
                # base_text_lines in config is ["Launch", "AOSP"]
                # We need [Type, Flavor]. Wait.
                # In MODULES config for launch: ('launch-aosp', 'launch', ['Launch', 'AOSP'])
                # TopLabel is defined by TYPE_LABELS['launch'] -> "Launch"
                # So we pass text_lines = ["AOSP", "Light"] ?
                # 
                # check process_module/create_icon logic:
                # create_icon(..., text_lines, type_label)
                # create_icon_content(..., text_lines, top_label_text)
                # header_lines = [top_label_text, text_lines[0]]
                # detail_line = text_lines[1]
                #
                # If we pass text_lines=["AOSP", "Light"] and type_label="Launch":
                # Header = ["Launch", "AOSP"] -> Equal Size. Correct.
                # Detail = "Light" -> Smaller. Correct.
                #
                # So for launch modules, 'base_text_lines' should be just the ["AOSP"] part?
                # In current replacement MODULES: ('launch-aosp', 'launch', ['Launch', 'AOSP'])
                # This has 2 items.
                # I should just take the second item "AOSP" as text_lines[0], and Flavor as text_lines[1].
                
                type_name = base_text_lines[1] # "AOSP"
                flavor_display = flavor.capitalize() # "Light"
                
                text_lines = [type_name, flavor_display]
                process_module(base_path, module_dir, module_type, text_lines, flavor)
                
        else:
            # Regular Modules
            # MODULES entry: ('app', 'aosp', ['AOSP', 'App'])
            # We want Header: "Scrolling", "AOSP"
            # Detail: "App"
            # TYPE_LABELS['aosp'] is "Scrolling"
            #
            # If we pass text_lines = ["AOSP", "App"]:
            # Header = ["Scrolling", "AOSP"] -> Equal Size. Correct.
            # Detail = "App" -> Smaller. Correct.
            
            process_module(base_path, module_dir, module_type, base_text_lines, flavor=None)

    print("\n图标生成完成！")

if __name__ == '__main__':
    main()
