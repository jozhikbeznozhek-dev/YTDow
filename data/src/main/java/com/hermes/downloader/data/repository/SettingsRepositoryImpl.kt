package com.hermes.downloader.data.repository

import android.content.Context
import android.content.SharedPreferences

import com.hermes.downloader.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("ytdow", Context.MODE_PRIVATE)

    override val savePath: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "save_path") trySend(PUBLISHED_DOWNLOADS_PATH)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(PUBLISHED_DOWNLOADS_PATH)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSavePath(path: String) = Unit

    private companion object {
        const val PUBLISHED_DOWNLOADS_PATH = "Downloads/YTDow"
    }
}
