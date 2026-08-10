package com.hermes.downloader.domain.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStagingDirectoryTest {

    @Test
    fun `uses an app-private downloads directory`() {
        val appFilesDirectory = File("/data/user/0/com.hermes.downloader/files")

        val stagingDirectory = DownloadStagingDirectory.from(appFilesDirectory)

        assertEquals(
            File(appFilesDirectory, "downloads").path,
            stagingDirectory.path
        )
    }

    @Test
    fun `accepts only files inside the task staging directory`() {
        val taskDirectory = File("/data/user/0/com.hermes.downloader/files/downloads/task")

        assertTrue(
            DownloadStagingDirectory.contains(taskDirectory, File(taskDirectory, "video.mp4"))
        )
        assertFalse(
            DownloadStagingDirectory.contains(taskDirectory, File("${taskDirectory.path}-other/video.mp4"))
        )
        assertFalse(
            DownloadStagingDirectory.contains(taskDirectory, File(taskDirectory, "../video.mp4"))
        )
    }
}
