package com.hermes.downloader.data.repository

import android.content.SharedPreferences
import android.os.Environment
import com.hermes.downloader.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class SettingsRepositoryImpl(private val prefs: SharedPreferences) : SettingsRepository {

    override val savePath: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "save_path") {
                trySend(prefs.getString("save_path", defaultPath()) ?: defaultPath())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString("save_path", defaultPath()) ?: defaultPath())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSavePath(path: String) {
        prefs.edit().putString("save_path", path).apply()
    }

    private fun defaultPath(): String =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDow")
            .apply { mkdirs() }.absolutePath
}
