package com.hermes.downloader.domain.queue

/**
 * Formal Download State Machine.
 * Valid transitions are enforced — no illegal state jumps.
 */
enum class DownloadState {
    QUEUED, PREPARING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED;

    fun canTransitionTo(target: DownloadState): Boolean = when (this to target) {
        QUEUED to PREPARING -> true
        QUEUED to CANCELLED -> true
        PREPARING to DOWNLOADING -> true
        PREPARING to FAILED -> true
        PREPARING to CANCELLED -> true
        DOWNLOADING to PAUSED -> true
        DOWNLOADING to COMPLETED -> true
        DOWNLOADING to FAILED -> true
        DOWNLOADING to CANCELLED -> true
        PAUSED to QUEUED -> true      // resume → back to queue
        PAUSED to CANCELLED -> true
        FAILED to QUEUED -> true      // retry
        FAILED to CANCELLED -> true
        COMPLETED to CANCELLED -> false  // terminal
        CANCELLED to QUEUED -> false     // terminal
        else -> false
    }

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
    val isActive: Boolean get() = this == QUEUED || this == PREPARING || this == DOWNLOADING || this == PAUSED
}
