#!/bin/bash

# 简化版自动编译并复制release版本APK的脚本
# 作者: Chris
# 使用方法: ./build_release_simple.sh

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_header() {
    echo -e "${PURPLE}🚀 $1${NC}"
}

# 显示开始信息
log_header "开始构建所有模块的Release版本APK..."

# 设置密钥库配置
KEYSTORE_CONFIG_FILE="keystore.properties"
# 读取配置文件（如果存在）
if [ -f "$KEYSTORE_CONFIG_FILE" ]; then
    log_info "读取配置文件: $KEYSTORE_CONFIG_FILE"
    source "$KEYSTORE_CONFIG_FILE"
fi

# 检查必要的配置是否存在
if [ -z "$keystore_file" ] && [ -z "$KEYSTORE_FILE_PATH" ]; then
    log_error "未配置 keystore_file (在 $KEYSTORE_CONFIG_FILE 中) 或 KEYSTORE_FILE_PATH 环境变量"
    exit 1
fi

if [ -z "$keystore_password" ] && [ -z "$KEYSTORE_PASSWORD" ]; then
    log_error "未配置 keystore_password (在 $KEYSTORE_CONFIG_FILE 中) 或 KEYSTORE_PASSWORD 环境变量"
    exit 1
fi

if [ -z "$key_alias" ] && [ -z "$KEY_ALIAS" ]; then
    log_error "未配置 key_alias (在 $KEYSTORE_CONFIG_FILE 中) 或 KEY_ALIAS 环境变量"
    exit 1
fi

if [ -z "$key_password" ] && [ -z "$KEY_PASSWORD" ]; then
    log_error "未配置 key_password (在 $KEYSTORE_CONFIG_FILE 中) 或 KEY_PASSWORD 环境变量"
    exit 1
fi

# 优先使用环境变量，其次使用配置文件
KEYSTORE_FILE_PATH="${KEYSTORE_FILE_PATH:-$keystore_file}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-$keystore_password}"
KEY_ALIAS="${KEY_ALIAS:-$key_alias}"
KEY_PASSWORD="${KEY_PASSWORD:-$key_password}"

# 检查密钥库是否存在
if [ ! -f "$KEYSTORE_FILE_PATH" ]; then
    log_error "密钥库文件不存在: $KEYSTORE_FILE_PATH"
    exit 1
fi

log_success "使用密钥库: $KEYSTORE_FILE_PATH"

# 创建输出目录
OUTPUT_DIR="apk-released"
mkdir -p "$OUTPUT_DIR"
log_info "输出目录: $OUTPUT_DIR"

# 设置签名环境变量（支持两种命名方式）
export KEYSTORE_FILE_PATH="$KEYSTORE_FILE_PATH"
export KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD"
export STORE_PASSWORD="$KEYSTORE_PASSWORD"
export KEY_ALIAS="$KEY_ALIAS"
export KEY_PASSWORD="$KEY_PASSWORD"

log_info "开始构建Release版本APK..."

# 执行构建
if ./gradlew assembleRelease --parallel; then
    log_success "构建成功!"
else
    log_error "构建失败!"
    exit 1
fi

# 获取版本信息
VERSION_NAME=$(grep "versionName" app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
BUILD_TIME=$(date +"%Y%m%d_%H%M%S")
GIT_HASH=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

log_info "版本信息: v$VERSION_NAME (构建时间: $BUILD_TIME, Git: $GIT_HASH)"

# 复制APK文件
log_info "复制APK文件到 $OUTPUT_DIR 目录..."

SUCCESS_COUNT=0
TOTAL_COUNT=0

# 定义模块数组（目录名|APK名|显示名）
declare -a MODULE_CONFIG=(
    "app|app-release|主应用"
    "aosp-performance|aosp-performance-release|AOSP性能测试"
    "aosp-power|aosp-power-release|AOSP电量测试"
    "aosp-picasso|aosp-picasso-release|AOSP-Picasso"
    "aosp-customscroller|aosp-customscroller-release|AOSP自定义滚动"
    "aosp-renderstress|aosp-renderstress-release|AOSP渲染压测"
    "aosp-softwarerender|aosp-softwarerender-release|AOSP软件渲染"
    "aosp-douyin|aosp-douyin-release|AOSP抖音版本"
    "aosp-video|aosp-video-release|AOSP视频版本"
    "aosp-purerenderthread|aosp-purerenderthread-release|AOSP纯渲染线程"
    "aosp-dualwindow|aosp-dualwindow-release|AOSP双窗口"
    "aosp-mixedrender|aosp-mixedrender-release|AOSP混合渲染"
    "compose|compose-release|Compose版本"
    "webview|webview-release|WebView版本"
    "webview-surface|webview-surface-release|WebView-Surface"
    "webview-texture|webview-texture-release|WebView-Texture"
    "webview-imagereader|webview-imagereader-release|WebView-ImageReader"
    "surface-map|surface-map-release|Surface地图"
    "gl-map|gl-map-release|GL地图"
    "launch-aosp|launch-aosp-light-release|AOSP启动器(Light)"
    "launch-aosp|launch-aosp-medium-release|AOSP启动器(Medium)"
    "launch-aosp|launch-aosp-heavy-release|AOSP启动器(Heavy)"
    "launch-compose|launch-compose-light-release|Compose启动器(Light)"
    "launch-compose|launch-compose-medium-release|Compose启动器(Medium)"
    "launch-compose|launch-compose-heavy-release|Compose启动器(Heavy)"
    "launch-webview|launch-webview-light-release|WebView启动器(Light)"
    "launch-webview|launch-webview-medium-release|WebView启动器(Medium)"
    "launch-webview|launch-webview-heavy-release|WebView启动器(Heavy)"
    "launch-gl|launch-gl-light-release|GL启动器(Light)"
    "launch-gl|launch-gl-medium-release|GL启动器(Medium)"
    "launch-gl|launch-gl-heavy-release|GL启动器(Heavy)"
    "launch-game|launch-game-light-release|Game启动器(Light)"
    "launch-game|launch-game-medium-release|Game启动器(Medium)"
    "launch-game|launch-game-heavy-release|Game启动器(Heavy)"
)

# 处理每个模块
for config in "${MODULE_CONFIG[@]}"; do
    # 解析配置
    IFS='|' read -r MODULE APK_NAME MODULE_NAME <<< "$config"
    
    SOURCE_APK="${MODULE}/build/outputs/apk/release/${APK_NAME}.apk"

    # 如果默认路径不存在，尝试在flavor目录下查找
    if [ ! -f "$SOURCE_APK" ]; then
        for flavor in light medium heavy; do
             TEMP_PATH="${MODULE}/build/outputs/apk/${flavor}/release/${APK_NAME}.apk"
             if [ -f "$TEMP_PATH" ]; then
                 SOURCE_APK="$TEMP_PATH"
                 break
             fi
        done
    fi
    
    TARGET_APK="$OUTPUT_DIR/${APK_NAME}-v${VERSION_NAME}.apk"
    
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
    
    if [ -f "$SOURCE_APK" ]; then
        # 获取文件大小
        FILE_SIZE=$(du -h "$SOURCE_APK" | cut -f1)
        
        # 复制文件
        cp "$SOURCE_APK" "$TARGET_APK"
        if [ $? -eq 0 ]; then
            log_success "$MODULE_NAME: 复制成功 -> $(basename "$TARGET_APK") ($FILE_SIZE)"
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            log_error "$MODULE_NAME: 复制失败"
        fi
    else
        log_error "$MODULE_NAME: 源文件不存在 $SOURCE_APK"
    fi
done

echo ""
log_header "构建结果统计"
log_info "成功: $SUCCESS_COUNT/$TOTAL_COUNT"

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    log_success "所有APK构建并复制成功!"
    
    echo ""
    log_info "生成的APK文件:"
    for apk in "$OUTPUT_DIR"/*.apk; do
        if [ -f "$apk" ]; then
            SIZE=$(du -h "$apk" | cut -f1)
            echo -e "${CYAN}   📱 $(basename "$apk") (${SIZE})${NC}"
        fi
    done
    
    echo ""
    log_info "安装命令示例:"
    echo -e "${YELLOW}   # 安装主应用${NC}"
    echo -e "${YELLOW}   adb install \"$OUTPUT_DIR/app-release-v${VERSION_NAME}.apk\"${NC}"
    echo ""
    echo -e "${YELLOW}   # 批量安装所有APK${NC}"
    echo -e "${YELLOW}   for apk in $OUTPUT_DIR/*.apk; do adb install \"\$apk\"; done${NC}"
    
    # 创建安装脚本
    INSTALL_SCRIPT="install_all_apks.sh"
    cat > "$INSTALL_SCRIPT" << EOF
#!/bin/bash
# 自动安装所有APK的脚本
# 由 build_release.sh 生成

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查adb是否可用
if ! command -v adb &> /dev/null; then
    echo -e "\${RED}错误: adb 命令未找到。请确保 Android SDK 已安装并添加到 PATH。\${NC}"
    exit 1
fi

# 检查设备是否连接
if ! adb devices | grep -q "device\$"; then
    echo -e "\${RED}错误: 没有连接的 Android 设备。请连接设备并开启 USB 调试。\${NC}"
    exit 1
fi

echo -e "\${BLUE}开始安装所有APK...\${NC}"
echo "输出目录: $OUTPUT_DIR"
echo ""

SUCCESS_COUNT=0
TOTAL_COUNT=0

# 安装每个APK
for apk in $OUTPUT_DIR/*.apk; do
    if [ -f "\$apk" ]; then
        TOTAL_COUNT=\$((TOTAL_COUNT + 1))
        APK_NAME=\$(basename "\$apk")
        APK_SIZE=\$(du -h "\$apk" | cut -f1)

        echo -e "\${YELLOW}安装: \$APK_NAME (\$APK_SIZE)\${NC}"

        # 尝试安装，如果已安装则使用 -r 参数替换
        if adb install -r "\$apk" 2>&1 | grep -q "Success\|already exists"; then
            echo -e "\${GREEN}✓ 安装成功\${NC}"
            SUCCESS_COUNT=\$((SUCCESS_COUNT + 1))
        else
            echo -e "\${RED}✗ 安装失败\${NC}"
        fi
        echo ""
    fi
done

echo -e "\${BLUE}安装完成统计:\${NC}"
echo -e "成功: \${GREEN}\$SUCCESS_COUNT\${NC}/\$TOTAL_COUNT"

if [ \$SUCCESS_COUNT -eq \$TOTAL_COUNT ]; then
    echo -e "\${GREEN}所有APK安装成功!\${NC}"
else
    echo -e "\${YELLOW}部分APK安装失败，可能是由于版本冲突或设备空间不足\${NC}"
fi

echo ""
echo -e "\${BLUE}设备上已安装的应用:\${NC}"
adb shell pm list packages | grep "example\|launch" | sed 's/package:/  - /'
EOF
    chmod +x "$INSTALL_SCRIPT"
    log_info "已创建安装脚本: $INSTALL_SCRIPT"

    echo ""
    log_info "其他可用脚本:"
    echo -e "${YELLOW}   # 安装所有 APK (包括非 launch 模块)${NC}"
    echo -e "${YELLOW}   ./install_all_apks.sh${NC}"
    echo ""
    echo -e "${YELLOW}   # 只安装 launch 变体 (light/medium/heavy)${NC}"
    echo -e "${YELLOW}   ./install_launch_variants.sh${NC}"
    echo ""
    echo -e "${YELLOW}   # 测试 launch 性能${NC}"
    echo -e "${YELLOW}   ./launch_performance_test.sh${NC}"
    echo ""
    echo -e "${YELLOW}   # 测试游戏性能${NC}"
    echo -e "${YELLOW}   ./launch_game.sh${NC}"
    
else
    log_error "部分APK构建失败，请检查错误信息"
    exit 1
fi

log_success "脚本执行完成! 🎉"