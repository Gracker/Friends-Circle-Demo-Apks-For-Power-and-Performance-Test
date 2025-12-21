#!/bin/bash

# 批量为所有模块添加应用信息显示的脚本

MODULES=(
    "scrolling-aosp-dualwindow:朋友圈双窗口版本:双Window刷新Demo"
    "scrolling-aosp-purerenderthread:朋友圈纯渲染线程版本:纯RenderThread列表滑动"
    "scrolling-aosp-power:朋友圈电量测试版本:电量测试版本，固定内容用于精确功耗测试"
    "scrolling-aosp-video:朋友圈视频版本:视频Feed版本"
    "scrolling-webview:朋友圈WebView版本:标准WebView Functor实现"
    "scrolling-webview-surface:朋友圈WebView Surface版本:WebView + SurfaceView"
    "scrolling-webview-texture:朋友圈WebView Texture版本:WebView + TextureView"
    "scrolling-webview-imagereader:朋友圈WebView ImageReader版本:WebView + ImageReader"
    "scrolling-gl-map:朋友圈OpenGL地图版本:OpenGL ES 2.0地图Demo"
    "scrolling-surface-map:朋友圈Surface地图版本:SurfaceView地图Demo"
)

BASE_DIR="/Users/chris/Code/HighPerformanceFriendsCircle"

for module_info in "${MODULES[@]}"; do
    IFS=':' read -r module_name app_name app_feature <<< "$module_info"

    echo "处理模块: $module_name"

    module_dir="$BASE_DIR/$module_name"

    # 检查目录是否存在
    if [ ! -d "$module_dir" ]; then
        echo "警告: 目录 $module_dir 不存在，跳过"
        continue
    fi

    # 跳过scrolling-aosp-douyin和scrolling-aosp-ebook（按用户要求）
    if [[ "$module_name" == "scrolling-aosp-douyin" || "$module_name" == "scrolling-aosp-ebook" ]]; then
        echo "跳过 $module_name（按用户要求）"
        continue
    fi

    # 创建model目录
    model_dir=$(find "$module_dir/src/main/java" -type d -name "com.example.*" | head -1)
    if [ -n "$model_dir" ]; then
        model_dir="$model_dir/model"
        mkdir -p "$model_dir"

        # 创建AppInfo.java
        package_name=$(basename "$model_dir" | sed 's|model||')
        cat > "$model_dir/AppInfo.java" << EOF
package ${package_name}model;

/**
 * 应用信息数据类
 */
public class AppInfo {
    private String appName;
    private String appFeature;
    private String packageName;

    public AppInfo(String appName, String appFeature, String packageName) {
        this.appName = appName;
        this.appFeature = appFeature;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppFeature() {
        return appFeature;
    }

    public void setAppFeature(String appFeature) {
        this.appFeature = appFeature;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
EOF
    fi

    # 复制布局文件
    if [ -d "$module_dir/src/main/res/layout" ]; then
        cp "$BASE_DIR/app/src/main/res/layout/include_app_info.xml" "$module_dir/src/main/res/layout/"
    fi

    echo "完成模块: $module_name"
    echo "---"
done

echo "所有模块处理完成！"