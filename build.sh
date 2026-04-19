#!/bin/bash

# 统一构建脚本 -- 编译所有模块并复制APK
#   ./build.sh          默认构建 Debug + Release
#   ./build.sh debug    构建Debug版本（启用TraceFix插桩）
#   ./build.sh release  构建Release版本（需要签名配置）
# 作者: Chris

set -euo pipefail

# ── 颜色定义 ──────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error()   { echo -e "${RED}❌ $1${NC}"; }
log_header()  { echo -e "${PURPLE}🚀 $1${NC}"; }
log_file()    { echo -e "${CYAN}   📱 $1${NC}"; }

# ── 输出目录清理 ──────────────────────────────────────────
clean_output_dirs() {
    local dirs=("apk-debug" "apk-release")
    for dir in "${dirs[@]}"; do
        mkdir -p "$dir"
        find "$dir" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
        log_info "已清理目录: $dir"
    done
}

# ── 参数解析 ──────────────────────────────────────────────
BUILD_TYPE="${1:-}"
if [ -n "$BUILD_TYPE" ] && [ "$BUILD_TYPE" != "debug" ] && [ "$BUILD_TYPE" != "release" ]; then
    echo "用法: $0 [debug|release]"
    echo ""
    echo "  (无参数) -- 默认先清理 apk-debug/apk-release，再依次构建 debug + release"
    echo "  debug    -- 先清理 apk-debug/apk-release，再构建Debug APK（启用TraceFix插桩）"
    echo "  release  -- 先清理 apk-debug/apk-release，再构建Release APK（需要签名配置）"
    exit 1
fi

if [ -z "$BUILD_TYPE" ]; then
    log_header "未指定构建类型，默认执行: debug + release"
    clean_output_dirs
    BUILD_SH_SKIP_OUTPUT_CLEAN=1 "$0" debug
    BUILD_SH_SKIP_OUTPUT_CLEAN=1 "$0" release
    log_success "Debug + Release 全部构建完成!"
    exit 0
fi

if [ "${BUILD_SH_SKIP_OUTPUT_CLEAN:-0}" != "1" ]; then
    clean_output_dirs
fi

# ── 构建类型参数 ──────────────────────────────────────────
if [ "$BUILD_TYPE" = "debug" ]; then
    BUILD_VARIANT="Debug"
    APK_SUFFIX="debug"
    OUTPUT_DIR="apk-debug"
    GRADLE_ARGS=(clean assembleDebug --parallel -PenableTraceFix=true)
    INSTALL_SCRIPT="install_all_debug_apks.sh"
    INSTALL_LABEL="Debug APK（含TraceFix插桩）"
else
    BUILD_VARIANT="Release"
    APK_SUFFIX="release"
    OUTPUT_DIR="apk-release"
    GRADLE_ARGS=(clean assembleRelease --parallel)
    INSTALL_SCRIPT="install_all_apks.sh"
    INSTALL_LABEL="${BUILD_VARIANT} APK"
fi

log_header "开始构建所有模块的${BUILD_VARIANT}版本APK..."

if [ "$BUILD_TYPE" = "debug" ]; then
    log_info "TraceFix 将在每个方法前后插入 Trace.beginSection()/endSection()"
fi

# ── Release签名检查 ──────────────────────────────────────
# 要求指定的签名变量已通过环境变量或 keystore.properties 设置
# 参数: $1=properties文件中的键名  $2=环境变量名  $3=配置文件路径
require_signing_var() {
    local prop_key="$1" env_key="$2" config_file="$3"
    if [ -z "${!prop_key:-}" ] && [ -z "${!env_key:-}" ]; then
        log_error "未配置 ${prop_key} (在 ${config_file} 中) 或 ${env_key} 环境变量"
        exit 1
    fi
}

if [ "$BUILD_TYPE" = "release" ]; then
    KEYSTORE_CONFIG_FILE="keystore.properties"
    if [ -f "$KEYSTORE_CONFIG_FILE" ]; then
        log_info "读取配置文件: $KEYSTORE_CONFIG_FILE"
        source "$KEYSTORE_CONFIG_FILE"
    fi

    require_signing_var "keystore_file"     "KEYSTORE_FILE_PATH" "$KEYSTORE_CONFIG_FILE"
    require_signing_var "keystore_password"  "KEYSTORE_PASSWORD"  "$KEYSTORE_CONFIG_FILE"
    require_signing_var "key_alias"          "KEY_ALIAS"          "$KEYSTORE_CONFIG_FILE"
    require_signing_var "key_password"       "KEY_PASSWORD"       "$KEYSTORE_CONFIG_FILE"

    KEYSTORE_FILE_PATH="${KEYSTORE_FILE_PATH:-$keystore_file}"
    KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-$keystore_password}"
    KEY_ALIAS="${KEY_ALIAS:-$key_alias}"
    KEY_PASSWORD="${KEY_PASSWORD:-$key_password}"

    if [ ! -f "$KEYSTORE_FILE_PATH" ]; then
        log_error "密钥库文件不存在: $KEYSTORE_FILE_PATH"
        exit 1
    fi
    log_success "使用密钥库: $KEYSTORE_FILE_PATH"

    export KEYSTORE_FILE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
    export STORE_PASSWORD="$KEYSTORE_PASSWORD"
fi

# ── 创建输出目录 ──────────────────────────────────────────
mkdir -p "$OUTPUT_DIR"
log_info "输出目录: $OUTPUT_DIR"

# ── 执行构建 ──────────────────────────────────────────────
log_info "执行: ./gradlew ${GRADLE_ARGS[*]}"

if ./gradlew "${GRADLE_ARGS[@]}"; then
    log_success "构建成功!"
else
    log_error "构建失败!"
    exit 1
fi

# ── 版本信息 ──────────────────────────────────────────────
VERSION_NAME=$(grep 'app-version-name' gradle/libs.versions.toml | sed 's/.*= *"\(.*\)".*/\1/')
BUILD_TIME=$(date +"%Y%m%d_%H%M%S")
GIT_HASH=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

log_info "版本信息: v$VERSION_NAME (构建时间: $BUILD_TIME, Git: $GIT_HASH)"

# ── 模块列表（目录名|APK基础名|显示名） ──────────────────
# APK后缀（-debug / -release）由 BUILD_TYPE 自动追加
declare -a MODULES=(
    "app|app|主应用"
    "scrolling-aosp-performance|scrolling-aosp-performance|滑动性能测试"
    "scrolling-aosp-power|scrolling-aosp-power|滑动电量测试"
    "scrolling-aosp-picasso|scrolling-aosp-picasso|滑动-Picasso"
    "scrolling-aosp-customscroller|scrolling-aosp-customscroller|滑动自定义滚动"
    "scrolling-aosp-renderstress|scrolling-aosp-renderstress|滑动渲染压测"
    "scrolling-aosp-softwarerender|scrolling-aosp-softwarerender|滑动软件渲染"
    "scrolling-aosp-douyin|scrolling-aosp-douyin|滑动抖音版本"
    "scrolling-aosp-video|scrolling-aosp-video|滑动视频版本"
    "scrolling-aosp-ebook|scrolling-aosp-ebook|滑动电子书"
    "scrolling-aosp-purerenderthread|scrolling-aosp-purerenderthread|滑动纯渲染线程"
    "scrolling-aosp-dualwindow|scrolling-aosp-dualwindow|滑动双窗口"
    "scrolling-aosp-mixedrender|scrolling-aosp-mixedrender|滑动混合渲染"
    "scrolling-compose|scrolling-compose|滑动Compose版本"
    "scrolling-webview|scrolling-webview|滑动WebView版本"
    "scrolling-webview-surface|scrolling-webview-surface|滑动WebView-Surface"
    "scrolling-webview-texture|scrolling-webview-texture|滑动WebView-Texture"
    "scrolling-webview-imagereader|scrolling-webview-imagereader|滑动WebView-ImageReader"
    "scrolling-surface-map|scrolling-surface-map|滑动Surface地图"
    "scrolling-gl-map|scrolling-gl-map|滑动GL地图"
    "launch-aosp|launch-aosp-light|AOSP启动器(Light)"
    "launch-aosp|launch-aosp-medium|AOSP启动器(Medium)"
    "launch-aosp|launch-aosp-heavy|AOSP启动器(Heavy)"
    "launch-compose|launch-compose-light|Compose启动器(Light)"
    "launch-compose|launch-compose-medium|Compose启动器(Medium)"
    "launch-compose|launch-compose-heavy|Compose启动器(Heavy)"
    "launch-webview|launch-webview-light|WebView启动器(Light)"
    "launch-webview|launch-webview-medium|WebView启动器(Medium)"
    "launch-webview|launch-webview-heavy|WebView启动器(Heavy)"
    "launch-gl|launch-gl-light|GL启动器(Light)"
    "launch-gl|launch-gl-medium|GL启动器(Medium)"
    "launch-gl|launch-gl-heavy|GL启动器(Heavy)"
    "launch-game|launch-game-light|Game启动器(Light)"
    "launch-game|launch-game-medium|Game启动器(Medium)"
    "launch-game|launch-game-heavy|Game启动器(Heavy)"
    # Switch 模块
    "switch-aosp|switch-aosp|Switch-AOSP"
    "switch-flutter|switch-flutter|Switch-Flutter"
    "switch-webview|switch-webview|Switch-WebView"
)

# ── 查找APK文件 ──────────────────────────────────────────
# 在标准输出目录和flavor子目录中查找APK文件
# 参数: $1=模块目录  $2=APK文件名(不含.apk)  $3=APK后缀(debug/release)
find_apk() {
    local module="$1" apk_name="$2" suffix="$3"
    local standard_path="${module}/build/outputs/apk/${suffix}/${apk_name}.apk"

    if [ -f "$standard_path" ]; then
        echo "$standard_path"
        return 0
    fi

    # launch模块使用flavor目录: light/medium/heavy
    for flavor in light medium heavy; do
        local flavor_path="${module}/build/outputs/apk/${flavor}/${suffix}/${apk_name}.apk"
        if [ -f "$flavor_path" ]; then
            echo "$flavor_path"
            return 0
        fi
    done
    return 0
}

# ── 复制APK ──────────────────────────────────────────────
log_info "复制APK文件到 $OUTPUT_DIR 目录..."

SUCCESS_COUNT=0
TOTAL_COUNT=0

for entry in "${MODULES[@]}"; do
    IFS='|' read -r MODULE APK_BASE MODULE_NAME <<< "$entry"
    APK_NAME="${APK_BASE}-${APK_SUFFIX}"
    TARGET_APK="$OUTPUT_DIR/${APK_NAME}-v${VERSION_NAME}.apk"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))

    SOURCE_APK=$(find_apk "$MODULE" "$APK_NAME" "$APK_SUFFIX")

    if [ -n "$SOURCE_APK" ]; then
        FILE_SIZE=$(du -h "$SOURCE_APK" | cut -f1)
        cp "$SOURCE_APK" "$TARGET_APK"
        log_success "$MODULE_NAME: 复制成功 -> $(basename "$TARGET_APK") ($FILE_SIZE)"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        log_error "$MODULE_NAME: 源文件不存在 (模块: $MODULE)"
    fi
done

# ── 结果统计 ──────────────────────────────────────────────
echo ""
log_header "构建结果统计"
log_info "成功: $SUCCESS_COUNT/$TOTAL_COUNT"

if [ $SUCCESS_COUNT -ne $TOTAL_COUNT ]; then
    log_error "部分APK构建失败，请检查错误信息"
    exit 1
fi

log_success "所有${BUILD_VARIANT} APK构建并复制成功!"

echo ""
log_info "生成的APK文件:"
for apk in "$OUTPUT_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        SIZE=$(du -h "$apk" | cut -f1)
        log_file "$(basename "$apk") (${SIZE})"
    fi
done

echo ""
log_info "安装命令示例:"
echo -e "${YELLOW}   adb install \"$OUTPUT_DIR/app-${APK_SUFFIX}-v${VERSION_NAME}.apk\"${NC}"
echo ""
echo -e "${YELLOW}   # 批量安装${NC}"
echo -e "${YELLOW}   for apk in $OUTPUT_DIR/*.apk; do adb install \"\$apk\"; done${NC}"

# ── 生成安装脚本 ──────────────────────────────────────────

cat > "$INSTALL_SCRIPT" << EOF
#!/bin/bash
# 自动安装所有${INSTALL_LABEL}的脚本
# 由 build.sh ${BUILD_TYPE} 生成

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if ! command -v adb &> /dev/null; then
    echo -e "\${RED}错误: adb 命令未找到。请确保 Android SDK 已安装并添加到 PATH。\${NC}"
    exit 1
fi

if ! adb devices | grep -q "device\$"; then
    echo -e "\${RED}错误: 没有连接的 Android 设备。请连接设备并开启 USB 调试。\${NC}"
    exit 1
fi

echo -e "\${BLUE}开始安装所有${INSTALL_LABEL}...\${NC}"
echo "输出目录: $OUTPUT_DIR"
echo ""

SUCCESS_COUNT=0
TOTAL_COUNT=0

for apk in $OUTPUT_DIR/*.apk; do
    if [ -f "\$apk" ]; then
        TOTAL_COUNT=\$((TOTAL_COUNT + 1))
        APK_NAME=\$(basename "\$apk")
        APK_SIZE=\$(du -h "\$apk" | cut -f1)

        echo -e "\${YELLOW}安装: \$APK_NAME (\$APK_SIZE)\${NC}"

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

# ── 构建类型特定提示 ──────────────────────────────────────
if [ "$BUILD_TYPE" = "debug" ]; then
    echo ""
    log_info "提示: 这些Debug APK已启用TraceFix插桩"
    log_info "使用 Perfetto/Systrace 可以看到所有方法级别的Trace信息"
fi

log_success "脚本执行完成!"
