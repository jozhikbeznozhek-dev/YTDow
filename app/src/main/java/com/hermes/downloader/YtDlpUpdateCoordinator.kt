package com.hermes.downloader

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL

/** Prevents the delayed startup update from racing the first download. */
object YtDlpUpdateCoordinator {
    private const val PREFS_NAME = "ytdow"
    private const val LAST_UPDATE_KEY = "ytdlp_last_update"
    private const val UPDATE_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L

    @Volatile
    private var attemptedThisProcess = false

    @Synchronized
    fun updateIfDue(context: Context) {
        if (attemptedThisProcess) return

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(LAST_UPDATE_KEY, 0)
        if (System.currentTimeMillis() - lastUpdate <= UPDATE_INTERVAL_MS) {
            attemptedThisProcess = true
            return
        }

        attemptedThisProcess = true
        try {
            val ytdlp = YoutubeDL.getInstance()
            ytdlp.init(appContext)
            val status = ytdlp.updateYoutubeDL(appContext, YoutubeDL.UpdateChannel._STABLE)
            prefs.edit().putLong(LAST_UPDATE_KEY, System.currentTimeMillis()).apply()
            Log.i("YTDow", "yt-dlp update result: $status")
        } catch (error: Throwable) {
            // A failed engine update must never prevent the bundled engine from running.
            Log.w("YTDow", "yt-dlp auto-update skipped", error)
        }
    }
}
