package com.hermes.downloader.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hermes.downloader.core.Logger
import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.repository.DownloadRepository

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) : DownloadRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("ytdow", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow(loadHistory())

    override fun getHistory(): Flow<List<DownloadHistoryEntry>> = _history.asStateFlow()

    override suspend fun addToHistory(entry: DownloadHistoryEntry) = withContext(Dispatchers.IO) {
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
        } catch (e: Exception) { logger.e("YTDow", "addToHistory", e) }
    }

    override suspend fun removeFromHistory(filePath: String) = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray(prefs.getString("download_history", "[]") ?: "[]")
            val filtered = JSONArray()
            for (i in 0 until arr.length())
                if (arr.getJSONObject(i).optString("filePath") != filePath) filtered.put(arr[i])
            prefs.edit().putString("download_history", filtered.toString()).apply()
        } catch (e: Exception) { logger.e("YTDow", "removeFromHistory", e) }
    }

    override suspend fun clearHistory() { prefs.edit().putString("download_history", "[]").apply() }

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
        val uri = findDownloadUri(filePath)
        val file = File(filePath)
        val deleted = when { uri != null -> context.contentResolver.delete(uri, null, null) > 0; file.exists() -> file.delete(); else -> true }
        if (deleted) removeFromHistory(filePath)
        deleted
    }

    fun findDownloadUri(filePath: String): android.net.Uri? {
        val fileName = File(filePath).name; if (fileName.isBlank()) return null
        context.contentResolver.query(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(android.provider.MediaStore.Downloads._ID, android.provider.MediaStore.Downloads.DISPLAY_NAME, android.provider.MediaStore.Downloads.RELATIVE_PATH),
            "${android.provider.MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf(fileName), null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID)
            val pathCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Downloads.RELATIVE_PATH)
            while (c.moveToNext())
                if (c.getString(pathCol) in listOf("Download/YTDow/", "Download/YTDow"))
                    return android.net.Uri.withAppendedPath(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(idCol).toString())
        }
        return null
    }


    private fun loadHistory(): List<DownloadHistoryEntry> = try {
        val arr = JSONArray(prefs.getString("download_history", "[]") ?: "[]")
        (0 until arr.length()).map { i -> val e = arr.getJSONObject(i); DownloadHistoryEntry(url = e.optString("url"), title = e.optString("title"), format = e.optString("format", "mp4"), quality = e.optString("quality", "best"), filePath = e.optString("filePath"), time = e.optLong("time", System.currentTimeMillis())) }.reversed()
    } catch (e: Exception) { logger.e("YTDow", "loadHistory", e); emptyList() }

}
