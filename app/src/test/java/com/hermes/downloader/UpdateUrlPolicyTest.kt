package com.hermes.downloader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class UpdateUrlPolicyTest {
    @Test
    fun `accepts only this repository release as initial URL`() {
        assertTrue(
            UpdateUrlPolicy.isInitialReleaseUrl(
                URL("https://github.com/jozhikbeznozhek-dev/YTDow/releases/download/v2.3.0/YTDow.apk")
            )
        )
        assertFalse(
            UpdateUrlPolicy.isInitialReleaseUrl(
                URL("https://github.com/attacker/YTDow/releases/download/v2.3.0/YTDow.apk")
            )
        )
        assertFalse(
            UpdateUrlPolicy.isInitialReleaseUrl(
                URL("http://github.com/jozhikbeznozhek-dev/YTDow/releases/download/v2.3.0/YTDow.apk")
            )
        )
    }

    @Test
    fun `accepts GitHub release asset redirect but rejects lookalike hosts`() {
        assertTrue(
            UpdateUrlPolicy.isTrustedRedirectUrl(
                URL("https://release-assets.githubusercontent.com/github-production-release-asset/123/file")
            )
        )
        assertFalse(
            UpdateUrlPolicy.isTrustedRedirectUrl(
                URL("https://release-assets.githubusercontent.com.evil.example/file")
            )
        )
        assertFalse(
            UpdateUrlPolicy.isTrustedRedirectUrl(URL("https://objects.githubusercontent.com/file"))
        )
    }
}
