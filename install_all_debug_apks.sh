#!/bin/bash
# 自动安装所有Debug APK（含TraceFix插桩）的脚本
# 由 build.sh debug 生成

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if ! command -v adb &> /dev/null; then
    echo -e "${RED}错误: adb 命令未找到。请确保 Android SDK 已安装并添加到 PATH。${NC}"
    exit 1
fi

if ! adb devices | grep -q "device$"; then
    echo -e "${RED}错误: 没有连接的 Android 设备。请连接设备并开启 USB 调试。${NC}"
    exit 1
fi

echo -e "${BLUE}开始安装所有Debug APK（含TraceFix插桩）...${NC}"
echo "输出目录: apk-debug"
echo ""

SUCCESS_COUNT=0
TOTAL_COUNT=0

for apk in apk-debug/*.apk; do
    if [ -f "$apk" ]; then
        TOTAL_COUNT=$((TOTAL_COUNT + 1))
        APK_NAME=$(basename "$apk")
        APK_SIZE=$(du -h "$apk" | cut -f1)

        echo -e "${YELLOW}安装: $APK_NAME ($APK_SIZE)${NC}"

        if adb install -r "$apk" 2>&1 | grep -q "Success\|already exists"; then
            echo -e "${GREEN}✓ 安装成功${NC}"
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            echo -e "${RED}✗ 安装失败${NC}"
        fi
        echo ""
    fi
done

echo -e "${BLUE}安装完成统计:${NC}"
echo -e "成功: ${GREEN}$SUCCESS_COUNT${NC}/$TOTAL_COUNT"

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    echo -e "${GREEN}所有APK安装成功!${NC}"
else
    echo -e "${YELLOW}部分APK安装失败，可能是由于版本冲突或设备空间不足${NC}"
fi

echo ""
echo -e "${BLUE}设备上已安装的应用:${NC}"
adb shell pm list packages | grep "example\|launch" | sed 's/package:/  - /'
