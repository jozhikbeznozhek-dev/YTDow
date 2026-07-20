package com.hermes.downloader.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.hermes.downloader.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
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
            if (key == "save_path") trySend(prefs.getString("save_path", defaultPath()) ?: defaultPath())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString("save_path", defaultPath()) ?: defaultPath())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSavePath(path: String) { prefs.edit().putString("save_path", path).apply() }

    private fun defaultPath() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDow").apply { mkdirs() }.absolutePath
}
