#!/bin/bash

# 安装所有 launch 变体的脚本
# 包括 launch-aosp, launch-compose, launch-webview, launch-gl, launch-game

set -e

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

# 检查adb是否可用
if ! command -v adb &> /dev/null; then
    log_error "adb 命令未找到。请确保 Android SDK 已安装并添加到 PATH。"
    exit 1
fi

# 检查设备是否连接
if ! adb devices | grep -q "device$"; then
    log_error "没有连接的 Android 设备。请连接设备并开启 USB 调试。"
    exit 1
fi

log_header "开始安装所有 Launch 变体 APK..."

# 定义 APK 路径
declare -a APK_CONFIG=(
    "launch-aosp|aosp-launch-variants|launch-aosp"
    "launch-compose|compose-launch-variants|launch-compose"
    "launch-webview|webview-launch-variants|launch-webview"
    "launch-gl|gl-launch-variants|launch-gl"
    "launch-game|game-launch-variants|launch-game"
)

# 函数：安装单个模块的所有变体
install_module_variants() {
    local module_name=$1
    local display_name=$2
    local module_path=$3

    log_info "处理 ${display_name} 模块..."

    SUCCESS_COUNT=0
    TOTAL_COUNT=0

    for variant in light medium heavy; do
        TOTAL_COUNT=$((TOTAL_COUNT + 1))

        # 查找 APK 文件
        local apk_path="${module_path}/build/outputs/apk/${variant}/release/${module_path}-${variant}-release.apk"

        # 如果 release 版本不存在，尝试 debug 版本
        if [ ! -f "$apk_path" ]; then
            apk_path="${module_path}/build/outputs/apk/${variant}/debug/${module_path}-${variant}-debug.apk"
        fi

        if [ -f "$apk_path" ]; then
            local apk_size=$(du -h "$apk_path" | cut -f1)
            log_warning "安装 ${display_name} (${variant}) - 大小: ${apk_size}"

            if adb install -r "$apk_path" >/dev/null 2>&1; then
                log_success "${display_name} (${variant}) 安装成功"
                SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            else
                log_error "${display_name} (${variant}) 安装失败"
            fi
        else
            log_error "${display_name} (${variant}) APK 文件不存在: $apk_path"
        fi
    done

    return $SUCCESS_COUNT
}

# 统计数据
TOTAL_SUCCESS=0
TOTAL_APK=0

# 安装每个模块的变体
for config in "${APK_CONFIG[@]}"; do
    IFS='|' read -r module_name display_name module_path <<< "$config"

    install_module_variants "$module_name" "$display_name" "$module_path"
    SUCCESS=$?
    TOTAL_SUCCESS=$((TOTAL_SUCCESS + SUCCESS))
    TOTAL_APK=$((TOTAL_APK + 3))  # 每个模块有3个变体
    echo ""
done

# 显示统计信息
log_header "安装结果统计"
log_info "成功安装: $TOTAL_SUCCESS/$TOTAL_APK"

if [ $TOTAL_SUCCESS -eq $TOTAL_APK ]; then
    log_success "所有 Launch 变体安装成功!"
    echo ""
    log_info "快速启动命令:"
    echo -e "${CYAN}  # AOSP Launchers${NC}"
    echo -e "${YELLOW}  adb shell am start -n com.example.launch.aosp.light/.LauncherActivity${NC}"
    echo -e "${YELLOW}  adb shell am start -n com.example.launch.aosp.medium/.LauncherActivity${NC}"
    echo -e "${YELLOW}  adb shell am start -n com.example.launch.aosp.heavy/.LauncherActivity${NC}"
    echo ""
    echo -e "${CYAN}  # Game Launchers${NC}"
    echo -e "${YELLOW}  adb shell monkey -p com.example.launch.game.light -c android.intent.category.LAUNCHER 1${NC}"
    echo -e "${YELLOW}  adb shell monkey -p com.example.launch.game.medium -c android.intent.category.LAUNCHER 1${NC}"
    echo -e "${YELLOW}  adb shell monkey -p com.example.launch.game.heavy -c android.intent.category.LAUNCHER 1${NC}"
else
    log_warning "部分 APK 安装失败。请检查："
    log_warning "1. 是否已经构建了所有 APK (运行 ./build_release.sh)"
    log_warning "2. 设备存储空间是否充足"
    log_warning "3. 是否有签名冲突"
fi

echo ""
log_info "使用说明:"
echo -e "  ${YELLOW}# 使用 launch_performance_test.sh 测试 AOSP 变体${NC}"
echo -e "  ${YELLOW}./launch_performance_test.sh${NC}"
echo ""
echo -e "  ${YELLOW}# 使用 launch_game.sh 测试游戏变体${NC}"
echo -e "  ${YELLOW}./launch_game.sh${NC}"

echo ""
log_success "脚本执行完成! 🎉"