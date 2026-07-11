package com.hermes.downloader

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class DownloadService : Service() {

    private val active = ConcurrentHashMap<String, Boolean>()
    private val pool = Executors.newFixedThreadPool(3)
    private var initDone = false

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, i: Intent?) {
            val tid = i?.getStringExtra(EXTRA_TASK_ID) ?: return
            YoutubeDL.getInstance().destroyProcessById(tid)
            active.remove(tid)
            if (active.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() } else notifySummary()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, cancelReceiver, IntentFilter(ACTION_CANCEL), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val fmt = intent.getStringExtra(EXTRA_FORMAT) ?: "mp4"
        val qual = intent.getStringExtra(EXTRA_QUALITY) ?: "best"
        val lang = intent.getStringExtra(EXTRA_AUDIO_LANG) ?: ""
        val tid = intent.getStringExtra(EXTRA_TASK_ID) ?: "unknown"
        val save = intent.getStringExtra(EXTRA_SAVE_PATH) ?: return START_NOT_STICKY

        active[tid] = true
        if (!initDone) { startForeground(1, n("Загрузка...", 0)); initDone = true }
        notifySummary()

        pool.execute {
            try {
                File(save).mkdirs()
                YoutubeDL.getInstance().init(this@DownloadService)

                val req = YoutubeDLRequest(url).apply {
                    addOption("-o", "$save/%(title)s.%(ext)s")
                    addOption("--no-playlist"); addOption("--no-colors"); addOption("--no-mtime")
                    addOption("--print", "after_move:filepath")
                    addOption("--print", "%(title)s")  // название видео
                    when {
                        fmt == "mp3" -> {
                            addOption("-f", "bestaudio/best")
                            addOption("-x"); addOption("--audio-format", "mp3"); addOption("--audio-quality", "192K")
                        }
                        else -> {
                            addOption("--merge-output-format", "mp4")
                            val langFilter = if (lang.isNotEmpty()) "[language=${lang}]" else ""
                            if (qual == "best") addOption("-f", "bestvideo+bestaudio${langFilter}/best")
                            else addOption("-f", "bestvideo[height<=${qual.replace("p", "")}]+bestaudio${langFilter}/best")
                        }
                    }
                }

                var filePath = ""
                var title = ""
                YoutubeDL.getInstance().execute(req, tid) { pct, eta, line ->
                    if (!active.containsKey(tid)) return@execute
                    val p = pct.roundToInt().coerceIn(0, 100)
                    val spd = Regex("""at\s+(\S+)\s""").find(line)?.groupValues?.get(1).orEmpty()
                    val et = Regex("""ETA\s+(\S+)""").find(line)?.groupValues?.get(1).orEmpty()
                    sendProgress(tid, p, spd, et)
                    val t = line.trim()
                    if (t.startsWith("/")) filePath = t
                    // Ловим название: %(title)s печатает его отдельной строкой
                    else if (t.isNotEmpty() && title.isEmpty() && !t.startsWith("[") && !t.contains("%")) {
                        title = t
                    }
                }

                if (active.containsKey(tid)) {
                    val prefs = getSharedPreferences("ytdow", MODE_PRIVATE)
                    val hist = prefs.getString("download_history", "[]") ?: "[]"
                    val arr = org.json.JSONArray(hist)
                    val entry = org.json.JSONObject().apply {
                        put("url", url); put("title", title); put("format", fmt)
                        put("quality", qual); put("filePath", filePath)
                        put("time", System.currentTimeMillis())
                    }
                    arr.put(entry.toString())
                    // Оставляем последние 50
                    val trimmed = org.json.JSONArray()
                    val start = maxOf(0, arr.length() - 50)
                    for (i in start until arr.length()) trimmed.put(arr.get(i))
                    prefs.edit().putString("download_history", trimmed.toString()).apply()

                    sendComplete(tid, filePath)
                }
            } catch (e: Exception) {
                if (active.containsKey(tid)) sendError(tid, e.message ?: "Ошибка")
            } finally {
                active.remove(tid)
                if (active.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() } else notifySummary()
            }
        }
        return START_NOT_STICKY
    }

    private fun notifySummary() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, n(if (active.size <= 1) "Загрузка..." else "Загрузок: ${active.size}", 0))
    }

    private fun n(t: String, p: Int) = NotificationCompat.Builder(this, "downloads")
        .setContentTitle("YTDow").setContentText(t)
        .setSmallIcon(android.R.drawable.stat_sys_download).setProgress(100, p, p == 0).setOngoing(true).build()

    private fun sendProgress(tid: String, pct: Int, speed: String, eta: String) {
        sendBroadcast(Intent(ACTION_PROGRESS).apply {
            setPackage(packageName); putExtra(EXTRA_TASK_ID, tid); putExtra(EXTRA_PERCENT, pct)
            putExtra(EXTRA_SPEED, speed); putExtra(EXTRA_ETA, eta)
        })
    }

    private fun sendComplete(tid: String, fp: String) {
        sendBroadcast(Intent(ACTION_COMPLETE).apply {
            setPackage(packageName); putExtra(EXTRA_TASK_ID, tid); putExtra(EXTRA_FILE_PATH, fp)
        })
    }

    private fun sendError(tid: String, err: String) {
        sendBroadcast(Intent(ACTION_ERROR).apply {
            setPackage(packageName); putExtra(EXTRA_TASK_ID, tid); putExtra(EXTRA_ERROR, err)
        })
    }

    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { pool.shutdown(); unregisterReceiver(cancelReceiver); super.onDestroy() }

    companion object {
        const val ACTION_CANCEL = "com.hermes.downloader.CANCEL"
        const val ACTION_PROGRESS = "com.hermes.downloader.PROGRESS"
        const val ACTION_COMPLETE = "com.hermes.downloader.COMPLETE"
        const val ACTION_ERROR = "com.hermes.downloader.ERROR"
        const val EXTRA_URL = "url"; const val EXTRA_FORMAT = "format"; const val EXTRA_QUALITY = "quality"
        const val EXTRA_TASK_ID = "taskId"; const val EXTRA_SAVE_PATH = "savePath"; const val EXTRA_PERCENT = "percent"
        const val EXTRA_SPEED = "speed"; const val EXTRA_ETA = "eta"; const val EXTRA_ERROR = "error"
        const val EXTRA_FILE_PATH = "filePath"; const val EXTRA_AUDIO_LANG = "audioLang"
    }
}
