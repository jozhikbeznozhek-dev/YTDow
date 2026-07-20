package com.hermes.downloader.domain.usecase

import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadHistoryUseCase(private val repo: DownloadRepository) {
    operator fun invoke(): Flow<List<DownloadHistoryEntry>> = repo.getHistory()
}

class AddToHistoryUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(entry: DownloadHistoryEntry) = repo.addToHistory(entry)
}

class RemoveFromHistoryUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(filePath: String) = repo.removeFromHistory(filePath)
}

class GetVideoMetadataUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(url: String, format: String, quality: String, audioLang: String): VideoMetadata =
        repo.getVideoMetadata(url, format, quality, audioLang)
}

class StartDownloadUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(task: DownloadTask): String = repo.executeDownload(task)
}

class CancelDownloadUseCase(private val repo: DownloadRepository) {
    operator fun invoke(taskId: String) = repo.cancelDownload(taskId)
}

class DeleteFileUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(filePath: String): Boolean = repo.deleteFile(filePath)
}
