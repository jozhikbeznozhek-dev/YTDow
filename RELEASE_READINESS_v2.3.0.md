# YTDow v2.3.0 — release readiness

Дата среза: 2026-08-10 01:22 UTC
Общий статус: **BLOCKED — тег и GitHub Release создавать нельзя**

## Резюме

Локальные Android, desktop и supply-chain проверки, которые можно выполнить в
данном окружении, завершены. Строгая Gradle dependency verification работает,
полный Android gate прошёл, unsigned и production-signed APK и валидный
CycloneDX SBOM созданы,
desktop-тесты и Linux smoke build прошли, известные package vulnerabilities и
секреты не обнаружены.

Публикация запрещена двумя независимыми Android-блокерами:

1. Ни одного физического Android-устройства не видно через `adb devices -l`.
2. Для native FFmpeg payload из `youtubedl-android:0.18.1` не подтверждён
   полный GPL Corresponding Source, воспроизводящий распространяемые бинарники.

Решение по несовместимой подписи принято 2026-08-16: v2.3.0 начинает новую
production signing identity без certificate rotation. Обновление поверх
debug-signed v2.2.x не поддерживается; требуется чистая установка. Публичные
файлы в `Downloads/YTDow` сохраняются, внутренняя история и настройки
сбрасываются. Release notes и signing runbook фиксируют этот разрыв явно.

Дополнительно официальный desktop target заявлен как macOS, однако нативная
production-signed/notarized macOS-сборка в этой сессии не создавалась.

## Исходное состояние и Git

| Поле | Значение |
|---|---|
| Репозиторий | `git@github.com:jozhikbeznozhek-dev/YTDow.git` |
| Исходный commit | `e99d198d45d10682df7e47cd22e5cfb8b7fea20c` (`v2.2.3`, `origin/main`) |
| Release branch | `release/v2.3.0` |
| Проверенный material candidate | `4c83971bdb02e1eb5983d0e4a017cd8f3a56407b` |
| Финальный release/tag commit | отсутствует — публикация заблокирована |
| Локальный/remote tag `v2.3.0` | отсутствует |
| GitHub Release `v2.3.0` | отсутствует (`gh release view`: `release not found`) |
| GitHub Actions secret names | все пять `YTDOW_*` signing secrets настроены 2026-08-16; значения не читались и не логировались |

До переноса изменений исходный release worktree не содержал пользовательских
изменений. Другие worktree, в том числе основной worktree на отдельном SHA,
не изменялись и не очищались.

### Контрольная точка `lfg-158dde`

`lfg-158dde` найден как worktree
`/home/mikhail/lfg-worktrees/lfg-158dde`, ветка `session_lfg-158dde`, HEAD
`e99d198`. Это не отдельный commit: контрольная точка содержала 25 изменённых
tracked-файлов и 21 содержательный untracked-файл. Перед переносом были
просмотрены status, список файлов и diff. Перенесены исходники, тесты,
конфигурация и документы; build artifacts, caches и локальные настройки не
переносились. Изменения были затем дополнительно исправлены и разложены по
логическим коммитам, слепой `cherry-pick` не применялся.

## Окружение

| Инструмент | Версия |
|---|---|
| ОС | Ubuntu 24.04.4 LTS, Linux 6.8.0-137-generic, x86_64 |
| JDK | Eclipse Temurin 17.0.19+10 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin plugin проекта | 2.3.21 |
| Android SDK | Platform 36; Build Tools 35.0.0; Platform Tools 37.0.1 |
| Python | 3.12.13, изолированная `.venv` |
| pip | 26.2.1 |
| pytest / pytest-qt | 9.1.1 / 4.5.0 |
| pip-audit | 2.10.1 |
| PyInstaller | 6.22.0 |
| CycloneDX Gradle plugin / CLI | 3.4.0 / 0.33.1 |
| OSV Scanner | 2.5.0 |
| actionlint | 1.7.12 |
| gitleaks | 8.30.1 |
| GitHub CLI | 2.45.0 |

Qt runtime libraries для Linux были распакованы в изолированный каталог
`/tmp/ytdow-qt-runtime-root`; глобальная Python-установка не изменялась.

## Gate matrix

| Gate | Статус | Доказательство / замечание |
|---|---|---|
| Preflight, refs, worktrees, tags | PASS | base/tag/remote/worktrees проверены; `v2.3.0` отсутствует |
| Осознанный перенос `lfg-158dde` | PASS | перенесены только source/config/docs; затем review и тесты |
| Gradle Wrapper integrity | PASS | официальный wrapper JAR и distribution checksum |
| Strict dependency verification | PASS | полный gate с `--dependency-verification strict`, exit 0 |
| Dependency locks | PASS | lockfiles для четырёх Gradle-модулей; hash-locked Python runtime/dev locks |
| Android unit tests | PASS | debug 33/33 и release 33/33; всего 66, 0 skipped/failures/errors |
| Android lint | PASS | exit 0, 0 errors, 28 рассмотренных warnings |
| `assembleRelease` | PASS | unsigned minified APK создан и zipaligned |
| CycloneDX SBOM | PASS | JSON и XML валидны по CycloneDX 1.6; 132 компонента |
| Runtime vulnerability scan | PASS | OSV: 132 Android + 10 Python packages, `No issues found` |
| Desktop pytest | PASS | 9/9 |
| Desktop compileall | PASS | только `desktop/hermes_downloader` и `desktop/main.py` |
| Desktop pip-audit | PASS | `No known vulnerabilities found` |
| PyInstaller Linux smoke | PASS (не release target) | bundle стартует и жив 8 секунд; resources присутствуют |
| Нативный macOS release | BLOCKED | Linux не является кросс-компилятором; native CI ещё не исполнялся, production signing/notarization отсутствуют |
| Production Android signing | PASS | новый RSA-4096 key; `:app:productionRelease` exit 0; v2/v3 signature и fingerprint проверены |
| Certificate continuity | DECIDED / CLEAN INSTALL | новый production certificate без rotation; установка поверх 2.2.x намеренно не поддерживается |
| Физическое Android-устройство | BLOCKED | `adb devices -l`: пустой список |
| GPL Corresponding Source | BLOCKED | недостаточно данных для точного native FFmpeg build |
| CI/workflow static review | PASS | `actionlint` exit 0; permissions минимизированы; Actions SHA-pinned |
| CI remote execution | NOT RUN | release-ветка не отправлялась из-за блокеров |
| Secret scan | PASS | gitleaks: 39 commits и 7.76 MB source snapshot, 0 leaks |
| Tag / GitHub Release | BLOCKED | намеренно не созданы |

## Dependency verification и Wrapper

Первичный строгий запуск воспроизвёл непосредственный блокер: Gradle сообщил
`Dependency verification failed` для Android lint/tooling artifacts, которых
не было в `gradle/verification-metadata.xml`. Через документированный режим
записи metadata добавлены только 26 фактически разрешённых SHA-256 записей;
необычные новые репозитории, wildcard trust и ignored artifacts не добавлялись.

Также обнаружено несоответствие: wrapper properties запрашивал Gradle 8.13, а
`gradle-wrapper.jar` происходил от Gradle 8.5. Wrapper пересоздан задачей Gradle
8.13. Проверенные значения:

- `gradle-wrapper.jar` SHA-256:
  `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`;
- `gradle-8.13-bin.zip` `distributionSha256Sum`:
  `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.

Metadata проверяет SHA-256 для artifacts и metadata в строгом режиме. PGP не
включён, потому что checksum coverage полная; verification не переведена в
lenient/off. Репозитории ограничены Google Maven, официальным Google mirror
Maven Central, Maven Central и Gradle Plugin Portal для plugins. CI выполняет
SHA-pinned `gradle/actions/wrapper-validation`.

## Android gate

Финальная команда после последнего изменения Android-кода:

```text
ANDROID_HOME=... ANDROID_SDK_ROOT=... ./gradlew --no-daemon \
  --dependency-verification strict clean test lint assembleRelease cyclonedxBom
```

Результат: exit 0, `BUILD SUCCESSFUL in 8m 5s`, 221 actionable tasks
(209 executed, 12 up-to-date).

### Тесты

- `testDebugUnitTest`: 33 tests, 0 skipped, 0 failures, 0 errors;
- `testReleaseUnitTest`: 33 tests, 0 skipped, 0 failures, 0 errors.

Прежнее число 58 не переиспользовано: финальное состояние прогнано заново и
содержит 66 variant-tests благодаря новым URL/staging policy tests.

### Lint и build warnings

Lint: 0 errors, 28 warnings. Категории: доступные обновления Gradle/AGP и
dependencies, KAPT→KSP, ChromeOS x86_64 ABI, форма launcher icon,
неиспользуемые colors и предложения KTX. Они не скрыты baseline или
suppression. Arm64-only является заявленным ограничением продукта.

Оставшиеся build warnings:

- Android tool parser понимает SDK XML до v3, установленный SDK содержит v4;
- CycloneDX 3.4.0 разрешает runtime configuration на configuration phase и
  мутирует `archives`, что станет несовместимо с Gradle 9;
- KAPT unit-test tasks получают Hilt options без активного processor;
- AGP не может strip заранее собранные `libffmpeg`, `libffprobe`, `libpython`
  и `libqjs`, поэтому упаковывает их как есть.

CycloneDX 3.4.0 был актуальной проверенной версией plugin. Эти warnings не
привели к ошибкам текущего Gradle 8.13, но native-library warning связан с
отдельным GPL blocker.

### Unsigned APK candidate

Путь: `app/build/outputs/apk/release/app-release-unsigned.apk`

- package: `com.jozhikbeznozhek.ytdow`;
- versionName: `2.3.0`;
- versionCode: `9` (предыдущая официальная версия: 8);
- minSdk: 24; targetSdk: 36; ABI: arm64-v8a;
- `zipalign -c -v 4`: PASS;
- `apksigner verify`: ожидаемый FAIL (`Missing META-INF/MANIFEST.MF`) — это не
  production artifact;
- SHA-256:
  `a4acf6e2caa8b87ab088f4d7f9b8ed0c3e26b32b986df7b2472acac4cc35890f`.

В APK не найдены случайные `.env`, keystore, signing properties, Git metadata,
Python cache или dev requirements.

### Production-signed APK candidate

После принятия clean-install решения 2026-08-16 создан новый RSA-4096
production certificate и настроены все пять защищённых GitHub Actions Secrets.
Зашифрованный PKCS12 и recovery environment file хранятся вне репозитория в
пользовательском Syncthing storage с правами `600`; Syncthing подтвердил 100%
completion на Pixel 9 Pro. Значения секретов не выводились.

Команда `:app:productionRelease` завершилась успешно за 4m 11s. Проверено:

- package `com.jozhikbeznozhek.ytdow`, versionName `2.3.0`, versionCode `9`;
- ABI `arm64-v8a`, размер 62,606,066 bytes;
- v1 false, v2 true, v3 true; один signer; Debug DN отсутствует;
- certificate DN `CN=YTDow Production, OU=Release, O=YTDow`;
- certificate SHA-256:
  `5b88f4e377b1c6bd5e492886c442002b14f20c970bf9ea90d43076747a1c65c9`;
- APK SHA-256:
  `80343b5f4f51502fdba49f873ee94344ead9b07367ac0dd6f827cd70f6723d1f`;
- `zipalign -c -v 4`: PASS.

### SBOM

- CycloneDX 1.6 JSON: 132 components, 0 `unspecified`, project license
  `GPL-3.0-only`, schema validation PASS;
- JSON SHA-256:
  `738f427c71b260443146c11859aaf5eabd12e34d861d5e7af723dc135e04f219`;
- XML schema validation PASS;
- XML SHA-256:
  `aa72608f15c2ecacd0ccc46c520d237502a38e47d8bdf2be86fd16ef8cdd28c7`.

Четыре project components (`app`, `core`, `data`, `domain`) наследуют лицензию
root metadata и не имеют дублирующего component-level license поля.

## Desktop gate

Зависимости установлены в `.venv` из
`desktop/requirements-dev.lock.txt` с `--require-hashes`; отдельный runtime lock
содержит 10 packages, dev lock — 50 packages и platform markers.

Финальные результаты:

- `PYTHONPATH=desktop QT_QPA_PLATFORM=offscreen python -m pytest -q
  desktop/tests`: 9 passed;
- `python -m compileall -q desktop/hermes_downloader desktop/main.py`: exit 0;
- `python -m pip_audit`: exit 0, 0 known vulnerabilities;
- PyInstaller 6.22.0: exit 0;
- Linux offscreen smoke: `timeout 8s`, exit 124 и пустой stderr — процесс не
  завершился аварийно;
- packaged styles и `icon.icns` присутствуют;
- Linux executable SHA-256:
  `c6e192c4bbab8c5cad00484f41f719aaa13bf9f43de2c08074552720bbf9f649`.

Диагностический запуск `pytest` из корня без `PYTHONPATH` завершился exit 2
из-за невозможности импортировать `hermes_downloader`; запуск без
`QT_QPA_PLATFORM=offscreen` завершился exit 134 из-за отсутствия display.
README и CI задают оба необходимых условия, итоговый документированный запуск
PASS.

PyInstaller сообщил о недоступном optional Qt TIFF plugin (`libtiff.so.5`) и
optional yt-dlp integrations (`curl_cffi`, `yt_dlp_ejs`, browser/alternate
platform modules). Основной bundle и используемые ресурсы прошли smoke test.
Linux artifact не является доказательством готовности macOS.

## Production signing и continuity

Задача `:app:productionRelease` требует все signing credentials и останавливает
сборку до упаковки, если хотя бы одного значения или файла нет. Новый key
создан вне репозитория, защищён паролем, синхронизирован в одобренное
пользователем хранилище и настроен в GitHub Actions Secrets. Локальный
production gate и независимая проверка `apksigner` завершились успешно.

Референсный APK v2.2.3:

- APK SHA-256:
  `c8722707a102275561c7c8cb3659b005fa19bf72b3a7af62b5b28011153021a2`;
- v1: false; v2: true; v3: true;
- signer DN: `C=US, O=Android, CN=Android Debug`;
- certificate SHA-256:
  `e1a4446581ad68dee2fed4af2c15f8f80ceef54af3669c30743fdee6a3fbf17f`.

APK v2.2.0, v2.2.1, v2.2.2 и v2.2.3 имеют тот же fingerprint. Подпись новым
production certificate не обновится поверх этих APK, а старый Debug key не
принимается release policy. Принято решение о clean-install break: v2.3.0
подписывается новым production key без lineage. Старый Debug key запрещено
настраивать в production workflow.

Пользовательская миграция описана в `release-notes/v2.3.0.md`: перед удалением
2.2.x нужно проверить файлы в публичной папке `Downloads/YTDow`; после удаления
история и настройки приложения не восстанавливаются. Порядок создания,
резервирования и проверки нового ключа описан в `docs/ANDROID_SIGNING.md`.

Release workflow теперь передаёт signing secrets только шагам восстановления,
production build и fingerprint comparison. Сторонний OSV action запускается
до появления signing identity. Workflow отклоняет debug DN, требует v2/v3,
совпадение ожидаемого fingerprint, zipalign и versionCode > 8.

## Device-test matrix

`adb devices -l` вернул только заголовок и ни одного устройства. Эмулятор не
использовался как замена. Персональные данные отсутствуют.

| Сценарий | Статус |
|---|---|
| MP4 download | BLOCKED — нет физического устройства |
| MP3 download | BLOCKED — нет физического устройства |
| Cancel active download | BLOCKED |
| Retry after cancel/error | BLOCKED |
| Delete through MediaStore, verify file and UI | BLOCKED |
| History after success/cancel/retry/delete | BLOCKED |
| Trusted WebView navigation and Back | BLOCKED |
| External links and unexpected schemes | BLOCKED |
| Navigation bypass attempts | BLOCKED |
| Install v2.3.0 over v2.2.3 | BLOCKED — устройство; ожидаемый результат: Android отклоняет несовместимую подпись |
| Uninstall 2.2.3 → clean install v2.3.0 | BLOCKED — есть production-signed candidate, нет подключённого устройства |
| Update production v2.3.0 → higher versionCode | BLOCKED — есть production identity, нужен тестовый APK с higher versionCode и устройство |
| Clean install, first run, permissions, main functions | BLOCKED |
| Screenshots and redacted filtered logcat | BLOCKED |

Имеющийся Playwright screenshot проверяет только отображение встроенного HTML
на viewport Pixel 7 и не засчитывается как device-functional evidence.

## Security и privacy findings

Исправлено:

- WebView ограничен локальным `appassets` origin; file/content access и network
  loads отключены; внешние переходы требуют main-frame user gesture;
- неизвестные схемы и embedded URL credentials блокируются;
- параметры JavaScript bridge имеют allowlists;
- updater допускает только HTTPS GitHub release chain, ограничивает размер APK,
  package id, versionCode и сравнивает signer digests;
- download output проверяется как canonical child приватного staging-каталога;
- MediaStore publish использует `IS_PENDING` и очищает запись при любой ошибке;
- удаление ограничено MediaStore `Download/YTDow` или канонической legacy папкой;
- Android backup и device transfer для данных приложения запрещены;
- desktop URL нормализуется, история записывается атомарно, partial files
  очищаются только внутри download root;
- runtime/dev dependencies разделены и hash-locked;
- Actions закреплены полными commit SHA, permissions минимизированы;
- release workflow проверяет wrapper, строгую verification, tests, lint, SBOM,
  OSV, signing, zipalign, package/version и provenance.

Сканы:

- `pip-audit`: 0 findings;
- OSV shipped runtime: 0 findings;
- gitleaks Git history: 39 commits, 0 leaks;
- gitleaks tracked+untracked source snapshot: 7.76 MB, 0 leaks.

Рекурсивный скан всего рабочего каталога не используется как runtime gate:
Gradle metadata и dev-tool manifests описывают build/test tools, а PyInstaller
bundle содержит публичные upstream service constants из yt-dlp. Runtime gate
сканирует итоговый Android SBOM и Python runtime lock; Python environment
дополнительно проверяет `pip-audit`.

## GPL и сторонние лицензии

Проект теперь содержит полный GPL-3.0-only license и third-party notice. Это
исправляет прежнее утверждение README о MIT без соответствующего LICENSE и при
наличии GPL-linked Android dependency.

`youtubedl-android:0.18.1` соответствует upstream tag commit
`d725d5c9a18c3a99a13ee0308bf78275dc310760`. AAR включает native FFmpeg 7.1.1
и GPL-enabled libraries. Upstream `BUILD_FFMPEG.md` предлагает клонировать
Termux packages, но не фиксирует точный Termux commit, source revisions,
patches и полный build environment для опубликованного payload. Поэтому exact
Corresponding Source или корректное written offer сейчас предоставить нельзя.
Это юридический release blocker, а не информационное предупреждение.

Privacy Policy описывает локальную историю/настройки, MediaStore, сеть к
source/CDN и GitHub, отсутствие analytics/ads, файлы и backup policy. Security
Policy описывает приватное сообщение уязвимостей и фактические release gates.

## Generated files и hygiene

`git ls-files` не обнаружил отслеживаемых APK/AAB, Gradle/PyInstaller build или
dist, `.venv`, Python caches, IDE files, logs, local properties, signing files
или keystores. `.gitignore` покрывает эти категории. Ничего не удалялось
массовым clean/reset; существующие worktree и пользовательские изменения в них
сохранены.

## Локальные атомарные коммиты

| Commit | Содержание |
|---|---|
| `2aa6598` | verified Gradle wrapper/metadata/locks, v2.3.0 build config, SHA-pinned CI/release pipeline |
| `99241b1` | Android download/history/MediaStore/WebView/updater hardening и tests |
| `80602c5` | desktop lifecycle, history, URL policy, tests, hashes и PyInstaller spec |
| `4c83971` | README, GPL license, privacy/security/notices и changelog |

Коммит, содержащий этот отчёт, определяется командой
`git log -1 -- RELEASE_READINESS_v2.3.0.md`; он не меняет проверенный код или
артефакты.

## Следующие минимально необходимые действия

1. Перенести signing passwords из recovery environment file в отдельный
   password manager и сделать вторую проверенную зашифрованную резервную копию
   ключа вне текущего Syncthing storage.
2. Подключить физическое arm64 Android-устройство и пройти всю device matrix на
   production-signed candidate, включая ожидаемый отказ обновления 2.2.3,
   сохранность публичных файлов, clean install и последующее production update,
   с redacted screenshots/logcat.
3. Получить или воспроизводимо собрать полный GPL Corresponding Source для
   bundled native payload и приложить его/действительное written offer.
4. Выполнить native macOS build, tests, signing, notarization и smoke на
   поддерживаемом runner, если desktop artifact входит в v2.3.0.
5. После устранения блокеров повторить финальные Android/desktop/signing gates
   из точного release commit, сформировать `SHA256SUMS`, проверить SBOM, только
   затем создать annotated tag `v2.3.0`, push и GitHub Release.

Ссылки на tag и GitHub Release отсутствуют намеренно: обязательные gates имеют
статус BLOCKED.
