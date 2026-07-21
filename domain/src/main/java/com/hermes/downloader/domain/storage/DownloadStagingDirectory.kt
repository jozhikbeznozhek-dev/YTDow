package com.hermes.downloader.domain.storage

import java.io.File

/** Resolves the private staging location used before publishing a download. */
object DownloadStagingDirectory {
    fun from(appFilesDirectory: File): File = File(appFilesDirectory, "downloads")
}
