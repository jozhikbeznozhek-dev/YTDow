# YTDow

YTDow — Android- и macOS-приложение для загрузки видео и аудио через `yt-dlp`.
Пользователь отвечает за право загружать и использовать выбранный материал.

Подготавливаемая версия Android: **2.3.0**. Постоянный application id:
`com.jozhikbeznozhek.ytdow`. Android-релизы предназначены для устройств
`arm64-v8a` с Android 7.0 (API 24) или новее.

## Возможности

- MP4 и MP3, выбор качества и языка аудиодорожки;
- параллельные загрузки с прогрессом, отменой и историей попыток;
- сохранение Android-файлов через MediaStore в `Downloads/YTDow`;
- проверка и установка подписанных обновлений из GitHub Releases;
- локальный интерфейс без аналитики и рекламных SDK;
- desktop-клиент на Python, PySide6 и `yt-dlp`.

## Устройство проекта

| Каталог | Назначение |
|---|---|
| `app` | Android Activity, WebView UI и foreground download service |
| `data` | Репозитории, локальная история и настройки |
| `domain` | Модели, use cases и контракты очереди |
| `core` | Общие Kotlin-интерфейсы |
| `desktop` | macOS-клиент |

Android использует Kotlin, Coroutines, Hilt, Room, AndroidX WebKit и
`youtubedl-android`. WebView загружает только встроенные ресурсы через
`appassets.androidplatform.net`; загрузки и история остаются на устройстве.

## Проверка Android

Требуются JDK 17, Android SDK Platform 36 и Build Tools 35.0.0.

```bash
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew --no-daemon --dependency-verification strict test lint assembleRelease cyclonedxBom
```

Обычная `assembleRelease` создает неподписанный артефакт для проверки. Для
публикуемой сборки задайте все четыре переменные и используйте обязательный
production gate:

```bash
export YTDOW_KEYSTORE=/secure/path/ytdow-release.p12
export YTDOW_STORE_PASSWORD='...'
export YTDOW_KEY_ALIAS='...'
export YTDOW_KEY_PASSWORD='...'
./gradlew --no-daemon :app:productionRelease
```

Ключ нельзя хранить в репозитории. Workflow релиза восстанавливает его из
GitHub Actions Secrets, проверяет APK через `apksigner`, выпускает SBOM,
контрольные суммы и provenance attestation.

## Проверка desktop

```bash
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install --require-hashes -r desktop/requirements-dev.lock.txt
PYTHONPATH=desktop QT_QPA_PLATFORM=offscreen python -m pytest -q desktop/tests
python -m compileall -q desktop/hermes_downloader desktop/main.py
python -m pip_audit
```

Сборка macOS:

```bash
cd desktop
pyinstaller --clean --noconfirm YTDow.spec
```

Для подписанной сборки задаются `YTDOW_CODESIGN_IDENTITY` и
`YTDOW_ENTITLEMENTS`; notarization выполняется в защищенной release-среде.

## Безопасность и приватность

Политика данных описана в [PRIVACY.md](PRIVACY.md), процесс сообщения об
уязвимостях — в [SECURITY.md](SECURITY.md), сторонние компоненты — в
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Gradle dependency locking,
SHA-256 verification metadata, Dependabot, OSV Scanner и `pip-audit` включены
в CI.

## Лицензия

Copyright (C) 2026 YTDow contributors.

Проект распространяется по GNU General Public License v3.0 only; см.
[LICENSE](LICENSE). Выбор GPL-3.0 обеспечивает совместимость с используемым
`youtubedl-android`, который опубликован под GPL-3.0.
