# Hermes Downloader — Android: что доделать

## ✅ Готово

- Проект Android (Gradle + Kotlin): `~/Desktop/android 2/`
- WebView UI (тёмная тема 1:1 с десктопом)
- MainActivity + JS-мост (`WebAppInterface`)
- DownloadService (foreground, youtubedl-android, прогресс)
- AndroidManifest (INTERNET, STORAGE, FOREGROUND_SERVICE)
- ProGuard (JS-интерфейс защищён)
- Ресурсы (colors.xml, themes.xml, strings.xml)
- Иконка приложения (`mipmap-*`)
- Gradle wrapper (`gradlew`, `gradle/wrapper/*`)
- Android SDK Platform 34 установлен локально
- Debug APK собирается: `app/build/outputs/apk/debug/app-debug.apk`
- Progress push из `DownloadService` в WebView
- Scoped Storage: сохранение в app-specific Downloads
- Встроенный Android `yt-dlp`/Python runtime через `youtubedl-android`
- Встроенный FFmpeg для mp3 и merge video+audio
- Скрипт сборки `build_apk.sh`
- README с документацией

---

## 🔴 Нужно доделать

**Критично: ошибка yt-dlp (n-challenge → HTTP 403)**

Причина: бинарник `youtubedl-android` содержит старую версию yt-dlp (2025.11.12),
которая не умеет решать новый YouTube n-challenge.

**Исправление (Termux на устройстве):**
```bash
pkg install python ffmpeg
pip install --upgrade yt-dlp
cp $(which yt-dlp) /sdcard/yt-dlp
```

**На компьютере:**
```bash
adb pull /sdcard/yt-dlp app/src/main/assets/yt-dlp
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🟡 Опционально

- **Notification click** — при тапе на уведомление открывать приложение и показывать результат загрузки.
- **Deep Link** — ловить YouTube-ссылки из других приложений (intent-filter).
- **Тёмная/светлая тема** — сейчас только тёмная. Добавить авто-переключение по системной теме.

---

## 📊 Сравнение десктоп ↔ Android

| | Десктоп (PySide6) | Android (Kotlin + WebView) |
|---|---|---|
| yt-dlp | `import yt_dlp` | bundled youtubedl-android runtime |
| UI | QSS/Qt | HTML/CSS |
| Многопоточность | QThreadPool | Thread + ForegroundService |
| Файлы | `~/Downloads/` | Scoped Storage |
| ffmpeg | Системный brew | bundled ffmpeg |
| Сборка | PyInstaller | Gradle |
