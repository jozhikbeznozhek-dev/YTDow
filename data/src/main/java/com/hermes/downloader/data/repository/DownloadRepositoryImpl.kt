package com.hermes.downloader.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hermes.downloader.core.Logger
import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.repository.DownloadRepository

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger
) : DownloadRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("ytdow", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow(loadHistory())
    private val historyMutex = Mutex()

    override fun getHistory(): Flow<List<DownloadHistoryEntry>> = _history.asStateFlow()

    override suspend fun addToHistory(entry: DownloadHistoryEntry) = withContext(Dispatchers.IO) {
        historyMutex.withLock {
            val hist = prefs.getString(HISTORY_KEY, "[]") ?: "[]"
            val arr = runCatching { JSONArray(hist) }.getOrElse { JSONArray() }
            arr.put(JSONObject().apply {
                put("url", entry.url)
                put("title", entry.title)
                put("format", entry.format)
                put("quality", entry.quality)
                put("filePath", entry.filePath)
                put("sizeBytes", entry.sizeBytes)
                put("time", entry.time)
            })
            persistHistory(trimmed(arr))
        }
    }

    override suspend fun removeFromHistory(filePath: String) = withContext(Dispatchers.IO) {
        historyMutex.withLock {
            val arr = runCatching {
                JSONArray(prefs.getString(HISTORY_KEY, "[]") ?: "[]")
            }.getOrElse { JSONArray() }
            val filtered = JSONArray()
            for (i in 0 until arr.length())
                if (arr.getJSONObject(i).optString("filePath") != filePath) filtered.put(arr[i])
            persistHistory(filtered)
        }
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyMutex.withLock { persistHistory(JSONArray()) }
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


    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = resolveOwnedDownloadUri(filePath)
            val deleted = when {
                uri != null -> context.contentResolver.delete(uri, null, null) > 0
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> deleteOwnedLegacyFile(filePath)
                else -> false
            }
            if (deleted) removeFromHistory(filePath)
            deleted
        }.onFailure { logger.e("YTDow", "deleteFile", it) }.getOrDefault(false)
    }

    private fun resolveOwnedDownloadUri(filePath: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (filePath.startsWith("content://")) {
            val uri = Uri.parse(filePath)
            return uri.takeIf(::isOwnedDownloadUri)
        }

        val fileName = File(filePath).name
        if (fileName.isBlank()) return null
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.RELATIVE_PATH),
            "${MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf(fileName),
            "${MediaStore.Downloads.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                if (isYTDowRelativePath(cursor.getString(pathColumn))) {
                    return Uri.withAppendedPath(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn).toString()
                    )
                }
            }
        }
        return null
    }

    private fun isOwnedDownloadUri(uri: Uri): Boolean {
        if (uri.scheme != "content" || uri.authority != MediaStore.AUTHORITY) return false
        return context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Downloads.RELATIVE_PATH),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst() && isYTDowRelativePath(
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH))
            )
        } == true
    }

    private fun deleteOwnedLegacyFile(filePath: String): Boolean {
        val file = File(filePath).canonicalFile
        @Suppress("DEPRECATION")
        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_DIRECTORY
        ).canonicalFile
        if (file.parentFile != root) return false
        return !file.exists() || file.delete()
    }

    private fun isYTDowRelativePath(path: String?): Boolean =
        path?.trimEnd('/') == "Download/$DOWNLOAD_DIRECTORY"

    private fun trimmed(source: JSONArray): JSONArray = JSONArray().also { result ->
        for (index in maxOf(0, source.length() - MAX_HISTORY_ENTRIES) until source.length()) {
            result.put(source[index])
        }
    }

    private fun persistHistory(history: JSONArray) {
        check(prefs.edit().putString(HISTORY_KEY, history.toString()).commit()) {
            "Unable to persist download history"
        }
        _history.value = loadHistory()
    }

    private fun loadHistory(): List<DownloadHistoryEntry> = try {
        val arr = JSONArray(prefs.getString(HISTORY_KEY, "[]") ?: "[]")
        (0 until arr.length()).map { index ->
            val entry = arr.getJSONObject(index)
            DownloadHistoryEntry(
                url = entry.optString("url"),
                title = entry.optString("title"),
                format = entry.optString("format", "mp4"),
                quality = entry.optString("quality", "best"),
                filePath = entry.optString("filePath"),
                sizeBytes = entry.optLong("sizeBytes", 0),
                time = entry.optLong("time", System.currentTimeMillis())
            )
        }.reversed()
    } catch (e: Exception) { logger.e("YTDow", "loadHistory", e); emptyList() }

    private companion object {
        const val HISTORY_KEY = "download_history"
        const val MAX_HISTORY_ENTRIES = 50
        const val DOWNLOAD_DIRECTORY = "YTDow"
    }

}
