package com.hermes.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.hermes.downloader.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import org.json.JSONObject
import java.io.File
import android.util.Log
import kotlin.concurrent.thread

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private var isReceiverRegistered = false
    private var savePath: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    private val viewModel: MainViewModel by viewModels()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, i: Intent?) {
            val action = i?.action ?: return
            val tid = i.getStringExtra(DownloadService.EXTRA_TASK_ID) ?: return
            when (action) {
                DownloadService.ACTION_PROGRESS -> {
                    val pct = i.getIntExtra(DownloadService.EXTRA_PERCENT, 0)
                    val spd = i.getStringExtra(DownloadService.EXTRA_SPEED) ?: ""
                    val eta = i.getStringExtra(DownloadService.EXTRA_ETA) ?: ""
                    js("onProgress('$tid',$pct,'${escJs(spd)}','${escJs(eta)}')")
                }
                DownloadService.ACTION_COMPLETE -> {
                    val fp = i.getStringExtra(DownloadService.EXTRA_FILE_PATH) ?: ""
                    js("onComplete('$tid','${escJs(fp)}')")
                }
                DownloadService.ACTION_ERROR -> {
                    val err = i.getStringExtra(DownloadService.EXTRA_ERROR) ?: ""
                    js("onError('$tid','${escJs(err)}')")
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("ytdow", MODE_PRIVATE)

        val defaultSaveDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDow").apply { mkdirs() }
        savePath = prefs.getString("save_path", null)?.takeIf { File(it).exists() } ?: defaultSaveDir.absolutePath

        // Auto-update yt-dlp once per week
        val lastUpdate = prefs.getLong("ytdlp_last_update", 0)
        if (System.currentTimeMillis() - lastUpdate > 7 * 24 * 3600 * 1000L) {
            thread(name = "ytdlp-update") {
                try {
                    val ytdlp = com.yausername.youtubedl_android.YoutubeDL.getInstance()
                    ytdlp.init(this@MainActivity)
                    ytdlp.updateYoutubeDL(this@MainActivity, com.yausername.youtubedl_android.YoutubeDL.UpdateChannel._STABLE)
                    prefs.edit().putLong("ytdlp_last_update", System.currentTimeMillis()).apply()
                } catch (_: Exception) {}
            }
        }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            setBackgroundColor(0xFF1E1E1E.toInt())
            addJavascriptInterface(WebAppInterface(), "Android")
        }
        setContentView(webView)
        createChannel()
        requestNotifyPerm()
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onStart() { super.onStart(); register() }
    override fun onStop() {
        if (isReceiverRegistered) { unregisterReceiver(receiver); isReceiverRegistered = false }
        super.onStop()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel("downloads", "Загрузки", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun requestNotifyPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun register() {
        if (isReceiverRegistered) return
        ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
            addAction(DownloadService.ACTION_PROGRESS)
            addAction(DownloadService.ACTION_COMPLETE)
            addAction(DownloadService.ACTION_ERROR)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        isReceiverRegistered = true
    }

    private fun js(s: String) {
        mainHandler.post {
            if (!isFinishing && !isDestroyed) {
                webView.evaluateJavascript(s, null)
            }
        }
    }

    private fun escJs(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun toast(msg: String) {
        mainHandler.post {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface fun getHistory(): String = viewModel.getUrlHistory()
        @JavascriptInterface fun getDownloadHistory(): String = viewModel.getHistoryJson()
        @JavascriptInterface fun getDownloadDir(): String = viewModel.getSavePath()

        @JavascriptInterface
        fun startDownload(url: String, format: String, quality: String, taskId: String, customPath: String, audioLang: String) {
            if (customPath.isNotEmpty() && customPath != savePath) {
                savePath = customPath
                prefs.edit().putString("save_path", customPath).apply()
            }
            // История ввода
            val arr = try {
                org.json.JSONArray(prefs.getString("history", "[]") ?: "[]")
            } catch (_: Exception) {
                org.json.JSONArray()
            }
            for (i in arr.length() - 1 downTo 0) {
                if (arr.getString(i) == url) arr.remove(i)
            }
            arr.put(url)
            val trimmed = org.json.JSONArray()
            val start = maxOf(0, arr.length() - 10)
            for (i in start until arr.length()) trimmed.put(arr[i])
            prefs.edit().putString("history", trimmed.toString()).apply()

            val intent = Intent(this@MainActivity, DownloadService::class.java).apply {
                putExtra(DownloadService.EXTRA_URL, url)
                putExtra(DownloadService.EXTRA_FORMAT, format)
                putExtra(DownloadService.EXTRA_QUALITY, quality)
                putExtra(DownloadService.EXTRA_TASK_ID, taskId)
                putExtra(DownloadService.EXTRA_SAVE_PATH, savePath)
                putExtra(DownloadService.EXTRA_AUDIO_LANG, audioLang)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        @JavascriptInterface
        fun cancelDownload(taskId: String) {
            sendBroadcast(Intent(DownloadService.ACTION_CANCEL).apply {
                setPackage(packageName)
                putExtra(DownloadService.EXTRA_TASK_ID, taskId)
            })
        }

        @JavascriptInterface
        fun openFile(filePath: String) {
            try {
                val file = File(filePath)
                val uri = findDownloadUri(filePath)
                    ?: if (file.exists()) FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file) else null
                if (uri == null) {
                    toast("Файл не найден: ${file.name}")
                    return
                }
                val mime = when {
                    filePath.endsWith(".mp3", true) -> "audio/mpeg"
                    filePath.endsWith(".mp4", true) -> "video/mp4"
                    else -> "*/*"
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                try {
                    startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    toast("Нет приложения для открытия файла")
                }
            } catch (e: Exception) {
                toast(e.message ?: "Ошибка открытия")
            }
        }

        @JavascriptInterface
        fun openFolder(path: String) = try {
            val folderUri = when {
                path.startsWith("file://") || path.startsWith("content://") -> Uri.parse(path)
                else -> Uri.parse("file://$path")
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                toast("Папка: $path")
            }
        } catch (_: Exception) {
            toast("Папка: $path")
        }

        @JavascriptInterface
        fun checkSize(url: String, format: String, quality: String, audioLang: String) {
            viewModel.calculateSize(url, format, quality, audioLang) { sizeBytes, title ->
                val payload = JSONObject().apply {
                    put("sizeBytes", sizeBytes)
                    put("title", title)
                    put("format", format)
                    put("quality", quality)
                }
                js("onSizeResult('${escJs(payload.toString())}')")
            }
        }

        @JavascriptInterface fun showToast(msg: String) { toast(msg) }

        @JavascriptInterface
        fun deleteFile(filePath: String) {
            viewModel.deleteFile(filePath)
            js("onHistoryChanged()")
        }

        @JavascriptInterface
        fun checkUpdate() {
            viewModel.launchIO {
                try {
                    val conn = java.net.URL("https://api.github.com/repos/jozhikbeznozhek-dev/YTDow/releases/latest")
                        .openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("Accept", "application/json")
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val latest = json.optString("tag_name", "").removePrefix("v")

                    val assets = json.optJSONArray("assets") ?: org.json.JSONArray()
                    var downloadUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name", "").endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }

                    val result = JSONObject().apply {
                        put("latest", latest)
                        put("downloadUrl", downloadUrl)
                        put("current", "2.0.0")
                    }
                    js("onUpdateResult('${escJs(result.toString())}')")
                } catch (e: Exception) {
                    js("onUpdateResult('${escJs("""{"error":"${e.message?.replace("\"", "'") ?: "?"}"}""")}')")
                }
            }
        }

        @JavascriptInterface
        fun downloadUpdate(url: String) {
            viewModel.launchIO {
                try {
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 30000
                    conn.readTimeout = 60000
                    val total = conn.contentLength
                    val apkFile = File(cacheDir, "update.apk")
                    conn.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                if (total > 0) {
                                    val pct = (downloaded * 100 / total).toInt()
                                    js("onUpdateProgress($pct)")
                                }
                            }
                        }
                    }
                    val uri = FileProvider.getUriForFile(
                        this@MainActivity, "${packageName}.fileprovider", apkFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    toast("Ошибка обновления: ${e.message}")
                }
            }
        }
    }

    private fun findDownloadUri(filePath: String): Uri? {
        val fileName = File(filePath).name; if (fileName.isBlank()) return null
        contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.RELATIVE_PATH),
            "${MediaStore.Downloads.DISPLAY_NAME}=?", arrayOf(fileName), null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            while (c.moveToNext())
                if (c.getString(pathCol) in listOf("Download/YTDow/", "Download/YTDow"))
                    return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(idCol).toString())
        }
        return null
    }
}
