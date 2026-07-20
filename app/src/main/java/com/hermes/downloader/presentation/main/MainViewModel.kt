package com.hermes.downloader.presentation.main

import com.hermes.downloader.data.ServiceLocator
import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.queue.QueueTask
import kotlinx.coroutines.*
import java.util.UUID

class MainViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val downloadRepo = ServiceLocator.downloadRepo
    private val queueManager = ServiceLocator.queueManager
    private val settingsRepo = ServiceLocator.settingsRepo

    /** Launch a coroutine on IO dispatcher — exposed for bridge methods. */
    fun launchIO(block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) { block() }
    }

    var currentFormat = DownloadFormat.MP4
    var currentQuality = "best"
    var currentAudioLang = ""

    fun calculateSize(url: String, format: String, quality: String, audioLang: String,
                       onResult: (sizeBytes: Long, title: String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val meta = downloadRepo.getVideoMetadata(url, format, quality, audioLang)
                val bytes = meta.fileSize.takeIf { it > 0 } ?: meta.fileSizeApproximate
                withContext(Dispatchers.Main) { onResult(bytes, meta.title) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(0, "") }
            }
        }
    }

    fun startDownload(url: String) {
        val task = QueueTask(
            id = UUID.randomUUID().toString().take(8),
            url = url,
            format = currentFormat,
            quality = currentQuality,
            audioLang = currentAudioLang
        )
        queueManager.enqueue(listOf(task))
        processQueueLoop()
    }

    fun cancelTask(taskId: String) {
        queueManager.cancel(taskId)
        downloadRepo.cancelDownload(taskId)
    }

    fun pauseTask(taskId: String) = queueManager.pause(taskId)
    fun resumeTask(taskId: String) = queueManager.resume(taskId)
    fun retryTask(taskId: String) = queueManager.retry(taskId)

    fun deleteFile(filePath: String) {
        scope.launch { downloadRepo.deleteFile(filePath) }
    }

    fun getSavePath(): String {
        return ServiceLocator.prefs.getString("save_path", defaultPath()) ?: defaultPath()
    }

    fun setSavePath(path: String) {
        scope.launch { settingsRepo.setSavePath(path) }
    }

    fun getHistoryJson(): String = ServiceLocator.prefs.getString("download_history", "[]") ?: "[]"
    fun getUrlHistory(): String = ServiceLocator.prefs.getString("history", "[]") ?: "[]"

    fun getAllQueueTasks(): List<QueueTask> = queueManager.getAllTasks()

    /** Process the queue: pick next task and start downloading until queue is empty or slots full. */
    private fun processQueueLoop() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                val next = queueManager.nextDownloadTask() ?: break
                try {
                    val domainTask = DownloadTask(
                        id = next.id,
                        url = next.url,
                        format = next.format,
                        quality = next.quality,
                        audioLang = next.audioLang
                    )
                    queueManager.onTaskPrepared(next.id, VideoMetadata(title = ""))
                    val filePath = downloadRepo.executeDownload(domainTask)
                    val entry = DownloadHistoryEntry(
                        url = next.url,
                        title = domainTask.title,
                        format = next.format.name.lowercase(),
                        quality = next.quality,
                        filePath = filePath
                    )
                    queueManager.onTaskCompleted(next.id, filePath, entry)
                } catch (e: Exception) {
                    queueManager.onTaskFailed(next.id, e.message ?: "Ошибка")
                    // Auto-retry if within limits
                    val task = queueManager.getAllTasks().find { it.id == next.id }
                    if (task != null && task.retryCount < task.maxRetries) {
                        queueManager.retry(next.id)
                    }
                }
            }
        }
    }

    private fun defaultPath(): String {
        val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        return java.io.File(dir, "YTDow").apply { mkdirs() }.absolutePath
    }
}
