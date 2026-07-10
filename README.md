# Hermes Downloader — Android

Десктопное приложение → Android (WebView-порт).
Интерфейс 1:1, используется yt-dlp ARM-бинарник.

## Архитектура

```
WebView (HTML/CSS/JS)  ←→  Kotlin Bridge (JavaScriptInterface)
       ↓                         ↓
  UI (тёмная тема)        MainActivity + DownloadService
                                 ↓
                          bundled yt-dlp runtime
                          (youtubedl-android)
                                 ↓
                          App-specific Downloads/HermesDownloader/
```

## Структура проекта

```
android2/
├── app/
│   ├── build.gradle.kts          # Зависимости: androidx, material
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions: INTERNET, STORAGE, FOREGROUND
│       ├── java/com/hermes/downloader/
│       │   ├── MainActivity.kt   # WebView shell + JS bridge
│       │   └── DownloadService.kt # Foreground download service
│       ├── assets/
│       │   └── index.html         # UI (тёмная тема, как на десктопе)
│       └── res/values/
│           ├── strings.xml
│           ├── colors.xml         # #1E1E1E, #007AFF, #2D2D2D
│           └── themes.xml         # Theme.AppCompat.NoActionBar
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── build_apk.sh                   # Скрипт сборки
```

## Сборка APK

### 1. Установи зависимости

- **Android Studio** (или SDK отдельно): CLI + JDK 17
- **ANDROID_HOME**: `export ANDROID_HOME=~/Library/Android/sdk`
- Gradle wrapper уже добавлен, отдельный системный `gradle` не нужен

### 2. Запусти сборку

```bash
cd ~/Desktop/android\ 2
chmod +x build_apk.sh
./build_apk.sh
```

Или через Android Studio: открой папку как проект → Build → Build APK.

### 3. Установи на устройство

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Как это работает

1. **MainActivity** создаёт WebView, загружает `index.html`
2. **WebAppInterface** (JS-bridge) пробрасывает методы в JS:
   - `Android.startDownload(url, format, quality, taskId)`
   - `Android.getDownloadDir()`
   - `Android.cancelDownload(taskId)`
3. **DownloadService** запускает bundled `yt-dlp` через `youtubedl-android`,
   получает callback прогресса и шлёт прогресс в WebView
4. **MainActivity** принимает внутренние broadcast-события сервиса и вызывает
   `evaluateJavascript("onProgress(...)")`

## Отличия от десктопной версии

| Десктоп (PySide6) | Android (WebView) |
|---|---|
| Python yt-dlp (import) | youtubedl-android bundled runtime |
| QThreadPool | Thread + ForegroundService |
| QSS (Qt Style Sheets) | CSS |
| QLineEdit/QComboBox | HTML input/select |
| Файловая система | App-specific external storage |
| ffmpeg | Системный ffmpeg | Bundled ffmpeg |

## Требования APK

- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- ABI: arm64-v8a
- Размер APK: зависит от bundled Python/yt-dlp/ffmpeg runtime
