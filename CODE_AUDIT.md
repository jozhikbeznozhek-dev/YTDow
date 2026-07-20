# Code Audit Report — YTDow v2.1.0

**Дата:** 20 июля 2026  
**Файлов проверено:** 22 (.kt)  
**Модулей:** 4 (app, data, domain, core)

---

## 🔴 Критические (Crash / Data Loss / Memory Leak)

### C1. Утечка MainViewModel — CoroutineScope никогда не отменяется

**Файл:** `MainViewModel.kt:12`  
```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```
Scope создаётся в конструкторе и **никогда не отменяется**. MainViewModel живёт вечно — при смене конфигурации или уничтожении Activity корутины продолжают работать, удерживая ссылки на старый контекст.

**Fix:** Добавить `fun onCleared() { scope.cancel() }` и вызывать при `onDestroy()`.

---

### C2. Утечка DownloadRepositoryImpl — CoroutineScope никогда не отменяется

**Файл:** `DownloadRepositoryImpl.kt:30`  
```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```
Та же проблема. Scope создаётся ServiceLocator'ом через `by lazy` и живёт до смерти процесса.

**Fix:** Добавить `fun destroy()` и вызывать при остановке приложения.

---

### C3. Утечка потока в автообновлении yt-dlp

**Файл:** `MainActivity.kt:75-83`  
```kotlin
thread(name = "ytdlp-update") {
    ytdlp.init(this@MainActivity)  // удерживает Activity
    ...
}
```
`kotlin.concurrent.thread` создаёт daemon-поток. Если Activity уничтожена во время обновления:
- Поток продолжает работу (утечка)
- Вызов `this@MainActivity` после `onDestroy()` → потенциальный краш

**Fix:** Заменить на `lifecycleScope.launch(Dispatchers.IO)` или использовать `viewModelScope`.

---

### C4. QueueManagerImpl: NPE при использовании `!!`

**Файл:** `QueueManagerImpl.kt:97,120,129`  
```kotlin
tasks[taskId] = tasks[taskId]!!.copy(...)
```
Если `taskId` отсутствует в `tasks` (например, отменён между `nextDownloadTask()` и `onTaskCompleted()`), приложение упадёт с `NullPointerException`.

**Fix:** Заменить `!!` на `?.let` с безопасной обработкой.

---

### C5. MainActivity: savePath сбрасывается при каждом запуске

**Файл:** `MainActivity.kt:68-70`  
```kotlin
savePath = defaultSaveDir.absolutePath
prefs.edit().putString("save_path", savePath).apply()
```
Всегда перезаписывает путь сохранения на дефолтный. Пользовательский путь, выбранный через настройки, **теряется при перезапуске приложения**.

**Fix:** Загружать сохранённый путь из prefs, а не перезаписывать его.

---

## 🟠 Высокие (Race Condition / UX / Безопасность)

### H1. Дублирование логики скачивания

**Файлы:** `DownloadService.kt` и `DownloadRepositoryImpl.kt`  
Оба содержат идентичную логику:
- Формирование `YoutubeDLRequest` (опции, фильтры)
- Копирование в публичную папку через MediaStore
- Сохранение в историю

**Fix:** Оставить одну реализацию в `DownloadRepositoryImpl`. `DownloadService` использовать только как обёртку foreground-сервиса.

---

### H2. WebView: `allowFileAccess = true` — риск чтения локальных файлов

**Файл:** `MainActivity.kt:88`  
```kotlin
settings.allowFileAccess = true
```
Разрешает доступ к `file://` URL. Если вредоносный скрипт попадёт в WebView, он сможет читать локальные файлы через JS-интерфейс.

**Fix:** Установить `allowFileAccess = false`. Приложение загружает только `file:///android_asset/index.html` — это работает без `allowFileAccess`.

---

### H3. `escJs()` неполное экранирование

**Файл:** `MainActivity.kt:140-145`  
```kotlin
return s.replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\n", "\\n")
    .replace("\r", "")
```
Не экранирует: `` ` ``, `$`, `"`, `<`, `>`. Имена файлов с бэктиками или `$` могут сломать JavaScript.

**Fix:** Добавить `JSON.quote()` или экранировать `"`, `` ` ``, `$`.

---

### H4. DownloadService: `START_NOT_STICKY` — потеря загрузок при kill

**Файл:** `DownloadService.kt:158`  
```kotlin
return START_NOT_STICKY
```
Если система убьёт сервис при нехватке памяти, **загрузка пропадёт без возможности восстановления**.

**Fix:** `START_REDELIVER_INTENT` — система перезапустит сервис и доставит Intent заново.

---

### H5. UpdateRepositoryImpl: хардкод текущей версии

**Файл:** `UpdateRepositoryImpl.kt:26,29`  
```kotlin
UpdateInfo(latest, downloadUrl, "2.0.0")
```
Жёстко зашита версия "2.0.0" вместо использования `BuildConfig.VERSION_NAME`.

**Fix:** Инжектить `BuildConfig.VERSION_NAME` или передавать как параметр.

---

## 🟡 Средние (Технический долг / Неочевидные баги)

### M1. SettingsRepositoryImpl: утечка SharedPreferences-листенера

**Файл:** `SettingsRepositoryImpl.kt:13-21`  
```kotlin
override val savePath: Flow<String> = callbackFlow {
    val listener = ...
    prefs.registerOnSharedPreferenceChangeListener(listener)
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
}
```
Если Flow-коллектор отменён НЕ через `awaitClose` (например, отмена родительского скоупа), `awaitClose` может не выполниться, и листенер НЕ отрегистрируется.

**Fix:** Использовать `channelFlow` с более надёжным `invokeOnClose`.

---

### M2. Жёсткие пути `/storage/emulated/0/Download`

**Файлы:** `DownloadService.kt:188`, `DownloadRepositoryImpl.kt:132,180`  
Путь хардкожен — не будет работать на устройствах с SD-картами или альтернативными путями хранения.

**Fix:** Использовать `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`.

---

### M3. QueueManagerImpl: `processQueue()` — пустой метод

**Файл:** `QueueManagerImpl.kt:145-150`  
Метод объявлен, вызывается из всех точек, но тело пустое. Название вводит в заблуждение.

**Fix:** Удалить или написать комментарий, объясняющий почему он пуст.

---

### M4. DownloadService: `active` не синхронизирован с `pool`

**Файл:** `DownloadService.kt:27-28`  
```kotlin
private val active = ConcurrentHashMap<String, Boolean>()
private val pool = Executors.newFixedThreadPool(3)
```
`active.put(tid, true)` на строке 62 происходит в главном потоке, а `active.remove(tid)` на 149 — в pool-потоке. Между ними может быть гонка при быстрой отмене.

**Fix:** Использовать `AtomicInteger` для счётчика активных задач вместо map.

---

### M5. DownloadRepositoryImpl: `!!` в cancelDownload

**Файл:** `DownloadRepositoryImpl.kt:141-145`  
```kotlin
YoutubeDL.getInstance().destroyProcessById(taskId)
```
Вызов yt-dlp из главного потока может заблокировать UI.

**Fix:** Обернуть в `scope.launch(Dispatchers.IO)`.

---

## 🟢 Низкие (Читаемость / Стандарты)

### L1. Неиспользуемый импорт `AtomicInteger`

**Файл:** `QueueManagerImpl.kt:11`  
`import java.util.concurrent.atomic.AtomicInteger` — нигде не используется.

**Fix:** Удалить.

---

### L2. `defaultPath()` дублируется

**Файлы:** `MainActivity.kt:68` и `DownloadRepositoryImpl.kt:191`  
Одинаковая логика в двух местах.

**Fix:** Вынести в `PathUtils.defaultSaveDir()`.

---

### L3. `"2.0.0"` хардкод версии в UpdateRepositoryImpl

Дублирует жалобу H5, но также и в `MainActivity.kt:289` (версия `"1.0.0"` для update-check).

**Fix:** Использовать `BuildConfig.VERSION_NAME` везде.

---

### L4. `DownloadService.onDestroy()` — нет try/catch

**Файл:** `DownloadService.kt:261-265`  
```kotlin
override fun onDestroy() {
    pool.shutdown()
    unregisterReceiver(cancelReceiver)  // может бросить IllegalArgumentException
    super.onDestroy()
}
```
Если receiver уже отрегистрирован, `unregisterReceiver` бросает исключение, и `pool.shutdown()` не вызывается.

**Fix:** `try { unregisterReceiver(...) } catch (_: Exception) {}`.

---

## 📊 Сводка

| Severity | Count | Ключевые |
|---|---|---|
| 🔴 Critical | 5 | NPE, memory leaks, savePath reset |
| 🟠 High | 5 | Duplicate code, WebView security, version hardcode |
| 🟡 Medium | 5 | Listener leak, hardcoded paths, empty method |
| 🟢 Low | 4 | Dead imports, duplicates, error handling |

**Рекомендуемый порядок исправления:**
1. **C5** (savePath сброс) — ломает UX прямо сейчас
2. **C1-C3** (scope leaks) — предотвращают OOM при длительном использовании
3. **C4** (NPE в QueueManager) — предотвращает краш
4. **H2-H3** (WebView security) — устраняет векторы атаки
5. **H1** (дублирование кода) — упрощает поддержку
6. **M1-M5** — технический долг
7. **L1-L4** — косметика
