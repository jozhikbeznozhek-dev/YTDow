package com.hermes.downloader.domain.queue

import com.hermes.downloader.domain.model.DownloadFormat
import com.hermes.downloader.domain.model.DownloadHistoryEntry
import com.hermes.downloader.domain.model.VideoMetadata

data class QueueTask(
    val id: String,
    val url: String,
    val format: DownloadFormat = DownloadFormat.MP4,
    val quality: String = "best",
    val audioLang: String = "",
    val priority: Int = 0,            // higher = more important
    val state: DownloadState = DownloadState.QUEUED,
    val progress: Int = 0,            // 0-100, -1 = indeterminate
    val speed: String = "",
    val eta: String = "",
    val title: String = "",
    val filePath: String = "",
    val errorMessage: String = "",
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

// ── Queue Manager interface ──
interface QueueManager {
    /** Add task(s) to the queue. Tasks start QUEUED, then move to PREPARING when slot opens. */
    fun enqueue(tasks: List<QueueTask>)

    /** Cancel a task wherever it is in the pipeline. */
    fun cancel(taskId: String)

    /** Pause an active download (if supported). */
    fun pause(taskId: String)

    /** Resume a paused task. */
    fun resume(taskId: String)

    /** Retry a failed task. */
    fun retry(taskId: String)

    /** Get current snapshot of all tasks and their states. */
    fun getAllTasks(): List<QueueTask>

    /** Maximum concurrent downloads. */
    var maxConcurrent: Int

    /** Called by DownloadRepository when a download completes. */
    fun onTaskCompleted(taskId: String, filePath: String, historyEntry: DownloadHistoryEntry)

    /** Called by DownloadRepository when a download fails. */
    fun onTaskFailed(taskId: String, error: String)

    /** Called by DownloadRepository with progress updates. */
    fun onTaskProgress(taskId: String, progress: Int, speed: String, eta: String)

    /** Called when metadata is fetched for a task. */
    fun onTaskPrepared(taskId: String, metadata: VideoMetadata)

    /** Get next task that should start downloading, or null if queue is empty/busy. */
    fun nextDownloadTask(): QueueTask?
}
