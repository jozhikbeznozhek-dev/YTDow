package com.hermes.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private var isReceiverRegistered = false
    private var savePath: String = ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, i: Intent?) {
            val action = i?.action ?: return
            val tid = i.getStringExtra(DownloadService.EXTRA_TASK_ID) ?: return
            when (action) {
                DownloadService.ACTION_PROGRESS -> {
                    val pct = i.getIntExtra(DownloadService.EXTRA_PERCENT, 0)
                    val spd = i.getStringExtra(DownloadService.EXTRA_SPEED) ?: ""
                    val eta = i.getStringExtra(DownloadService.EXTRA_ETA) ?: ""
                    toast("📥 $pct%")  // дебаг: видно ли прогресс
                    js("onProgress('$tid',$pct,'${esc(spd)}','${esc(eta)}')")
                }
                DownloadService.ACTION_COMPLETE -> {
                    val fp = i.getStringExtra(DownloadService.EXTRA_FILE_PATH) ?: ""
                    js("onComplete('$tid','${esc(fp)}')")
                }
                DownloadService.ACTION_ERROR -> {
                    val err = i.getStringExtra(DownloadService.EXTRA_ERROR) ?: ""
                    js("onError('$tid','${esc(err)}')")
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("hermes", MODE_PRIVATE)
        savePath = prefs.getString("save_path", null) ?: File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir, "HermesDownloader"
        ).also { it.mkdirs() }.absolutePath

        Thread {
            try { YoutubeDL.getInstance().init(this); YoutubeDL.getInstance().updateYoutubeDL(this, com.yausername.youtubedl_android.YoutubeDL.UpdateChannel._NIGHTLY) } catch (_: Exception) {}
        }.start()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient(); webChromeClient = WebChromeClient()
            setBackgroundColor(0xFF1E1E1E.toInt()); addJavascriptInterface(WebAppInterface(), "Android")
        }
        setContentView(webView); createChannel(); requestNotifyPerm()
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onStart() { super.onStart(); register() }
    override fun onStop() { if (isReceiverRegistered) { unregisterReceiver(receiver); isReceiverRegistered = false }; super.onStop() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("downloads", "Загрузки", NotificationManager.IMPORTANCE_LOW))
    }
    private fun requestNotifyPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
    private fun register() {
        if (isReceiverRegistered) return
        ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
            addAction(DownloadService.ACTION_PROGRESS); addAction(DownloadService.ACTION_COMPLETE); addAction(DownloadService.ACTION_ERROR)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        isReceiverRegistered = true
    }

    private fun js(s: String) { webView.post { webView.evaluateJavascript(s, null) } }
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")

    inner class WebAppInterface {
        @JavascriptInterface fun getHistory(): String = prefs.getString("history", "[]") ?: "[]"
        @JavascriptInterface fun getDownloadHistory(): String = prefs.getString("download_history", "[]") ?: "[]"
        @JavascriptInterface fun getDownloadDir(): String = savePath

        @JavascriptInterface
        fun calcSize(url: String) {
            val u = url; val cb = "calc_${System.currentTimeMillis()}"
            js("onCalcStart()")
            Thread {
                try {
                    YoutubeDL.getInstance().init(this@MainActivity)
                    val req = YoutubeDLRequest(u).apply {
                        addOption("--dump-json"); addOption("--no-playlist"); addOption("--skip-download")
                        addOption("--no-colors"); addOption("--no-warnings")
                    }
                    val sb = StringBuilder()
                    YoutubeDL.getInstance().execute(req, cb) { _, _, line -> sb.append(line) }
                    val raw = sb.toString()
                    // Ищем JSON объект: от первого '{' до последнего '}'
                    val jsonStart = raw.indexOf('{')
                    val jsonEnd = raw.lastIndexOf('}')
                    if (jsonStart < 0 || jsonEnd <= jsonStart) {
                        runOnUiThread { js("onCalcResult('${esc("""{"error":"Не удалось получить данные"}""")}')") }
                        return@Thread
                    }
                    val jsonStr = raw.substring(jsonStart, jsonEnd + 1)
                    runOnUiThread {
                        try {
                            val obj = JSONObject(jsonStr)
                            val title = obj.optString("title", "Без названия")
                            val dur = obj.optLong("duration", 0)
                            val durStr = if (dur >= 3600) "${dur/3600}:${String.format("%02d",(dur%3600)/60)}:${String.format("%02d",dur%60)}"
                                        else "${dur/60}:${String.format("%02d",dur%60)}"

                            // Собираем форматы: высота → размер
                            val formats = obj.optJSONArray("formats") ?: JSONArray()
                            val heightSizes = linkedMapOf<String, Long>() // height → filesize
                            var bestSize = 0L
                            for (fi in 0 until formats.length()) {
                                val f = formats.getJSONObject(fi)
                                val h = f.optInt("height", 0)
                                val fs = f.optLong("filesize", f.optLong("filesize_approx", 0))
                                if (h >= 360 && fs > 0) {
                                    val key = "${h}p"
                                    val existing = heightSizes[key] ?: 0
                                    if (fs > existing) heightSizes[key] = fs
                                }
                                if (fs > bestSize) bestSize = fs
                            }

                            // Формируем строку размеров по высотам
                            val sizes = mutableListOf<String>()
                            val sortedHeights = heightSizes.keys.sortedByDescending { it.removeSuffix("p").toInt() }
                            for (h in sortedHeights) {
                                val bytes = heightSizes[h]!!
                                val sz = when {
                                    bytes > 1_073_741_824L -> "${"%.1f".format(bytes/1_073_741_824.0)} ГБ"
                                    bytes > 1_048_576L -> "${"%.0f".format(bytes/1_048_576.0)} МБ"
                                    else -> "${bytes/1024} КБ"
                                }
                                sizes.add("$h: $sz")
                            }

                            val totalSizeStr = when {
                                bestSize > 1_073_741_824L -> "${"%.1f".format(bestSize/1_073_741_824.0)} ГБ"
                                bestSize > 1_048_576L -> "${"%.0f".format(bestSize/1_048_576.0)} МБ"
                                bestSize > 0 -> "${bestSize/1024} КБ"
                                else -> "?"
                            }

                            val result = JSONObject().apply {
                                put("title", title)
                                put("duration", durStr)
                                put("totalSize", totalSizeStr)
                                put("sizes", sizes.joinToString("\n"))
                                put("heights", JSONArray(sortedHeights))
                            }
                            js("onCalcResult('${esc(result.toString())}')")
                        } catch (e: Exception) {
                            js("onCalcResult('${esc("""{"error":"${(e.message?:"").replace("\"","'")}"}""")}')")
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { js("onCalcResult('${esc("""{"error":"${(e.message?:"").replace("\"","'")}"}""")}')") }
                }
            }.start()
        }

        @JavascriptInterface
        fun startDownload(url: String, format: String, quality: String, taskId: String, customPath: String) {
            if (customPath.isNotEmpty() && customPath != savePath) { savePath = customPath; prefs.edit().putString("save_path", customPath).apply() }
            val arr = try { JSONArray(prefs.getString("history", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            for (i in arr.length()-1 downTo 0) if (arr.getString(i) == url) arr.remove(i); arr.put(url)
            val t = JSONArray(); val s = maxOf(0, arr.length()-10); for (i in s until arr.length()) t.put(arr.get(i))
            prefs.edit().putString("history", t.toString()).apply()
            val intent = Intent(this@MainActivity, DownloadService::class.java).apply {
                putExtra(DownloadService.EXTRA_URL, url); putExtra(DownloadService.EXTRA_FORMAT, format)
                putExtra(DownloadService.EXTRA_QUALITY, quality); putExtra(DownloadService.EXTRA_TASK_ID, taskId)
                putExtra(DownloadService.EXTRA_SAVE_PATH, savePath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        }

        @JavascriptInterface fun cancelDownload(taskId: String) {
            sendBroadcast(Intent(DownloadService.ACTION_CANCEL).apply { setPackage(packageName); putExtra(DownloadService.EXTRA_TASK_ID, taskId) })
        }

        @JavascriptInterface
        fun openFile(filePath: String) {
            try {
                val file = File(filePath)
                if (!file.exists()) { toast("Файл не найден: ${file.name}"); return }
                val uri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file)
                val mime = when { filePath.endsWith(".mp3")->"audio/mpeg"; filePath.endsWith(".mp4")->"video/mp4"; else->"*/*" }
                startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION })
            } catch (e: Exception) { toast(e.message ?: "Ошибка") }
        }

        @JavascriptInterface fun openFolder(path: String) = try {
            startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(path), "resource/folder"); flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (_: Exception) { toast("Папка: $path") }

        @JavascriptInterface fun showToast(msg: String) { toast(msg) }

        @JavascriptInterface
        fun deleteFile(filePath: String) {
            try {
                val file = File(filePath)
                if (file.exists()) file.delete()
                // Удаляем запись из истории
                val hist = prefs.getString("download_history", "[]") ?: "[]"
                val arr = org.json.JSONArray(hist)
                val filtered = org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    try {
                        val entry = org.json.JSONObject(arr.getString(i))
                        if (entry.optString("filePath", "") != filePath) filtered.put(arr.get(i))
                    } catch (_: Exception) { filtered.put(arr.get(i)) }
                }
                prefs.edit().putString("download_history", filtered.toString()).apply()
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun checkUpdate() {
            Thread {
                try {
                    val url = java.net.URL("https://api.github.com/repos/NousResearch/hermes-agent/releases/latest")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000; conn.readTimeout = 5000
                    conn.setRequestProperty("Accept", "application/json")
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val latest = json.optString("tag_name", "").removePrefix("v")
                    js("onUpdateResult('$latest')")
                } catch (e: Exception) {
                    js("onUpdateResult('error')")
                }
            }.start()
        }
    }

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
}
