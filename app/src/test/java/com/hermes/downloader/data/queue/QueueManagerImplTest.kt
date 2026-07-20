package com.hermes.downloader.data.queue

import com.hermes.downloader.domain.model.DownloadFormat
import com.hermes.downloader.domain.queue.DownloadState
import com.hermes.downloader.domain.queue.QueueTask
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.hermes.downloader.core.Logger

class QueueManagerImplTest {

    private lateinit var qm: QueueManagerImpl
    private val stubLogger = object : Logger {
        override fun d(tag: String, msg: String) {}
        override fun e(tag: String, msg: String, throwable: Throwable?) {}
        override fun w(tag: String, msg: String) {}
        override fun i(tag: String, msg: String) {}
    }

    @Before
    fun setUp() {
        qm = QueueManagerImpl(stubLogger)
        qm.maxConcurrent = 2
    }

    @Test
    fun `enqueue adds tasks to queue`() {
        val task = QueueTask(id = "t1", url = "https://example.com/v1")
        qm.enqueue(listOf(task))
        assertEquals(1, qm.queuedCount())
        assertEquals(0, qm.activeCount())
    }

    @Test
    fun `nextDownloadTask returns task when slot available`() {
        val task = QueueTask(id = "t1", url = "https://example.com/v1")
        qm.enqueue(listOf(task))
        val next = qm.nextDownloadTask()
        assertNotNull(next)
        assertEquals("t1", next?.id)
        assertEquals(DownloadState.PREPARING, next?.state)
    }

    @Test
    fun `nextDownloadTask returns null when slots full`() {
        qm.maxConcurrent = 1
        qm.enqueue(listOf(
            QueueTask(id = "t1", url = "u1"),
            QueueTask(id = "t2", url = "u2")
        ))
        val first = qm.nextDownloadTask()
        assertNotNull(first)
        val second = qm.nextDownloadTask()
        assertNull(second) // slot taken
    }

    @Test
    fun `cancel removes task from queue`() {
        val task = QueueTask(id = "t1", url = "u1")
        qm.enqueue(listOf(task))
        qm.cancel("t1")
        val next = qm.nextDownloadTask()
        assertNull(next) // cancelled, not queued
        assertEquals(DownloadState.CANCELLED, qm.getAllTasks().find { it.id == "t1" }?.state)
    }

    @Test
    fun `retry moves failed task back to queued`() {
        val task = QueueTask(id = "t1", url = "u1", state = DownloadState.FAILED)
        qm.enqueue(listOf(task))
        qm.nextDownloadTask() // consume it
        qm.onTaskFailed("t1", "test error")
        qm.retry("t1")
        val t = qm.getAllTasks().find { it.id == "t1" }
        assertEquals(DownloadState.QUEUED, t?.state)
        assertEquals(1, t?.retryCount)
    }

    @Test
    fun `retry does not exceed max retries`() {
        val task = QueueTask(id = "t1", url = "u1", state = DownloadState.FAILED,
            retryCount = 3, maxRetries = 3)
        qm.enqueue(listOf(task))
        qm.nextDownloadTask()
        qm.onTaskFailed("t1", "error")
        qm.retry("t1") // should not retry beyond max
        val t = qm.getAllTasks().find { it.id == "t1" }
        assertEquals(DownloadState.FAILED, t?.state) // still failed
    }

    @Test
    fun `priority orders tasks correctly`() {
        qm.enqueue(listOf(
            QueueTask(id = "low", url = "u1", priority = 0),
            QueueTask(id = "high", url = "u2", priority = 10),
            QueueTask(id = "mid", url = "u3", priority = 5)
        ))
        assertEquals("high", qm.nextDownloadTask()?.id)
        assertEquals("mid", qm.nextDownloadTask()?.id)
        assertNull(qm.nextDownloadTask()) // maxConcurrent=2, third waits
    }
}
