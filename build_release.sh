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
DEFAULT_KEYSTORE_PATH="/Users/chris/Code/APK-Key/Chris"
DEFAULT_KEYSTORE_PASSWORD="@Ab2960617"
DEFAULT_KEY_ALIAS="key0"
DEFAULT_KEY_PASSWORD="@Ab2960617"

# 读取配置文件（如果存在）
if [ -f "$KEYSTORE_CONFIG_FILE" ]; then
    log_info "读取配置文件: $KEYSTORE_CONFIG_FILE"
    source "$KEYSTORE_CONFIG_FILE"
    KEYSTORE_FILE_PATH="${keystore_file:-$DEFAULT_KEYSTORE_PATH}"
    KEYSTORE_PASSWORD="${keystore_password:-$DEFAULT_KEYSTORE_PASSWORD}"
    KEY_ALIAS="${key_alias:-$DEFAULT_KEY_ALIAS}"
    KEY_PASSWORD="${key_password:-$DEFAULT_KEY_PASSWORD}"
else
    log_warning "配置文件 $KEYSTORE_CONFIG_FILE 不存在，使用默认配置"
    KEYSTORE_FILE_PATH="$DEFAULT_KEYSTORE_PATH"
    KEYSTORE_PASSWORD="$DEFAULT_KEYSTORE_PASSWORD"
    KEY_ALIAS="$DEFAULT_KEY_ALIAS"
    KEY_PASSWORD="$DEFAULT_KEY_PASSWORD"
fi

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

# 设置签名环境变量
export KEYSTORE_FILE_PATH="$KEYSTORE_FILE_PATH"
export KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD"
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

# 定义模块和APK文件映射
MODULES="app wechatfriendforperformance wechatfriendforpower wechatfriendforwebview"
APK_NAMES="app-release wechatfriendforperformance-release wechatfriendforpower-release wechatfriendforwebview-release"
MODULE_NAMES="主应用 性能测试 电量测试 WebView版本"

# 转换为数组
set -- $MODULES
MODULES_ARRAY=("$@")
set -- $APK_NAMES  
APK_NAMES_ARRAY=("$@")
set -- $MODULE_NAMES
MODULE_NAMES_ARRAY=("$@")

# 处理每个模块
for i in $(seq 0 3); do
    MODULE="${MODULES_ARRAY[$i]}"
    APK_NAME="${APK_NAMES_ARRAY[$i]}"
    MODULE_NAME="${MODULE_NAMES_ARRAY[$i]}"
    
    SOURCE_APK="${MODULE}/build/outputs/apk/release/${APK_NAME}.apk"
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
echo "开始安装所有APK..."
for apk in $OUTPUT_DIR/*.apk; do
    echo "安装: \$(basename "\$apk")"
    adb install "\$apk"
done
echo "安装完成!"
EOF
    chmod +x "$INSTALL_SCRIPT"
    log_info "已创建安装脚本: $INSTALL_SCRIPT"
    
else
    log_error "部分APK构建失败，请检查错误信息"
    exit 1
fi

log_success "脚本执行完成! 🎉"