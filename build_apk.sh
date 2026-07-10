#!/bin/bash
# YTDow — сборка APK для Android
# Требования: Android SDK + JDK 17 + Kotlin

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "=== YTDow — Android Build ==="
echo "Project: $PROJECT_DIR"

# 1. Проверяем окружение
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
        export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
    else
        echo "❌ Android SDK не найден."
        echo "   Укажи путь к SDK: export ANDROID_HOME=~/Library/Android/sdk"
        exit 1
    fi
fi

echo "✓ ANDROID_HOME: $ANDROID_HOME"

# 2. Gradle wrapper
if [ ! -x "$PROJECT_DIR/gradlew" ]; then
    echo "❌ gradlew не найден или не исполняемый."
    echo "   Создай wrapper: gradle wrapper --gradle-version 8.5"
    exit 1
fi

# 3. Сборка
echo ""
echo "=== Сборка APK ==="
"$PROJECT_DIR/gradlew" assembleDebug

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✓ APK собран: $APK_PATH"
    echo "  Размер: $(du -sh "$APK_PATH" | cut -f1)"
    echo ""
    echo "  Установить на устройство:"
    echo "    adb install \"$APK_PATH\""
else
    echo "❌ Сборка не удалась. Проверь логи выше."
    exit 1
fi
