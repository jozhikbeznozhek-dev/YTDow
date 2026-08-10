package com.hermes.downloader

import java.net.URI

/** URL and option boundary for the local WebView bridge. */
internal object AppUrlPolicy {
    private const val APP_HOST = "appassets.androidplatform.net"
    private val downloadFormats = setOf("mp3", "mp4")
    private val downloadQualities = setOf("best", "2160p", "1440p", "1080p", "720p", "480p")
    private val audioLanguage = Regex("[A-Za-z0-9._-]{0,32}")

    fun isTrustedAssetUrl(rawUrl: String): Boolean = parse(rawUrl)?.let { uri ->
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(APP_HOST, ignoreCase = true) &&
            uri.rawUserInfo == null &&
            uri.port == -1 &&
            uri.normalize().rawPath?.startsWith("/assets/") == true
    } == true

    fun isSafeExternalWebUrl(rawUrl: String): Boolean = parse(rawUrl)?.let { uri ->
        uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null
    } == true

    fun isSupportedDownloadUrl(rawUrl: String): Boolean = isSafeExternalWebUrl(rawUrl)

    fun areSupportedDownloadOptions(format: String, quality: String, language: String): Boolean =
        format in downloadFormats && quality in downloadQualities && audioLanguage.matches(language)

    private fun parse(rawUrl: String): URI? = runCatching { URI(rawUrl) }.getOrNull()
}
