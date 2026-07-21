package com.hermes.downloader.domain.repository

import com.hermes.downloader.domain.model.DownloadHistoryEntry

import com.hermes.downloader.domain.model.VideoMetadata
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getHistory(): Flow<List<DownloadHistoryEntry>>
    suspend fun addToHistory(entry: DownloadHistoryEntry)
    suspend fun removeFromHistory(filePath: String)
    suspend fun clearHistory()

    suspend fun getVideoMetadata(url: String, format: String, quality: String, audioLang: String): VideoMetadata
    suspend fun deleteFile(filePath: String): Boolean
}

interface SettingsRepository {
    val savePath: Flow<String>
    suspend fun setSavePath(path: String)
}

interface UpdateRepository {
    suspend fun checkForUpdate(): UpdateInfo
}

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val currentVersion: String
)
