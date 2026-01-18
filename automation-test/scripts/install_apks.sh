#!/bin/bash
# ============================================================================
# APK 安装脚本
# 用于安装所有测试 APK 到设备
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APK_DIR="${PROJECT_ROOT}/../apk-released"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 函数：打印带颜色的消息
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 函数：检查设备连接
check_device() {
    if ! adb devices | grep -q "device$"; then
        log_error "没有检测到已连接的设备！"
        echo "请确保："
        echo "  1. 设备已通过 USB 连接"
        echo "  2. USB 调试已开启"
        echo "  3. 已授权该电脑进行调试"
        exit 1
    fi
    
    DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
    ANDROID_VERSION=$(adb shell getprop ro.build.version.release | tr -d '\r')
    log_info "已连接设备: ${DEVICE_MODEL} (Android ${ANDROID_VERSION})"
}

# 函数：安装单个 APK
install_apk() {
    local apk_file="$1"
    local apk_name=$(basename "$apk_file")
    
    if [[ ! -f "$apk_file" ]]; then
        log_warning "APK 不存在: $apk_name"
        return 1
    fi
    
    log_info "安装中: $apk_name"
    if adb install -r -d "$apk_file" > /dev/null 2>&1; then
        log_success "安装成功: $apk_name"
        return 0
    else
        log_error "安装失败: $apk_name"
        return 1
    fi
}

# 函数：显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --all           安装所有 APK"
    echo "  --scrolling     只安装滑动测试 APK"
    echo "  --launch        只安装启动测试 APK"
    echo "  --switch        只安装 Switch 测试 APK"
    echo "  --quick         安装核心测试 APK (aosp-performance + launch + switch)"
    echo "  --list          列出所有可用 APK"
    echo "  -h, --help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --all        # 安装全部 APK"
    echo "  $0 --quick      # 快速安装核心 APK"
}

# 函数：列出所有 APK
list_apks() {
    echo "可用的 APK 文件:"
    echo ""
    echo "滑动测试 APK:"
    ls -1 "$APK_DIR"/*-release-*.apk 2>/dev/null | grep -E "(aosp|compose|webview|gl-map|surface-map)" | grep -v "launch-" | grep -v "switch-" | while read f; do
        echo "  - $(basename "$f")"
    done
    echo ""
    echo "启动测试 APK:"
    ls -1 "$APK_DIR"/launch-*.apk 2>/dev/null | while read f; do
        echo "  - $(basename "$f")"
    done
    echo ""
    echo "Switch 测试 APK:"
    ls -1 "$APK_DIR"/switch-*.apk 2>/dev/null | while read f; do
        echo "  - $(basename "$f")"
    done
}

# 函数：安装滑动测试 APK
install_scrolling() {
    log_info "安装滑动测试 APK..."
    local success=0
    local failed=0
    
    SCROLLING_APKS=(
        "aosp-performance-release-v1.0.0.apk"
        "compose-release-v1.0.0.apk"
        "webview-release-v1.0.0.apk"
        "gl-map-release-v1.0.0.apk"
        "surface-map-release-v1.0.0.apk"
        "aosp-customscroller-release-v1.0.0.apk"
        "aosp-renderstress-release-v1.0.0.apk"
        "aosp-softwarerender-release-v1.0.0.apk"
        "aosp-douyin-release-v1.0.0.apk"
        "scrolling-aosp-ebook-release-v1.0.0.apk"
    )
    
    for apk in "${SCROLLING_APKS[@]}"; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done
    
    log_info "滑动测试 APK: $success 成功, $failed 失败"
}

# 函数：安装启动测试 APK
install_launch() {
    log_info "安装启动测试 APK..."
    local success=0
    local failed=0
    
    LAUNCH_APKS=(
        "launch-aosp-light-release-v1.0.0.apk"
        "launch-aosp-medium-release-v1.0.0.apk"
        "launch-aosp-heavy-release-v1.0.0.apk"
        "launch-compose-light-release-v1.0.0.apk"
        "launch-compose-medium-release-v1.0.0.apk"
        "launch-compose-heavy-release-v1.0.0.apk"
        "launch-webview-light-release-v1.0.0.apk"
        "launch-webview-medium-release-v1.0.0.apk"
        "launch-webview-heavy-release-v1.0.0.apk"
        "launch-gl-light-release-v1.0.0.apk"
        "launch-gl-medium-release-v1.0.0.apk"
        "launch-gl-heavy-release-v1.0.0.apk"
        "launch-game-light-release-v1.0.0.apk"
        "launch-game-medium-release-v1.0.0.apk"
        "launch-game-heavy-release-v1.0.0.apk"
    )
    
    for apk in "${LAUNCH_APKS[@]}"; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done
    
    log_info "启动测试 APK: $success 成功, $failed 失败"
}

# 函数：安装 Switch 测试 APK
install_switch() {
    log_info "安装 Switch 测试 APK..."
    local success=0
    local failed=0
    
    SWITCH_APKS=(
        "switch-aosp-release-v1.0.0.apk"
        "switch-webview-release-v1.0.0.apk"
        "switch-flutter-release-v1.0.0.apk"
    )
    
    for apk in "${SWITCH_APKS[@]}"; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done
    
    log_info "Switch 测试 APK: $success 成功, $failed 失败"
}

# 函数：安装核心 APK (快速模式)
install_quick() {
    log_info "安装核心测试 APK (快速模式)..."
    
    QUICK_APKS=(
        "aosp-performance-release-v1.0.0.apk"
        "compose-release-v1.0.0.apk"
        "launch-aosp-light-release-v1.0.0.apk"
        "launch-aosp-medium-release-v1.0.0.apk"
        "launch-aosp-heavy-release-v1.0.0.apk"
        "switch-aosp-release-v1.0.0.apk"
    )
    
    local success=0
    local failed=0
    
    for apk in "${QUICK_APKS[@]}"; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done
    
    log_info "核心 APK: $success 成功, $failed 失败"
}

# 主程序
main() {
    if [[ $# -eq 0 ]]; then
        show_help
        exit 0
    fi
    
    case "$1" in
        --all)
            check_device
            install_scrolling
            install_launch
            install_switch
            log_success "所有 APK 安装完成！"
            ;;
        --scrolling)
            check_device
            install_scrolling
            ;;
        --launch)
            check_device
            install_launch
            ;;
        --switch)
            check_device
            install_switch
            ;;
        --quick)
            check_device
            install_quick
            ;;
        --list)
            list_apks
            ;;
        -h|--help)
            show_help
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
