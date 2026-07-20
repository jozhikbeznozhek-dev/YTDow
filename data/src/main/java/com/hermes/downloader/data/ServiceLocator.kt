package com.hermes.downloader.data

import android.app.Application
import android.content.SharedPreferences
import com.hermes.downloader.data.repository.DownloadRepositoryImpl
import com.hermes.downloader.data.repository.SettingsRepositoryImpl
import com.hermes.downloader.data.queue.QueueManagerImpl
import com.hermes.downloader.domain.repository.DownloadRepository
import com.hermes.downloader.domain.repository.SettingsRepository
import com.hermes.downloader.domain.queue.QueueManager
import com.hermes.downloader.core.Logger

object ServiceLocator {
    lateinit var app: Application
    lateinit var prefs: SharedPreferences

    val downloadRepo: DownloadRepository by lazy { DownloadRepositoryImpl(app, prefs) }
    val settingsRepo: SettingsRepository by lazy { SettingsRepositoryImpl(prefs) }
    val queueManager: QueueManager by lazy { QueueManagerImpl() }
    val logger: Logger by lazy { AndroidLogger() }

    fun init(app: Application) {
        this.app = app
        this.prefs = app.getSharedPreferences("ytdow", 0)
    }
}
