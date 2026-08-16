package com.hermes.downloader

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yausername.ffmpeg.FFmpeg
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.hermes.downloader.domain.model.DownloadHistoryEntry
import com.hermes.downloader.domain.repository.DownloadRepository
import com.hermes.downloader.domain.storage.DownloadStagingDirectory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var downloadRepository: DownloadRepository

    private val active = ConcurrentHashMap<String, Boolean>()
    private val lastPersistedProgressAt = ConcurrentHashMap<String, Long>()
    private val pool = Executors.newFixedThreadPool(3)
    @Volatile private var initDone = false

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, i: Intent?) {
            val tid = i?.getStringExtra(EXTRA_TASK_ID) ?: return
            YoutubeDL.getInstance().destroyProcessById(tid)
            active.remove(tid)
            AttemptHistoryStore.cancel(this@DownloadService, tid)
            if (active.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                notifySummary()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, cancelReceiver,
            IntentFilter(ACTION_CANCEL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val fmt = intent.getStringExtra(EXTRA_FORMAT) ?: "mp4"
        val qual = intent.getStringExtra(EXTRA_QUALITY) ?: "best"
        val lang = intent.getStringExtra(EXTRA_AUDIO_LANG) ?: ""
        val tid = intent.getStringExtra(EXTRA_TASK_ID) ?: "unknown"
        active[tid] = true

        // Синхронизированный первый запуск foreground
        if (!initDone) {
            synchronized(this) {
                if (!initDone) {
                    startForeground(1, n("Загрузка...", 0))
                    initDone = true
                }
            }
        }
        notifySummary()

        pool.execute {
            var stagingDirectory: File? = null
            var title = ""
            try {
                val stagingRoot = DownloadStagingDirectory.from(filesDir).apply {
                    check(mkdirs() || isDirectory) { "Unable to create download staging root" }
                }
                stagingDirectory = File(stagingRoot, tid.replace(Regex("[^A-Za-z0-9._-]"), "_")).apply {
                    deleteRecursively()
                    check(mkdirs() || isDirectory) { "Unable to create task staging directory" }
                }
                YoutubeDL.getInstance().init(this@DownloadService)
                FFmpeg.getInstance().init(this@DownloadService)
                YtDlpUpdateCoordinator.updateIfDue(this@DownloadService)

                val result = try {
                    executeDownload(url, fmt, qual, lang, tid, stagingDirectory, forceIpv4 = false)
                } catch (firstError: Exception) {
                    if (!active.containsKey(tid) || !shouldRetryWithIpv4(firstError)) throw firstError
                    Log.w("YTDow", "Default network route failed; retrying task $tid over IPv4")
                    stagingDirectory.deleteRecursively()
                    check(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
                        "Unable to reset task staging directory"
                    }
                    executeDownload(url, fmt, qual, lang, tid, stagingDirectory, forceIpv4 = true)
                }
                title = result.title

                if (active.containsKey(tid)) {
                    val sourceFile = File(result.filePath).canonicalFile
                    check(DownloadStagingDirectory.contains(stagingDirectory, sourceFile)) {
                        "Загрузчик вернул файл вне приватного каталога"
                    }
                    check(sourceFile.isFile) { "Загруженный файл не найден" }
                    val sourceSizeBytes = sourceFile.length()
                    val finalPath = copyToPublicDownloads(sourceFile.absolutePath)
                        ?: throw IllegalStateException("Не удалось опубликовать загрузку в Downloads")
                    runBlocking {
                        downloadRepository.addToHistory(
                            DownloadHistoryEntry(
                                url = url,
                                title = title.ifBlank { sourceFile.nameWithoutExtension },
                                format = fmt,
                                quality = qual,
                                filePath = finalPath,
                                sizeBytes = sourceSizeBytes
                            )
                        )
                    }
                    AttemptHistoryStore.complete(
                        this@DownloadService, tid, url, title, fmt, qual, finalPath, sourceSizeBytes
                    )
                    sendComplete(tid, finalPath)
                }
            } catch (e: Exception) {
                if (active.containsKey(tid)) {
                    Log.e("YTDow", "Download failed for task $tid", e)
                    val message = DownloadFailureMessage.from(e.message)
                    AttemptHistoryStore.fail(this@DownloadService, tid, url, title, fmt, qual, message)
                    sendError(tid, message)
                }
            } finally {
                active.remove(tid)
                lastPersistedProgressAt.remove(tid)
                stagingDirectory?.deleteRecursively()
                if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notifySummary()
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun executeDownload(
        url: String,
        format: String,
        quality: String,
        audioLanguage: String,
        taskId: String,
        stagingDirectory: File,
        forceIpv4: Boolean
    ): DownloadResult {
        val request = YoutubeDLRequest(url).apply {
            addOption("-o", "${stagingDirectory.absolutePath}/%(title)s.%(ext)s")
            addOption("--no-playlist")
            addOption("--no-colors")
            addOption("--no-mtime")
            addOption("--no-keep-video")
            addOption("--socket-timeout", 20)
            if (forceIpv4) addOption("--force-ipv4")
            addOption("--print", "before_dl:${TITLE_PREFIX}%(title)s")
            addOption("--print", "after_move:${FILE_PREFIX}%(filepath)s")

            if (format == "mp3") {
                addOption("-f", "bestaudio/best")
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "192K")
            } else {
                addOption("--merge-output-format", "mp4")
                val languageFilter = audioLanguage.takeIf { it.isNotEmpty() }?.let { "[language=$it]" }.orEmpty()
                val selector = if (quality == "best") {
                    "bestvideo+bestaudio$languageFilter/best"
                } else {
                    "bestvideo[height<=${quality.removeSuffix("p")}]+bestaudio$languageFilter/best"
                }
                addOption("-f", selector)
            }
        }

        var filePath = ""
        var title = ""
        YoutubeDL.getInstance().execute(request, taskId) progressCallback@{ percent, _, line ->
            if (!active.containsKey(taskId)) return@progressCallback
            val output = line.trim()
            when {
                output.startsWith(FILE_PREFIX) -> filePath = output.removePrefix(FILE_PREFIX).trim()
                output.startsWith(TITLE_PREFIX) -> title = output.removePrefix(TITLE_PREFIX).trim()
            }

            val parsedPercent = Regex("""(\d+(?:\.\d+)?)%""").find(output)
                ?.groupValues?.get(1)?.toDoubleOrNull()
            val progress = when {
                percent > 0.0f -> percent.roundToInt()
                parsedPercent != null -> parsedPercent.roundToInt()
                else -> -1
            }.coerceIn(-1, 100)
            val speed = Regex("""at\s+(\S+)\s""").find(output)?.groupValues?.get(1).orEmpty()
            val eta = Regex("""ETA\s+(\S+)""").find(output)?.groupValues?.get(1).orEmpty()
            persistProgressIfDue(taskId, progress, speed, eta)
            sendProgress(taskId, progress, speed, eta)
        }

        if (filePath.isBlank()) {
            filePath = stagingDirectory.walkTopDown()
                .filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                .maxByOrNull { it.lastModified() }
                ?.absolutePath
                .orEmpty()
        }
        return DownloadResult(filePath, title)
    }

    private fun shouldRetryWithIpv4(error: Throwable): Boolean {
        val message = generateSequence(error as Throwable?) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return IPV4_RETRY_MARKERS.any(message::contains)
    }

    private fun copyToPublicDownloads(srcPath: String): String? {
        if (srcPath.isEmpty() || !File(srcPath).exists()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyToScopedDownloads(srcPath)
        } else {
            copyToLegacyDownloads(srcPath)
        }
    }

    private fun copyToLegacyDownloads(srcPath: String): String? = try {
        val sourceFile = File(srcPath)
        @Suppress("DEPRECATION")
        val targetDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "YTDow"
        )
        check(targetDirectory.mkdirs() || targetDirectory.isDirectory) {
            "Не удалось создать папку Downloads/YTDow"
        }
        val targetFile = uniqueLegacyTarget(targetDirectory, sourceFile.name)
        sourceFile.copyTo(targetFile, overwrite = false)
        sourceFile.delete()
        targetFile.absolutePath
    } catch (exception: Exception) {
        Log.e("YTDow", "Unable to publish legacy download", exception)
        null
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun copyToScopedDownloads(srcPath: String): String? {
        var targetUri: Uri? = null
        return try {
            val sourceFile = File(srcPath)
            val fileName = sourceFile.name
            val mime = when {
                fileName.endsWith(".mp3") -> "audio/mpeg"
                fileName.endsWith(".mp4") -> "video/mp4"
                else -> "*/*"
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/YTDow")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val mediaUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            targetUri = mediaUri
            contentResolver.openOutputStream(mediaUri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            } ?: error("Не удалось открыть MediaStore для записи")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(contentResolver.update(mediaUri, values, null, null) == 1) {
                "Не удалось завершить запись MediaStore"
            }
            sourceFile.delete()
            return mediaUri.toString()
        } catch (exception: Exception) {
            targetUri?.let { contentResolver.delete(it, null, null) }
            Log.e("YTDow", "Unable to publish download", exception)
            null
        }
    }

    private fun notifySummary() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, n(if (active.size <= 1) "Загрузка..." else "Загрузок: ${active.size}", 0))
    }

    private fun persistProgressIfDue(taskId: String, percent: Int, speed: String, eta: String) {
        val now = SystemClock.elapsedRealtime()
        val previous = lastPersistedProgressAt[taskId]
        if (previous != null && now - previous < PROGRESS_PERSIST_INTERVAL_MS && percent != 100) return
        lastPersistedProgressAt[taskId] = now
        AttemptHistoryStore.progress(this, taskId, percent, speed, eta)
    }

    private fun n(t: String, p: Int) = NotificationCompat.Builder(this, "downloads")
        .setContentTitle("YTDow")
        .setContentText(t)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setProgress(100, p, p == 0)
        .setOngoing(true)
        .build()

    private fun sendProgress(tid: String, pct: Int, speed: String, eta: String) {
        sendBroadcast(Intent(ACTION_PROGRESS).apply {
            setPackage(packageName)
            putExtra(EXTRA_TASK_ID, tid)
            putExtra(EXTRA_PERCENT, pct)
            putExtra(EXTRA_SPEED, speed)
            putExtra(EXTRA_ETA, eta)
        })
    }

    private fun sendComplete(tid: String, fp: String) {
        sendBroadcast(Intent(ACTION_COMPLETE).apply {
            setPackage(packageName)
            putExtra(EXTRA_TASK_ID, tid)
            putExtra(EXTRA_FILE_PATH, fp)
        })
    }

    private fun sendError(tid: String, err: String) {
        sendBroadcast(Intent(ACTION_ERROR).apply {
            setPackage(packageName)
            putExtra(EXTRA_TASK_ID, tid)
            putExtra(EXTRA_ERROR, err)
        })
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        active.keys.toList().forEach { taskId ->
            YoutubeDL.getInstance().destroyProcessById(taskId)
            AttemptHistoryStore.timeout(this, taskId)
            sendError(taskId, "Система остановила длительную фоновую загрузку")
        }
        active.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onDestroy() {
        pool.shutdownNow()
        try { unregisterReceiver(cancelReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        private const val TITLE_PREFIX = "YTDOW_TITLE:"
        private const val FILE_PREFIX = "YTDOW_FILE:"
        private const val PROGRESS_PERSIST_INTERVAL_MS = 750L
        private val IPV4_RETRY_MARKERS = listOf(
            "network is unreachable",
            "no route to host",
            "timed out",
            "timeout",
            "connection reset",
            "temporary failure in name resolution"
        )
        const val ACTION_CANCEL = "com.hermes.downloader.CANCEL"
        const val ACTION_PROGRESS = "com.hermes.downloader.PROGRESS"
        const val ACTION_COMPLETE = "com.hermes.downloader.COMPLETE"
        const val ACTION_ERROR = "com.hermes.downloader.ERROR"
        const val EXTRA_URL = "url"
        const val EXTRA_FORMAT = "format"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_TASK_ID = "taskId"

        const val EXTRA_PERCENT = "percent"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_ETA = "eta"
        const val EXTRA_ERROR = "error"
        const val EXTRA_FILE_PATH = "filePath"
        const val EXTRA_AUDIO_LANG = "audioLang"
    }

    private fun uniqueLegacyTarget(directory: File, originalName: String): File {
        val requested = File(directory, originalName)
        if (!requested.exists()) return requested
        val extension = requested.extension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        val baseName = requested.name.removeSuffix(extension)
        for (suffix in 1..9999) {
            val candidate = File(directory, "$baseName ($suffix)$extension")
            if (!candidate.exists()) return candidate
        }
        error("Слишком много файлов с одинаковым названием")
    }

    private data class DownloadResult(val filePath: String, val title: String)
}
