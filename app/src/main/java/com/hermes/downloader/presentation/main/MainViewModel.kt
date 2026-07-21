package com.hermes.downloader.presentation.main

import com.hermes.downloader.domain.model.*
import com.hermes.downloader.domain.repository.DownloadRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*

import javax.inject.Inject
import androidx.lifecycle.viewModelScope

@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadRepo: DownloadRepository,
    private val prefs: android.content.SharedPreferences
) : androidx.lifecycle.ViewModel() {

    var currentFormat = DownloadFormat.MP4
    var currentQuality = "best"
    var currentAudioLang = ""

    fun launchIO(block: suspend () -> Unit) { viewModelScope.launch(Dispatchers.IO) { block() } }

    fun calculateSize(url: String, format: String, quality: String, audioLang: String,
                       onResult: (sizeBytes: Long, title: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meta = downloadRepo.getVideoMetadata(url, format, quality, audioLang)
                val bytes = meta.fileSize.takeIf { it > 0 } ?: meta.fileSizeApproximate
                withContext(Dispatchers.Main) { onResult(bytes, meta.title) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(0, "") } }
        }
    }

    fun deleteFile(filePath: String) { viewModelScope.launch { downloadRepo.deleteFile(filePath) } }

    fun getSavePath(): String = PUBLISHED_DOWNLOADS_PATH
    fun getHistoryJson(): String = prefs.getString("download_history", "[]") ?: "[]"
    fun getUrlHistory(): String = prefs.getString("history", "[]") ?: "[]"
    private companion object {
        const val PUBLISHED_DOWNLOADS_PATH = "Downloads/YTDow"
    }
}
