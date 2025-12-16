#!/bin/bash

# Performance Test App Command Line Launcher Script
# This script provides easy commands to launch different load types via adb

PACKAGE_NAME="com.example.wechatfriendforperformance"
MAIN_ACTIVITY="${PACKAGE_NAME}.PerformanceMainActivity"

echo "=== WeChat Friends Circle Performance Test Launcher ==="
echo "Available commands:"
echo

# Function to launch activity
launch_activity() {
    local activity_type=$1
    local description=$2
    echo "Launching $description..."
    adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY" --es activity_type "$activity_type"
    if [ $? -eq 0 ]; then
        echo "✓ Successfully launched $description"
    else
        echo "✗ Failed to launch $description"
    fi
    echo
}

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "Error: adb command not found. Please make sure Android SDK is installed and adb is in your PATH."
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "Error: No Android device connected. Please connect a device and enable USB debugging."
    exit 1
fi

# Parse command line arguments
case "$1" in
    "minimal")
        launch_activity "minimal" "Minimal Load Activity (lightest case)"
        ;;
    "light")
        launch_activity "light" "Light Load Activity"
        ;;
    "medium")
        launch_activity "medium" "Medium Load Activity"
        ;;
    "heavy")
        launch_activity "heavy" "Heavy Load Activity"
        ;;
    "light_between_frames")
        launch_activity "light_between_frames" "Light Load Between Frames Activity"
        ;;
    "medium_between_frames")
        launch_activity "medium_between_frames" "Medium Load Between Frames Activity"
        ;;
    "heavy_between_frames")
        launch_activity "heavy_between_frames" "Heavy Load Between Frames Activity"
        ;;
    "light_mixed")
        launch_activity "light_mixed" "Light Mixed Load Activity (light doFrame + light between frames)"
        ;;
    "medium_mixed")
        launch_activity "medium_mixed" "Medium Mixed Load Activity (medium doFrame + medium between frames)"
        ;;
    "heavy_mixed")
        launch_activity "heavy_mixed" "Heavy Mixed Load Activity (heavy doFrame + heavy between frames)"
        ;;
    "help"|"-h"|"--help"|"")
        echo "Usage: $0 <load_type>"
        echo
        echo "Available load types:"
        echo "  minimal                   - Minimal load (lightest case, no computational load)"
        echo "  light                     - Light load (small computational load in doFrame)"
        echo "  medium                    - Medium load (medium computational load in doFrame)"
        echo "  heavy                     - Heavy load (high computational load in doFrame)"
        echo "  light_between_frames      - Light load between doFrame calls"
        echo "  medium_between_frames     - Medium load between doFrame calls"
        echo "  heavy_between_frames      - Heavy load between doFrame calls"
        echo "  light_mixed               - Light mixed load (light doFrame + light between frames)"
        echo "  medium_mixed              - Medium mixed load (medium doFrame + medium between frames)"
        echo "  heavy_mixed               - Heavy mixed load (heavy doFrame + heavy between frames)"
        echo
        echo "Examples:"
        echo "  $0 minimal                # Launch minimal load test"
        echo "  $0 heavy                  # Launch heavy load test"
        echo "  $0 light_mixed            # Launch light mixed load test"
        echo "  $0 medium_mixed           # Launch medium mixed load test"
        echo "  $0 heavy_mixed            # Launch heavy mixed load test"
        echo
        echo "Note: For game performance tests, use ./launch_game.sh instead"
        echo
        echo "Direct adb commands (for CI usage):"
        echo "  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY --es activity_type minimal"
        echo "  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY --es activity_type heavy"
        echo "  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY --es activity_type light_mixed"
        echo "  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY --es activity_type medium_mixed"
        echo "  adb shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY --es activity_type heavy_mixed"
        ;;
    *)
        echo "Error: Unknown load type '$1'"
        echo "Run '$0 help' to see available options."
        exit 1
        ;;
esac