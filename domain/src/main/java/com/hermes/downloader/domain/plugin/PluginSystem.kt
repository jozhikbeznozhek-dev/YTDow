package com.hermes.downloader.domain.plugin

import com.hermes.downloader.domain.model.VideoMetadata

/** Metadata extraction for a specific service. */
interface MetadataProvider {
    suspend fun fetchMetadata(url: String): VideoMetadata
}

/** Extracts available formats and qualities. */
interface FormatExtractor {
    fun extractFormats(metadata: VideoMetadata): List<String>
    fun extractQualities(metadata: VideoMetadata): List<String>
}

/** Downloads media from a service. */
interface ServiceDownloader {
    suspend fun download(url: String, format: String, quality: String, outputPath: String): String
}

/** Optional thumbnail. */
interface ThumbnailProvider {
    suspend fun getThumbnailUrl(url: String): String?
}

/** Optional authentication. */
interface ServiceAuthenticator {
    suspend fun authenticate(): String?
    fun isAuthenticated(): Boolean
}

/**
 * Each supported platform implements this provider interface.
 * Register via ServiceRegistry to add support without modifying core code.
 */
interface VideoServiceProvider {
    val name: String
    val metadataProvider: MetadataProvider
    val formatExtractor: FormatExtractor
    val downloader: ServiceDownloader
    val thumbnailProvider: ThumbnailProvider?
    val authenticator: ServiceAuthenticator?

    /** Whether this provider handles the given URL. */
    fun canHandle(url: String): Boolean
}

/** Simple registry for service providers. */
object ServiceRegistry {
    private val providers = mutableListOf<VideoServiceProvider>()

    fun register(provider: VideoServiceProvider) {
        providers.add(provider)
    }

    fun findProvider(url: String): VideoServiceProvider? =
        providers.find { it.canHandle(url) }

    fun allProviders(): List<VideoServiceProvider> = providers.toList()
}
