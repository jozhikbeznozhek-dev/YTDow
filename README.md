# YTDow — YouTube Video Downloader for Android

Android-приложение для скачивания видео и аудио с YouTube, VK и других сайтов.

## Возможности

- ⬇ Скачивание видео (MP4) и аудио (MP3)
- 📊 Расчёт размера по качеству перед загрузкой
- ⚡ Параллельные загрузки (до 3 одновременно)
- 📦 Пакетная загрузка (ссылки через запятую)
- 📋 История скачанного с возможностью открыть/удалить
- 🔄 Проверка обновлений через GitHub Releases

## Скриншоты

<p align="center">
  <em>Главная · Загрузка · Библиотека · Настройки</em>
</p>

## Установка

Скачай APK из [Releases](https://github.com/jozhikbeznozhek-dev/YTDow/releases) и установи.

## Сборка из исходников

```bash
# Клонируй репозиторий
git clone https://github.com/jozhikbeznozhek-dev/YTDow.git
cd YTDow

# Открой в Android Studio или собери через Gradle
./gradlew assembleDebug
```

Требования:
- Android SDK 34
- JDK 17
- Kotlin 1.9

## Технологии

- Kotlin + WebView (HTML/CSS/JS)
- [youtubedl-android](https://github.com/junkfood02/youtubedl-android)
- yt-dlp
- FFmpeg

## Лицензия

MIT
