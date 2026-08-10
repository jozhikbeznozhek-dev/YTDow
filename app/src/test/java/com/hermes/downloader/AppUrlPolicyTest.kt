package com.hermes.downloader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUrlPolicyTest {
    @Test
    fun `trusts only normalized local app assets`() {
        assertTrue(AppUrlPolicy.isTrustedAssetUrl("https://appassets.androidplatform.net/assets/index.html"))
        assertTrue(AppUrlPolicy.isTrustedAssetUrl("HTTPS://APPASSETS.ANDROIDPLATFORM.NET/assets/legal.html"))
        assertFalse(AppUrlPolicy.isTrustedAssetUrl("https://appassets.androidplatform.net/assets/../secret"))
        assertFalse(AppUrlPolicy.isTrustedAssetUrl("https://appassets.androidplatform.net.evil.example/assets/index.html"))
        assertFalse(AppUrlPolicy.isTrustedAssetUrl("https://user@appassets.androidplatform.net/assets/index.html"))
        assertFalse(AppUrlPolicy.isTrustedAssetUrl("http://appassets.androidplatform.net/assets/index.html"))
    }

    @Test
    fun `allows external browser only for ordinary web URLs`() {
        assertTrue(AppUrlPolicy.isSafeExternalWebUrl("https://github.com/jozhikbeznozhek-dev/YTDow"))
        assertTrue(AppUrlPolicy.isSafeExternalWebUrl("http://example.com/video"))
        assertFalse(AppUrlPolicy.isSafeExternalWebUrl("javascript:alert(1)"))
        assertFalse(AppUrlPolicy.isSafeExternalWebUrl("file:///data/local/tmp/payload"))
        assertFalse(AppUrlPolicy.isSafeExternalWebUrl("https://user:password@example.com/video"))
    }

    @Test
    fun `accepts only supported download options`() {
        assertTrue(AppUrlPolicy.areSupportedDownloadOptions("mp4", "1080p", ""))
        assertTrue(AppUrlPolicy.areSupportedDownloadOptions("mp3", "best", "en-US"))
        assertFalse(AppUrlPolicy.areSupportedDownloadOptions("webm", "1080p", ""))
        assertFalse(AppUrlPolicy.areSupportedDownloadOptions("mp4", "9999p", ""))
        assertFalse(AppUrlPolicy.areSupportedDownloadOptions("mp4", "best", "en][height>0"))
    }
}
