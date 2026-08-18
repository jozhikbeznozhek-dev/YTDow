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
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.hermes.downloader.domain.queue.TaskIdFactory
import com.hermes.downloader.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
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
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = !isTrustedAppUri(request.url)
            }
            webChromeClient = WebChromeClient()
            setBackgroundColor(0xFF000000.toInt())
            addJavascriptInterface(WebAppInterface(), "Android")
        }
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.YTDowApp?.closeActiveSheet?.() === true") { closed ->
                    if (closed != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                }
            }
        })
        createChannel()
        requestNotifyPerm()
        requestLegacyStoragePerm()
        webView.loadUrl(APP_URL)
        scheduleYtDlpUpdate()
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

    private fun scheduleYtDlpUpdate() {
        mainHandler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            thread(name = "ytdlp-update") {
                YtDlpUpdateCoordinator.updateIfDue(this@MainActivity)
            }
        }, YTDLP_UPDATE_DELAY_MS)
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
                val uri = resolveDownloadUri(filePath)
                if (uri == null) {
                    toast("Файл не найден: ${file.name}")
                    return
                }
                val mime = contentResolver.getType(uri) ?: when {
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
        fun openFolder(@Suppress("UNUSED_PARAMETER") path: String) = try {
            val folderUri = DocumentsContract.buildRootUri(
                "com.android.providers.downloads.documents",
                "downloads"
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = folderUri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            try {
                startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                toast("Папка: Downloads/YTDow")
            }
        } catch (_: Exception) {
            toast("Папка: Downloads/YTDow")
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
            viewModel.deleteFile(filePath) { deleted ->
                js("onDeleteResult('${escJs(filePath)}',$deleted)")
            }
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
                var partialFile: File? = null
                var apkFile: File? = null
                try {
                    val updateUrl = URL(url)
                    check(UpdateUrlPolicy.isInitialReleaseUrl(updateUrl)) {
                        "Недопустимый адрес обновления"
                    }
                    conn = openTrustedUpdateConnection(updateUrl)
                    val statusCode = conn.responseCode
                    check(statusCode in 200..299) { "GitHub вернул HTTP $statusCode" }
                    val total = conn.contentLengthLong
                    check(total <= MAX_UPDATE_BYTES) { "APK слишком большой" }
                    val updateDirectory = File(cacheDir, "updates").apply {
                        check(mkdirs() || isDirectory) { "Не удалось создать каталог обновления" }
                    }
                    val targetApk = File(updateDirectory, "update.apk").also { apkFile = it }
                    val partialApk = File(updateDirectory, "update.apk.part").also { partialFile = it }
                    if (partialApk.exists()) check(partialApk.delete()) { "Не удалось очистить старую загрузку" }
                    conn.inputStream.use { input ->
                        partialApk.outputStream().use { output ->
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
                    check(partialApk.length() > 0) { "GitHub вернул пустой файл" }
                    if (targetApk.exists()) check(targetApk.delete()) { "Не удалось заменить старое обновление" }
                    check(partialApk.renameTo(targetApk)) { "Не удалось подготовить APK" }
                    validateUpdateApk(targetApk)
                    js("onUpdateStatus('APK загружен и проверен')")
                    mainHandler.post { preparePackageInstall(targetApk) }
                } catch (e: Exception) {
                    partialFile?.delete()
                    apkFile?.delete()
                    val message = e.message ?: "Неизвестная ошибка"
                    js("onUpdateStatus('${escJs("Ошибка обновления: $message")}',true)")
                    toast("Ошибка обновления: $message")
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    private fun openTrustedUpdateConnection(initialUrl: URL): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_UPDATE_REDIRECTS + 1) { redirectCount ->
            check(
                if (redirectCount == 0) UpdateUrlPolicy.isInitialReleaseUrl(currentUrl)
                else UpdateUrlPolicy.isTrustedRedirectUrl(currentUrl)
            ) {
                "GitHub перенаправил обновление на недоверенный адрес"
            }
            val connection = currentUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "YTDow/${BuildConfig.VERSION_NAME}")
            val status = connection.responseCode
            if (status !in 300..399) return connection

            val location = connection.getHeaderField("Location")
                ?: run {
                    connection.disconnect()
                    error("GitHub вернул перенаправление без адреса")
                }
            connection.disconnect()
            check(redirectCount < MAX_UPDATE_REDIRECTS) { "Слишком много перенаправлений обновления" }
            currentUrl = URL(currentUrl, location)
        }
        error("Слишком много перенаправлений обновления")
    }

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
        check(currentSigners.isNotEmpty() && currentSigners == updateSigners) {
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

    private fun resolveDownloadUri(filePath: String): Uri? {
        if (filePath.startsWith("content://")) {
            return Uri.parse(filePath).takeIf(::isOwnedDownloadUri)
        }
        val mediaUri = findDownloadUri(filePath)
        if (mediaUri != null) return mediaUri

        val file = runCatching { File(filePath).canonicalFile }.getOrNull() ?: return null
        if (!isOwnedLegacyFile(file) || !file.isFile) return null
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun isOwnedDownloadUri(uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            uri.scheme != "content" || uri.authority != MediaStore.AUTHORITY
        ) return false
        return contentResolver.query(
            uri,
            arrayOf(MediaStore.Downloads.RELATIVE_PATH),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst() && cursor.getString(
                cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            ).trimEnd('/') == "Download/YTDow"
        } == true
    }

    private fun isOwnedLegacyFile(file: File): Boolean {
        @Suppress("DEPRECATION")
        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "YTDow"
        ).canonicalFile
        return file.parentFile == root
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
        private const val APP_HOST = "appassets.androidplatform.net"
        private const val APP_URL = "https://$APP_HOST/assets/index.html"
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/jozhikbeznozhek-dev/YTDow/releases/latest"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val MAX_UPDATE_BYTES = 300L * 1024L * 1024L
        private const val MAX_UPDATE_REDIRECTS = 5
        private const val YTDLP_UPDATE_DELAY_MS = 3000L
    }

    private fun isTrustedAppUri(uri: Uri): Boolean =
        uri.scheme == "https" && uri.host == APP_HOST && uri.path?.startsWith("/assets/") == true

}
