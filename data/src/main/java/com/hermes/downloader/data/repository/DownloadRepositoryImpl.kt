package com.hermes.downloader.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.hermes.downloader.data.ServiceLocator
import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.repository.DownloadRepository
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class DownloadRepositoryImpl(
    private val context: Context,
    private val prefs: SharedPreferences
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun destroy() { scope.cancel() }

    private val active = ConcurrentHashMap<String, Job>()
    private val _currentTasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val currentTasks: Flow<Map<String, DownloadTask>> = _currentTasks.asStateFlow()

    private val _taskEvents = MutableStateFlow<TaskEvent?>(null)
    val taskEvents: Flow<TaskEvent?> = _taskEvents.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())

    override fun getHistory(): Flow<List<DownloadHistoryEntry>> = _history.asStateFlow()

    override suspend fun addToHistory(entry: DownloadHistoryEntry) {
        withContext(Dispatchers.IO) {
            try {
                val hist = prefs.getString("download_history", "[]") ?: "[]"
                val arr = JSONArray(hist)
                arr.put(JSONObject().apply {
                    put("url", entry.url); put("title", entry.title)
                    put("format", entry.format); put("quality", entry.quality)
                    put("filePath", entry.filePath); put("sizeBytes", entry.sizeBytes)
                    put("time", entry.time)
                })
                val trimmed = JSONArray()
                for (i in maxOf(0, arr.length() - 50) until arr.length()) trimmed.put(arr[i])
                prefs.edit().putString("download_history", trimmed.toString()).apply()
            } catch (e: Exception) { ServiceLocator.logger.e("YTDow", "addToHistory", e) }
        }
    }

    override suspend fun removeFromHistory(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray(prefs.getString("download_history", "[]") ?: "[]")
                val filtered = JSONArray()
                for (i in 0 until arr.length()) {
                    if (arr.getJSONObject(i).optString("filePath") != filePath) filtered.put(arr[i])
                }
                prefs.edit().putString("download_history", filtered.toString()).apply()
            } catch (e: Exception) { ServiceLocator.logger.e("YTDow", "removeFromHistory", e) }
        }
    }

    override suspend fun clearHistory() {
        prefs.edit().putString("download_history", "[]").apply()
    }

    override suspend fun getVideoMetadata(url: String, format: String, quality: String, audioLang: String): VideoMetadata =
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().init(context)
            val req = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                when {
                    format == "mp3" -> addOption("-f", "bestaudio/best")
                    quality == "best" -> {
                        val lf = if (audioLang.isNotEmpty()) "[language=$audioLang]" else ""
                        addOption("-f", "bestvideo+bestaudio$lf/best")
                    }
                    else -> {
                        val lf = if (audioLang.isNotEmpty()) "[language=$audioLang]" else ""
                        addOption("-f", "bestvideo[height<=${quality.removeSuffix("p")}]+bestaudio$lf/best")
                    }
                }
            }
            val info = YoutubeDL.getInstance().getInfo(req)
            VideoMetadata(title = info.title ?: "", fileSize = info.fileSize, fileSizeApproximate = info.fileSizeApproximate)
        }

    override suspend fun executeDownload(task: DownloadTask): String = withContext(Dispatchers.IO) {
        val savePath = prefs.getString("save_path", null) ?: defaultPath()
        File(savePath).mkdirs()
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)

        val req = YoutubeDLRequest(task.url).apply {
            addOption("-o", "$savePath/%(title)s.%(ext)s")
            addOption("--no-playlist"); addOption("--no-colors"); addOption("--no-mtime")
            addOption("--no-keep-video"); addOption("--print", "after_move:filepath"); addOption("--print", "%(title)s")
            when (task.format) {
                DownloadFormat.MP3 -> { addOption("-f", "bestaudio/best"); addOption("-x"); addOption("--audio-format", "mp3"); addOption("--audio-quality", "192K") }
                DownloadFormat.MP4 -> {
                    addOption("--merge-output-format", "mp4")
                    val lf = if (task.audioLang.isNotEmpty()) "[language=${task.audioLang}]" else ""
                    if (task.quality == "best") addOption("-f", "bestvideo+bestaudio$lf/best")
                    else addOption("-f", "bestvideo[height<=${task.quality.replace("p", "")}]+bestaudio$lf/best")
                }
            }
        }

        var filePath = ""; var title = ""
        YoutubeDL.getInstance().execute(req, task.id) { pct, _, line ->
            val pp = Regex("""(\d+(?:\.\d+)?)%""").find(line)?.groupValues?.get(1)?.toDoubleOrNull()
            val p = when { pct > 0.0f -> pct.roundToInt(); pp != null -> pp.roundToInt(); else -> -1 }.coerceIn(-1, 100)
            val spd = Regex("""at\s+(\S+)\s""").find(line)?.groupValues?.get(1).orEmpty()
            val current = _currentTasks.value.toMutableMap()
            current[task.id] = task.copy(progress = p, speed = spd)
            _currentTasks.value = current
            val t = line.trim()
            if (t.startsWith("/")) filePath = t
            else if (t.isNotEmpty() && title.isEmpty() && !t.startsWith("[") && !t.contains("%")) title = t
        }

        val finalPath = if (filePath.startsWith("/storage/emulated/0/Download")) filePath else copyToPublic(filePath) ?: filePath
        addToHistory(DownloadHistoryEntry(url = task.url, title = title, format = task.format.name.lowercase(), quality = task.quality, filePath = finalPath))
        _taskEvents.value = TaskEvent.Completed(task.id, finalPath)
        val current = _currentTasks.value.toMutableMap()
        current[task.id] = task.copy(status = DownloadStatus.COMPLETED, progress = 100, filePath = finalPath)
        _currentTasks.value = current
        finalPath
    }

    override fun cancelDownload(taskId: String) {
        YoutubeDL.getInstance().destroyProcessById(taskId)
        val current = _currentTasks.value.toMutableMap()
        current[taskId]?.let { current[taskId] = it.copy(status = DownloadStatus.CANCELLED) }
        _currentTasks.value = current
    }

    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val uri = findDownloadUri(filePath)
        val file = File(filePath)
        val deleted = when { uri != null -> context.contentResolver.delete(uri, null, null) > 0; file.exists() -> file.delete(); else -> true }
        if (deleted) removeFromHistory(filePath)
        deleted
    }

    fun findDownloadUri(filePath: String): Uri? {
        val fileName = File(filePath).name; if (fileName.isBlank()) return null
        context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.RELATIVE_PATH),
            "${MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf(fileName), null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            while (c.moveToNext()) {
                if (c.getString(pathCol) in listOf("Download/YTDow/", "Download/YTDow"))
                    return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(idCol).toString())
            }
        }
        return null
    }

    private fun copyToPublic(srcPath: String): String? {
        if (srcPath.isEmpty() || !File(srcPath).exists()) return null
        return try {
            val sf = File(srcPath); val fn = sf.name
            val mime = when { fn.endsWith(".mp3") -> "audio/mpeg"; fn.endsWith(".mp4") -> "video/mp4"; else -> "*/*" }
            val cv = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, fn); put(MediaStore.Downloads.MIME_TYPE, mime); put(MediaStore.Downloads.RELATIVE_PATH, "Download/YTDow") }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { sf.inputStream().use { inp -> inp.copyTo(it) } } ?: return null
            sf.delete()
            "/storage/emulated/0/Download/YTDow/$fn"
        } catch (e: Exception) { ServiceLocator.logger.e("YTDow", "copyToPublic", e); null }
    }

    private fun loadHistory(): List<DownloadHistoryEntry> {
        return try {
            val arr = JSONArray(prefs.getString("download_history", "[]") ?: "[]")
            (0 until arr.length()).map { i -> val e = arr.getJSONObject(i); DownloadHistoryEntry(url = e.optString("url"), title = e.optString("title"), format = e.optString("format", "mp4"), quality = e.optString("quality", "best"), filePath = e.optString("filePath"), time = e.optLong("time", System.currentTimeMillis())) }.reversed()
        } catch (e: Exception) { ServiceLocator.logger.e("YTDow", "loadHistory", e); emptyList() }
    }

    private fun defaultPath() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDow").apply { mkdirs() }.absolutePath
}

sealed class TaskEvent {
    data class Completed(val taskId: String, val filePath: String) : TaskEvent()
    data class Error(val taskId: String, val message: String) : TaskEvent()
    data class Progress(val taskId: String, val percent: Int, val speed: String, val eta: String) : TaskEvent()
}
