#!/bin/bash
# ============================================================================
# APK 安装脚本
# 用于安装所有测试 APK 到设备
# APK 列表从 apk_registry.json 动态读取，避免硬编码
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APK_DIR="${PROJECT_ROOT}/../apk-released"
REGISTRY_FILE="${PROJECT_ROOT}/config/apk_registry.json"

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

# ============================================================================
# 从 apk_registry.json 读取 APK 列表的辅助函数
# ============================================================================

get_scrolling_apks() {
    python3 -c "
import json
with open('${REGISTRY_FILE}') as f:
    data = json.load(f)
for app in data.get('scrolling_tests', {}).get('apps', []):
    apk = app.get('apk', '')
    if apk:
        print(apk)
"
}

get_launch_apks() {
    python3 -c "
import json
with open('${REGISTRY_FILE}') as f:
    data = json.load(f)
for app in data.get('launch_tests', {}).get('apps', []):
    for flavor in app.get('flavors', []):
        apk = flavor.get('apk', '')
        if apk:
            print(apk)
"
}

get_switch_apks() {
    python3 -c "
import json
with open('${REGISTRY_FILE}') as f:
    data = json.load(f)
for app in data.get('switch_tests', {}).get('apps', []):
    apk = app.get('apk', '')
    if apk:
        print(apk)
"
}

get_quick_apks() {
    python3 -c "
import json
with open('${REGISTRY_FILE}') as f:
    data = json.load(f)
quick_names = {'aosp-performance', 'compose'}
quick_launch = {'launch-aosp'}
quick_switch = {'switch-aosp'}
for app in data.get('scrolling_tests', {}).get('apps', []):
    if app['name'] in quick_names and app.get('apk'):
        print(app['apk'])
for app in data.get('launch_tests', {}).get('apps', []):
    if app['name'] in quick_launch:
        for flavor in app.get('flavors', []):
            if flavor.get('apk'):
                print(flavor['apk'])
for app in data.get('switch_tests', {}).get('apps', []):
    if app['name'] in quick_switch and app.get('apk'):
        print(app['apk'])
"
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
    echo "可用的 APK 文件 (来自 apk_registry.json):"
    echo ""
    echo "滑动测试 APK:"
    get_scrolling_apks | while IFS= read -r apk; do
        if [[ -f "$APK_DIR/$apk" ]]; then
            echo "  + $apk"
        else
            echo "  - $apk (未找到)"
        fi
    done
    echo ""
    echo "启动测试 APK:"
    get_launch_apks | while IFS= read -r apk; do
        if [[ -f "$APK_DIR/$apk" ]]; then
            echo "  + $apk"
        else
            echo "  - $apk (未找到)"
        fi
    done
    echo ""
    echo "Switch 测试 APK:"
    get_switch_apks | while IFS= read -r apk; do
        if [[ -f "$APK_DIR/$apk" ]]; then
            echo "  + $apk"
        else
            echo "  - $apk (未找到)"
        fi
    done
}

# 函数：安装滑动测试 APK
install_scrolling() {
    log_info "安装滑动测试 APK..."
    local success=0
    local failed=0

    while IFS= read -r apk; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done < <(get_scrolling_apks)

    log_info "滑动测试 APK: $success 成功, $failed 失败"
}

# 函数：安装启动测试 APK
install_launch() {
    log_info "安装启动测试 APK..."
    local success=0
    local failed=0

    while IFS= read -r apk; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done < <(get_launch_apks)

    log_info "启动测试 APK: $success 成功, $failed 失败"
}

# 函数：安装 Switch 测试 APK
install_switch() {
    log_info "安装 Switch 测试 APK..."
    local success=0
    local failed=0

    while IFS= read -r apk; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done < <(get_switch_apks)

    log_info "Switch 测试 APK: $success 成功, $failed 失败"
}

# 函数：安装核心 APK (快速模式)
install_quick() {
    log_info "安装核心测试 APK (快速模式)..."
    local success=0
    local failed=0

    while IFS= read -r apk; do
        if install_apk "$APK_DIR/$apk"; then
            ((success++))
        else
            ((failed++))
        fi
    done < <(get_quick_apks)

    log_info "核心 APK: $success 成功, $failed 失败"
}

# 主程序
main() {
    if [[ $# -eq 0 ]]; then
        show_help
        exit 0
    fi

    # 检查注册表文件
    if [[ ! -f "$REGISTRY_FILE" ]]; then
        log_error "APK 注册表不存在: $REGISTRY_FILE"
        exit 1
    fi

    # 检查 python3 是否可用
    if ! command -v python3 &> /dev/null; then
        log_error "需要 python3 来解析 APK 注册表"
        exit 1
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
