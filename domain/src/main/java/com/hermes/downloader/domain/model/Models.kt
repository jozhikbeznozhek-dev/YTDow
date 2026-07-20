package com.hermes.downloader.domain.model

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String = "Ожидание...",
    val format: DownloadFormat = DownloadFormat.MP4,
    val quality: String = "best",
    val audioLang: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,          // 0-100, -1 = indeterminate
    val speed: String = "",
    val eta: String = "",
    val filePath: String = "",
    val errorMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class DownloadFormat { MP4, MP3 }

enum class DownloadStatus {
    QUEUED,
    PREPARING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadHistoryEntry(
    val url: String,
    val title: String,
    val format: String,
    val quality: String,
    val filePath: String,
    val sizeBytes: Long = 0,
    val time: Long = System.currentTimeMillis()
)

data class VideoMetadata(
    val title: String,
    val duration: String = "",
    val fileSize: Long = 0,
    val fileSizeApproximate: Long = 0,
    val formats: List<String> = emptyList(),
    val audioLanguages: List<String> = emptyList(),
    val formatSizes: Map<String, Long> = emptyMap()
)
