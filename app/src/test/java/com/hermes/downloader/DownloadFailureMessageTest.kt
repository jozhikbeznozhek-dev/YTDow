package com.hermes.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFailureMessageTest {
    @Test
    fun `explains dns failures without exposing a raw trace`() {
        val message = DownloadFailureMessage.from(
            "WARNING: retrying\nERROR: TransportError: [Errno 7] No address associated with hostname"
        )

        assertTrue(message.startsWith("Не удалось найти YouTube"))
        assertFalse(message.contains("TransportError"))
    }

    @Test
    fun `explains tls timeouts and notes ipv4 fallback`() {
        val message = DownloadFailureMessage.from(
            "WARNING: retrying\nERROR: _ssl.c:993: The handshake operation timed out"
        )

        assertTrue(message.startsWith("YouTube не ответил вовремя"))
        assertTrue(message.contains("IPv4"))
    }

    @Test
    fun `keeps only the final yt dlp error for unknown failures`() {
        val message = DownloadFailureMessage.from(
            "WARNING: old version\nERROR: Unsupported URL: https://example.com/test"
        )

        assertEquals("Unsupported URL: https://example.com/test", message)
    }

    @Test
    fun `uses a stable fallback for an empty exception`() {
        assertEquals("Ошибка загрузки", DownloadFailureMessage.from(null))
    }
}
