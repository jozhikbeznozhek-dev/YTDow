# YTDow — Video Downloader

Кроссплатформенный загрузчик видео с YouTube, VK и других сайтов.

## 📥 Скачать

| Платформа | Файл |
|-----------|------|
| **macOS** | [YTDow.app](https://github.com/jozhikbeznozhek-dev/YTDow/releases) |
| **Android** | [YTDow.apk](https://github.com/jozhikbeznozhek-dev/YTDow/releases) |

## Возможности

- ⬇ Скачивание видео (MP4) и аудио (MP3)
- 📊 Расчёт размера по качеству перед загрузкой
- ⚡ Параллельные загрузки (до 3 одновременно)
- 📦 Пакетная загрузка (ссылки через запятую)
- 📋 История скачанного с возможностью открыть/удалить
- 🔄 Проверка обновлений через GitHub Releases

## Сборка

### Android
```bash
cd android
./gradlew assembleDebug
```

### macOS
```bash
cd desktop
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt pyinstaller
pyinstaller build.spec
# .app в dist/
```

## Технологии

- **Android:** Kotlin + WebView + youtubedl-android
- **macOS:** Python + PySide6 + yt-dlp

## Лицензия

MIT
