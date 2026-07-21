package com.hermes.downloader.domain.queue

import java.util.UUID

/** Creates task identifiers owned by the native download pipeline. */
object TaskIdFactory {
    fun newId(): String = UUID.randomUUID().toString()
}
