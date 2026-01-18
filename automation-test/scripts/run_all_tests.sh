#!/bin/bash
# ============================================================================
# 一键执行所有测试
# 完整的自动化性能测试流程
# ============================================================================

set -e

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 设置结果目录 (包含时间戳)
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_ROOT="$PROJECT_ROOT/results"
RESULTS_DIR="$RESULTS_ROOT/$TIMESTAMP"

mkdir -p "$RESULTS_DIR"
echo "测试结果目录: $RESULTS_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 函数：打印带颜色的消息
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_header() { echo -e "\n${CYAN}============================================================${NC}"; echo -e "${CYAN} $1${NC}"; echo -e "${CYAN}============================================================${NC}\n"; }

# 显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "一键执行完整的性能测试套件"
    echo ""
    echo "选项:"
    echo "  --full          完整测试（所有应用、所有负载类型）"
    echo "  --quick         快速测试（核心应用、部分负载类型）"
    echo "  --scrolling     只运行滑动测试"
    echo "  --launch        只运行启动测试"
    echo "  --switch        只运行 Switch 测试"
    echo "  --skip-install  跳过 APK 安装步骤"
    echo "  --report-only   只生成报告（使用已有测试数据）"
    echo "  -h, --help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --quick              # 快速测试"
    echo "  $0 --full               # 完整测试"
    echo "  $0 --scrolling          # 只测试滑动"
    echo "  $0 --skip-install --quick  # 跳过安装，快速测试"
}

# 检查 Python 环境
check_python() {
    if ! command -v python3 &> /dev/null; then
        log_error "未找到 python3，请先安装 Python 3"
        exit 1
    fi
    log_info "Python3: $(python3 --version)"
}

# 检查设备连接
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

# 安装 APK
run_install() {
    local mode=$1
    log_header "步骤 1/4: 安装测试 APK"
    
    if [[ "$mode" == "full" ]]; then
        bash "$SCRIPT_DIR/install_apks.sh" --all
    else
        bash "$SCRIPT_DIR/install_apks.sh" --quick
    fi
}

# 滑动测试
run_scrolling_test() {
    local mode=$1
    log_header "步骤 2/4: 滑动性能测试"
    
    if [[ "$mode" == "full" ]]; then
        python3 "$SCRIPT_DIR/run_scrolling_test.py" --all --results-dir "$RESULTS_DIR"
    else
        python3 "$SCRIPT_DIR/run_scrolling_test.py" --results-dir "$RESULTS_DIR"
    fi
}

# 启动测试
run_launch_test() {
    local mode=$1
    log_header "步骤 3/4: 启动性能测试"
    
    if [[ "$mode" == "full" ]]; then
        python3 "$SCRIPT_DIR/run_launch_test.py" --all --results-dir "$RESULTS_DIR"
    else
        python3 "$SCRIPT_DIR/run_launch_test.py" --results-dir "$RESULTS_DIR"
    fi
}

# Switch 测试
run_switch_test() {
    local mode=$1
    log_header "步骤 4/4: Switch 跳转测试"
    
    if [[ "$mode" == "full" ]]; then
        python3 "$SCRIPT_DIR/run_switch_test.py" --all --results-dir "$RESULTS_DIR"
    else
        python3 "$SCRIPT_DIR/run_switch_test.py" --results-dir "$RESULTS_DIR"
    fi
}

# 生成报告
run_report() {
    log_header "生成测试报告"
    python3 "$PROJECT_ROOT/lib/report_generator.py" --results-dir "$RESULTS_DIR"
}

# 主函数
main() {
    local mode="quick"
    local skip_install=false
    local report_only=false
    local run_scrolling=true
    local run_launch=true
    local run_switch=true
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --full)
                mode="full"
                shift
                ;;
            --quick)
                mode="quick"
                shift
                ;;
            --scrolling)
                run_launch=false
                run_switch=false
                shift
                ;;
            --launch)
                run_scrolling=false
                run_switch=false
                shift
                ;;
            --switch)
                run_scrolling=false
                run_launch=false
                shift
                ;;
            --skip-install)
                skip_install=true
                shift
                ;;
            --report-only)
                report_only=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 显示测试配置
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║               📱 自动化性能测试套件                           ║"
    echo "╠═══════════════════════════════════════════════════════════════╣"
    echo "║  测试模式: $(printf '%-51s' "$mode")║"
    echo "║  滑动测试: $(printf '%-51s' "$run_scrolling")║"
    echo "║  启动测试: $(printf '%-51s' "$run_launch")║"
    echo "║  Switch测试: $(printf '%-49s' "$run_switch")║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
    
    # 如果只生成报告
    if [[ "$report_only" == true ]]; then
        run_report
        exit 0
    fi
    
    # 检查环境
    log_header "环境检查"
    check_python
    check_device
    
    # 记录开始时间
    START_TIME=$(date +%s)
    
    # 安装 APK
    if [[ "$skip_install" == false ]]; then
        run_install "$mode"
    else
        log_info "跳过 APK 安装步骤"
    fi
    
    # 运行测试
    if [[ "$run_scrolling" == true ]]; then
        run_scrolling_test "$mode"
    fi
    
    if [[ "$run_launch" == true ]]; then
        run_launch_test "$mode"
    fi
    
    if [[ "$run_switch" == true ]]; then
        run_switch_test "$mode"
    fi
    
    # 生成报告
    run_report
    
    # 计算总耗时
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    MINUTES=$((DURATION / 60))
    SECONDS=$((DURATION % 60))
    
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║                    ✅ 测试完成                                 ║"
    echo "╠═══════════════════════════════════════════════════════════════╣"
    echo "║  总耗时: ${MINUTES}分${SECONDS}秒                                             ║"
    echo "║  报告位置: ${PROJECT_ROOT}/results/                           ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
}

main "$@"
