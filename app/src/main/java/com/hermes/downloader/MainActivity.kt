package com.hermes.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.hermes.downloader.domain.queue.TaskIdFactory
import com.hermes.downloader.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import android.util.Log
import kotlin.concurrent.thread

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private var isReceiverRegistered = false
    private var pendingUpdateApk: File? = null

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
            setBackgroundColor(0xFF000000.toInt())
            addJavascriptInterface(WebAppInterface(), "Android")
        }
        setContentView(webView)
        createChannel()
        requestNotifyPerm()
        requestLegacyStoragePerm()
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onStart() { super.onStart(); register() }

    override fun onResume() {
        super.onResume()
        val apk = pendingUpdateApk ?: return
        if (canInstallPackages()) {
            pendingUpdateApk = null
            launchPackageInstaller(apk)
        }
    }

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

    private fun requestLegacyStoragePerm() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1002)
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
        @JavascriptInterface fun getAttemptHistory(): String = AttemptHistoryStore.json(this@MainActivity)
        @JavascriptInterface fun getDownloadDir(): String = viewModel.getSavePath()
        @JavascriptInterface fun getAppVersion(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun startDownload(
            url: String,
            format: String,
            quality: String,
            audioLang: String
        ): String {
            val nativeTaskId = TaskIdFactory.newId()
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
                putExtra(DownloadService.EXTRA_TASK_ID, nativeTaskId)
                putExtra(DownloadService.EXTRA_AUDIO_LANG, audioLang)
            }
            AttemptHistoryStore.start(this@MainActivity, nativeTaskId, url, format, quality)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            return nativeTaskId
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
                var conn: HttpURLConnection? = null
                try {
                    conn = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                    conn.setRequestProperty("User-Agent", "YTDow/${BuildConfig.VERSION_NAME}")
                    val statusCode = conn.responseCode
                    check(statusCode in 200..299) { "GitHub вернул HTTP $statusCode" }
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
                        put("current", BuildConfig.VERSION_NAME)
                        put("hasUpdate", VersionComparator.isNewer(latest, BuildConfig.VERSION_NAME))
                    }
                    js("onUpdateResult('${escJs(result.toString())}')")
                } catch (e: Exception) {
                    val result = JSONObject().put("error", e.message ?: "Не удалось проверить обновление")
                    js("onUpdateResult('${escJs(result.toString())}')")
                } finally {
                    conn?.disconnect()
                }
            }
        }

        @JavascriptInterface
        fun downloadUpdate(url: String) {
            viewModel.launchIO {
                var conn: HttpURLConnection? = null
                try {
                    val updateUrl = URL(url)
                    check(isTrustedUpdateUrl(updateUrl)) { "Недопустимый адрес обновления" }
                    conn = updateUrl.openConnection() as HttpURLConnection
                    conn.connectTimeout = 30000
                    conn.readTimeout = 60000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("User-Agent", "YTDow/${BuildConfig.VERSION_NAME}")
                    val statusCode = conn.responseCode
                    check(statusCode in 200..299) { "GitHub вернул HTTP $statusCode" }
                    val total = conn.contentLengthLong
                    check(total <= MAX_UPDATE_BYTES) { "APK слишком большой" }
                    val apkFile = File(cacheDir, "update.apk")
                    val partialFile = File(cacheDir, "update.apk.part")
                    conn.inputStream.use { input ->
                        partialFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                check(downloaded <= MAX_UPDATE_BYTES) { "APK слишком большой" }
                                if (total > 0) {
                                    val pct = (downloaded * 100 / total).toInt()
                                    js("onUpdateProgress($pct)")
                                }
                            }
                        }
                    }
                    check(partialFile.length() > 0) { "GitHub вернул пустой файл" }
                    if (apkFile.exists()) check(apkFile.delete()) { "Не удалось заменить старое обновление" }
                    check(partialFile.renameTo(apkFile)) { "Не удалось подготовить APK" }
                    validateUpdateApk(apkFile)
                    js("onUpdateStatus('APK загружен и проверен')")
                    mainHandler.post { preparePackageInstall(apkFile) }
                } catch (e: Exception) {
                    val message = e.message ?: "Неизвестная ошибка"
                    js("onUpdateStatus('${escJs("Ошибка обновления: $message")}',true)")
                    toast("Ошибка обновления: $message")
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    private fun isTrustedUpdateUrl(url: URL): Boolean =
        url.protocol.equals("https", ignoreCase = true) &&
            url.host.equals("github.com", ignoreCase = true) &&
            url.path.startsWith("/jozhikbeznozhek-dev/YTDow/releases/download/")

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun preparePackageInstall(apkFile: File) {
        if (!canInstallPackages()) {
            pendingUpdateApk = apkFile
            js("onUpdateStatus('Разрешите установку обновлений для YTDow, затем вернитесь в приложение')")
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                })
            } catch (e: Exception) {
                pendingUpdateApk = null
                js("onUpdateStatus('${escJs("Не удалось открыть разрешение: ${e.message ?: "ошибка"}")}',true)")
            }
            return
        }
        launchPackageInstaller(apkFile)
    }

    private fun launchPackageInstaller(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
            js("onUpdateStatus('Открываем установщик Android…')")
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            js("onUpdateStatus('${escJs("Не удалось открыть установщик: ${e.message ?: "ошибка"}")}',true)")
        }
    }

    @Suppress("DEPRECATION")
    private fun validateUpdateApk(apkFile: File) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val updateInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: error("Файл не является Android APK")
        check(updateInfo.packageName == packageName) { "APK предназначен для другого приложения" }

        val currentInfo = packageManager.getPackageInfo(packageName, flags)
        val updateCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) updateInfo.longVersionCode
            else updateInfo.versionCode.toLong()
        val currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) currentInfo.longVersionCode
            else currentInfo.versionCode.toLong()
        check(updateCode > currentCode) { "Версия APK не новее установленной" }

        val currentSigners = signerDigests(currentInfo)
        val updateSigners = signerDigests(updateInfo)
        check(currentSigners.isNotEmpty() && currentSigners.intersect(updateSigners).isNotEmpty()) {
            "Подпись APK не совпадает с установленным приложением"
        }
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
        } else {
            info.signatures ?: emptyArray()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }.toSet()
    }

    private fun findDownloadUri(filePath: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
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

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/jozhikbeznozhek-dev/YTDow/releases/latest"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val MAX_UPDATE_BYTES = 300L * 1024L * 1024L
    }

}
