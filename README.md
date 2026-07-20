# YTDow — Smart Video Downloader

Кроссплатформенный загрузчик видео с поддержкой YouTube и других платформ.  
Чистая архитектура, многомодульный Gradle-проект, автообновление yt-dlp.

## Стек

| Слой | Технологии |
|---|---|
| **Android UI** | WebView + Material Design (Compose-ready) |
| **Android Core** | Kotlin, Coroutines, Flow, Hilt DI |
| **Хранение** | Room Database (3 DAO, 3 Entity) |
| **Загрузки** | youtubedl-android + FFmpeg |
| **Архитектура** | Clean Architecture (4 Gradle-модуля) |
| **Тесты** | JUnit 4, MockK, Coroutines Test |
| **Desktop** | Python 3.12 + PySide6 + yt-dlp |

## Архитектура Android

```
:app        — Activity, WebView, UI (entry point)
:data       — Repository impl, Room DB, ServiceLocator
:domain     — Models, UseCases, Queue Manager (чистый Kotlin)
:core       — Logger interface (чистый Kotlin)
```

- **Нет циклических зависимостей**: `app → data → domain, core`
- **Plugin System**: `ServiceRegistry` + `VideoServiceProvider` — добавление новых платформ без правок ядра
- **Download State Machine**: 7 состояний с валидированными переходами
- **Queue Manager**: приоритеты, retry, pause/resume, ограничение параллельных задач
- **Logger**: интерфейс с возможностью подмены на Timber

## Возможности

- ⬇ MP4 (best / 720p / 1080p) и MP3 (192 kbps)
- 📊 Расчёт размера до загрузки через `getInfo()`
- 📦 Пакетная загрузка — ссылки через запятую
- 🔄 Автообновление yt-dlp раз в 7 дней
- 📋 История загрузок с открытием/удалением
- 🌐 Выбор аудиодорожки (язык)
- 📁 Настраиваемая папка сохранения
- 🔔 Проверка обновлений через GitHub Releases API

## Быстрый старт

### Android

```bash
# Сборка (требуется JDK 17 + Android SDK 34)
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/sdk
./gradlew :app:packageDebug

# Установка на эмулятор/устройство
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Тесты
./gradlew test
```

### macOS Desktop

```bash
cd desktop
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt pyinstaller
pyinstaller build.spec
open dist/YTDow.app
```

## Структура проекта

```
YTDow/
├── app/src/main/           # Android entry point
│   ├── assets/index.html   # WebView UI
│   └── java/.../
│       ├── MainActivity.kt
│       ├── DownloadService.kt
│       └── presentation/   # ViewModel
├── data/src/main/           # Реализации
│   └── java/.../
│       ├── repository/      # DownloadRepositoryImpl
│       ├── local/           # Room Database
│       └── queue/           # QueueManagerImpl
├── domain/src/main/         # Бизнес-логика
│   └── java/.../
│       ├── model/           # Сущности
│       ├── repository/      # Интерфейсы
│       ├── usecase/         # Use Cases
│       ├── queue/           # Queue Manager
│       └── plugin/          # Plugin System
├── core/src/main/           # Утилиты (чистый Kotlin)
├── desktop/                 # macOS версия (Python)
└── app/src/test/            # Unit-тесты (18 тестов)
```

## Лицензия

MIT
