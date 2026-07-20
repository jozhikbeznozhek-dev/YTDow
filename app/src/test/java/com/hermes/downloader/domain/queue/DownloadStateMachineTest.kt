package com.hermes.downloader.domain.queue

import org.junit.Assert.*
import org.junit.Test

class DownloadStateMachineTest {

    @Test
    fun `queued can transition to preparing`() {
        assertTrue(DownloadState.QUEUED.canTransitionTo(DownloadState.PREPARING))
    }

    @Test
    fun `queued can transition to cancelled`() {
        assertTrue(DownloadState.QUEUED.canTransitionTo(DownloadState.CANCELLED))
    }

    @Test
    fun `queued cannot transition to completed directly`() {
        assertFalse(DownloadState.QUEUED.canTransitionTo(DownloadState.COMPLETED))
    }

    @Test
    fun `downloading can transition to paused`() {
        assertTrue(DownloadState.DOWNLOADING.canTransitionTo(DownloadState.PAUSED))
    }

    @Test
    fun `downloading can transition to completed`() {
        assertTrue(DownloadState.DOWNLOADING.canTransitionTo(DownloadState.COMPLETED))
    }

    @Test
    fun `downloading can transition to failed`() {
        assertTrue(DownloadState.DOWNLOADING.canTransitionTo(DownloadState.FAILED))
    }

    @Test
    fun `failed can transition to queued for retry`() {
        assertTrue(DownloadState.FAILED.canTransitionTo(DownloadState.QUEUED))
    }

    @Test
    fun `completed is terminal — no transitions out`() {
        assertTrue(DownloadState.COMPLETED.isTerminal)
        DownloadState.entries.forEach { target ->
            if (target != DownloadState.COMPLETED) {
                assertFalse("COMPLETED -> $target should be false",
                    DownloadState.COMPLETED.canTransitionTo(target))
            }
        }
    }

    @Test
    fun `cancelled is terminal`() {
        assertTrue(DownloadState.CANCELLED.isTerminal)
        DownloadState.entries.forEach { target ->
            if (target != DownloadState.CANCELLED) {
                assertFalse("CANCELLED -> $target should be false",
                    DownloadState.CANCELLED.canTransitionTo(target))
            }
        }
    }

    @Test
    fun `active states return true for isActive`() {
        assertTrue(DownloadState.QUEUED.isActive)
        assertTrue(DownloadState.PREPARING.isActive)
        assertTrue(DownloadState.DOWNLOADING.isActive)
        assertTrue(DownloadState.PAUSED.isActive)
    }

    @Test
    fun `terminal states return false for isActive`() {
        assertFalse(DownloadState.COMPLETED.isActive)
        assertFalse(DownloadState.CANCELLED.isActive)
        assertFalse(DownloadState.FAILED.isActive)
    }
}
