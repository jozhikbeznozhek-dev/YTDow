package com.hermes.downloader.presentation.main

import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.queue.QueueTask
import com.hermes.downloader.domain.queue.QueueManager
import com.hermes.downloader.domain.repository.DownloadRepository
import com.hermes.downloader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.UUID
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadRepo: DownloadRepository,
    private val queueManager: QueueManager,
    private val settingsRepo: SettingsRepository,
    private val prefs: android.content.SharedPreferences
) : androidx.lifecycle.ViewModel() {

    var currentFormat = DownloadFormat.MP4
    var currentQuality = "best"
    var currentAudioLang = ""

    fun launchIO(block: suspend () -> Unit) { viewModelScope.launch(Dispatchers.IO) { block() } }

    fun calculateSize(url: String, format: String, quality: String, audioLang: String,
                       onResult: (sizeBytes: Long, title: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meta = downloadRepo.getVideoMetadata(url, format, quality, audioLang)
                val bytes = meta.fileSize.takeIf { it > 0 } ?: meta.fileSizeApproximate
                withContext(Dispatchers.Main) { onResult(bytes, meta.title) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(0, "") } }
        }
    }

    fun startDownload(url: String) {
        val task = QueueTask(id = UUID.randomUUID().toString().take(8), url = url, format = currentFormat, quality = currentQuality, audioLang = currentAudioLang)
        queueManager.enqueue(listOf(task))
        processQueueLoop()
    }

    fun cancelTask(taskId: String) { queueManager.cancel(taskId); downloadRepo.cancelDownload(taskId) }
    fun pauseTask(taskId: String) = queueManager.pause(taskId)
    fun resumeTask(taskId: String) = queueManager.resume(taskId)
    fun retryTask(taskId: String) = queueManager.retry(taskId)
    fun getAllQueueTasks(): List<QueueTask> = queueManager.getAllTasks()

    fun deleteFile(filePath: String) { viewModelScope.launch { downloadRepo.deleteFile(filePath) } }

    fun getSavePath(): String = prefs.getString("save_path", defaultPath()) ?: defaultPath()
    fun getHistoryJson(): String = prefs.getString("download_history", "[]") ?: "[]"
    fun getUrlHistory(): String = prefs.getString("history", "[]") ?: "[]"

    private fun processQueueLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val next = queueManager.nextDownloadTask() ?: break
                try {
                    val domainTask = DownloadTask(id = next.id, url = next.url, format = next.format, quality = next.quality, audioLang = next.audioLang)
                    queueManager.onTaskPrepared(next.id, VideoMetadata(title = ""))
                    val filePath = downloadRepo.executeDownload(domainTask)
                    val entry = DownloadHistoryEntry(url = next.url, title = domainTask.title, format = next.format.name.lowercase(), quality = next.quality, filePath = filePath)
                    queueManager.onTaskCompleted(next.id, filePath, entry)
                } catch (e: Exception) {
                    queueManager.onTaskFailed(next.id, e.message ?: "Ошибка")
                    val task = queueManager.getAllTasks().find { it.id == next.id }
                    if (task != null && task.retryCount < task.maxRetries) queueManager.retry(next.id)
                }
            }
        }
    }

    private fun defaultPath(): String {
        val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        return java.io.File(dir, "YTDow").apply { mkdirs() }.absolutePath
    }
}
