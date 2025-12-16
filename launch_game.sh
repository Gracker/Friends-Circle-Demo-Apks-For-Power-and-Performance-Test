#!/bin/bash

# Game Performance Test Launcher Script
# This script provides easy commands to launch different game load types via adb

GAME_PACKAGE_NAME_BASE="com.example.launch.game"

echo "=== Game Performance Test Launcher ==="
echo "Available commands:"
echo

# Function to launch game activity
launch_game() {
    local load_type=$1
    local description=$2
    local package_name="${GAME_PACKAGE_NAME_BASE}.${load_type}"

    echo "Launching $description..."
    echo "Package: $package_name"

    # Try to stop the app first
    adb shell am force-stop "$package_name" 2>/dev/null || true

    # Launch the app
    adb shell monkey -p "$package_name" -c android.intent.category.LAUNCHER 1

    if [ $? -eq 0 ]; then
        echo "✓ Successfully launched $description"
    else
        echo "✗ Failed to launch $description"
        echo "  Make sure the APK is installed: adb install -r launch-game/build/outputs/apk/${load_type}/debug/launch-game-${load_type}-debug.apk"
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
    "light")
        launch_game "light" "Game Light Load (3 seconds)"
        ;;
    "medium")
        launch_game "medium" "Game Medium Load (10 seconds with video)"
        ;;
    "heavy")
        launch_game "heavy" "Game Heavy Load (20 seconds with video and resources)"
        ;;
    "all")
        echo "Launching all game variants sequentially..."
        launch_game "light" "Game Light Load (3 seconds)"
        sleep 1
        launch_game "medium" "Game Medium Load (10 seconds with video)"
        sleep 1
        launch_game "heavy" "Game Heavy Load (20 seconds with video and resources)"
        ;;
    "build")
        echo "Building all game variants..."
        ./gradlew :launch-game:assembleLightDebug :launch-game:assembleMediumDebug :launch-game:assembleHeavyDebug
        ;;
    "install")
        echo "Installing all game variants..."
        echo "Installing light variant..."
        adb install -r launch-game/build/outputs/apk/light/debug/launch-game-light-debug.apk || echo "Failed to install light variant"
        echo "Installing medium variant..."
        adb install -r launch-game/build/outputs/apk/medium/debug/launch-game-medium-debug.apk || echo "Failed to install medium variant"
        echo "Installing heavy variant..."
        adb install -r launch-game/build/outputs/apk/heavy/debug/launch-game-heavy-debug.apk || echo "Failed to install heavy variant"
        ;;
    "help"|"-h"|"--help"|"")
        echo "Usage: $0 <command>"
        echo
        echo "Commands:"
        echo "  light                     - Launch light load game (3 seconds)"
        echo "  medium                    - Launch medium load game (10 seconds with video)"
        echo "  heavy                     - Launch heavy load game (20 seconds with video and resources)"
        echo "  all                       - Launch all game variants sequentially"
        echo "  build                     - Build all game variants"
        echo "  install                   - Install all game variants"
        echo
        echo "Examples:"
        echo "  $0 light                  # Launch light load game"
        echo "  $0 medium                 # Launch medium load game"
        echo "  $0 heavy                  # Launch heavy load game"
        echo "  $0 build                  # Build all variants"
        echo "  $0 install                # Install all variants"
        echo
        echo "Package names:"
        echo "  Light:  com.example.launch.game.light"
        echo "  Medium: com.example.launch.game.medium"
        echo "  Heavy: com.example.launch.game.heavy"
        echo
        echo "Note: Each variant is built as a separate APK with different package IDs."
        ;;
    *)
        echo "Error: Unknown command '$1'"
        echo "Run '$0 help' to see available options."
        exit 1
        ;;
esac