package com.hermes.downloader.data.queue

import com.hermes.downloader.core.Logger
import com.hermes.downloader.domain.model.DownloadHistoryEntry
import com.hermes.downloader.domain.model.VideoMetadata
import com.hermes.downloader.domain.queue.DownloadState
import com.hermes.downloader.domain.queue.QueueManager
import com.hermes.downloader.domain.queue.QueueTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManagerImpl @Inject constructor(
    private val logger: Logger
) : QueueManager {

    private val queue = PriorityBlockingQueue<QueueTask>(11) { a, b -> b.priority.compareTo(a.priority) }
    private val tasks = ConcurrentHashMap<String, QueueTask>()
    private val downloading = ConcurrentHashMap<String, Boolean>()
    override var maxConcurrent = 3

    override fun enqueue(tasks: List<QueueTask>) {
        var enqueuedCount = 0
        for (task in tasks) {
            if (this.tasks.containsKey(task.id)) {
                logger.w("YTDowQueue", "ignored duplicate task id: ${task.id}")
                continue
            }
            this.tasks[task.id] = task
            queue.add(task)
            enqueuedCount++
        }
        logger.d("YTDowQueue", "enqueued: $enqueuedCount")
        processQueue()
    }

    override fun cancel(taskId: String) {
        tasks[taskId]?.let { task ->
            if (!task.state.canTransitionTo(DownloadState.CANCELLED)) return
            if (task.state == DownloadState.DOWNLOADING || task.state == DownloadState.PREPARING) downloading.remove(taskId)
            queue.remove(task)
            tasks[taskId] = task.copy(state = DownloadState.CANCELLED)
            logger.d("YTDowQueue", "cancelled: $taskId")
        }
        processQueue()
    }

    override fun pause(taskId: String) {
        tasks[taskId]?.let { task ->
            if (!task.state.canTransitionTo(DownloadState.PAUSED)) return
            if (task.state == DownloadState.DOWNLOADING) {
                downloading.remove(taskId)
                tasks[taskId] = task.copy(state = DownloadState.PAUSED)
                logger.d("YTDowQueue", "paused: $taskId")
            }
        }
        processQueue()
    }

    override fun resume(taskId: String) {
        tasks[taskId]?.let { task ->
            if (!task.state.canTransitionTo(DownloadState.QUEUED)) return
            if (task.state == DownloadState.PAUSED) {
                tasks[taskId] = task.copy(state = DownloadState.QUEUED)
                queue.add(tasks[taskId]!!)
                logger.d("YTDowQueue", "resumed: $taskId")
            }
        }
        processQueue()
    }

    override fun retry(taskId: String) {
        tasks[taskId]?.let { task ->
            if (!task.state.canTransitionTo(DownloadState.QUEUED)) return
            if (task.state == DownloadState.FAILED && task.retryCount < task.maxRetries) {
                tasks[taskId] = task.copy(state = DownloadState.QUEUED, retryCount = task.retryCount + 1, errorMessage = "")
                queue.add(tasks[taskId]!!)
                logger.d("YTDowQueue", "retry: $taskId attempt=${task.retryCount + 1}")
            }
        }
        processQueue()
    }

    override fun getAllTasks(): List<QueueTask> = tasks.values.toList()

    override fun onTaskCompleted(taskId: String, filePath: String, historyEntry: DownloadHistoryEntry) {
        downloading.remove(taskId)
        tasks[taskId]?.let { tasks[taskId] = it.copy(state = DownloadState.COMPLETED, filePath = filePath, progress = 100, title = historyEntry.title) }
        logger.d("YTDowQueue", "completed: $taskId")
        processQueue()
    }

    override fun onTaskFailed(taskId: String, error: String) {
        downloading.remove(taskId)
        tasks[taskId]?.let { tasks[taskId] = it.copy(state = DownloadState.FAILED, errorMessage = error) }
        logger.e("YTDowQueue", "failed: $taskId error=$error")
        processQueue()
    }

    override fun onTaskProgress(taskId: String, progress: Int, speed: String, eta: String) {
        tasks[taskId]?.let { tasks[taskId] = it.copy(state = DownloadState.DOWNLOADING, progress = progress, speed = speed, eta = eta) }
    }

    override fun onTaskPrepared(taskId: String, metadata: VideoMetadata) {
        tasks[taskId]?.let { tasks[taskId] = it.copy(state = DownloadState.PREPARING, title = metadata.title) }
    }

    override fun nextDownloadTask(): QueueTask? {
        if (downloading.size >= maxConcurrent) return null
        val task = queue.poll() ?: return null
        tasks[task.id] = task.copy(state = DownloadState.PREPARING)
        downloading[task.id] = true
        return tasks[task.id]
    }

    private fun processQueue() {}

    fun activeCount(): Int = downloading.size
    fun queuedCount(): Int = queue.size
}
