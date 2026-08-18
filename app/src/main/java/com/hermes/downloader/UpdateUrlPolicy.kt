package com.hermes.downloader

import java.net.URL

/** Network boundary for self-update downloads. */
internal object UpdateUrlPolicy {
    private const val RELEASE_PATH_PREFIX =
        "/jozhikbeznozhek-dev/YTDow/releases/download/"

    fun isInitialReleaseUrl(url: URL): Boolean =
        isHttps(url) && isRepositoryReleaseUrl(url)

    fun isTrustedRedirectUrl(url: URL): Boolean =
        isHttps(url) && (
            isRepositoryReleaseUrl(url) ||
                url.host.equals("release-assets.githubusercontent.com", ignoreCase = true)
            )

    private fun isHttps(url: URL): Boolean =
        url.protocol.equals("https", ignoreCase = true)

    private fun isRepositoryReleaseUrl(url: URL): Boolean =
        url.host.equals("github.com", ignoreCase = true) &&
            url.path.startsWith(RELEASE_PATH_PREFIX)
}
