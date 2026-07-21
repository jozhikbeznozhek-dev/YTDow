package com.hermes.downloader.domain.storage

import java.io.File
import org.junit.Assert.assertEquals
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
}
