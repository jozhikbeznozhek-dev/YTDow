package com.hermes.downloader.core

/** Abstract logger — swap android.util.Log ↔ Timber easily. */
interface Logger {
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String, throwable: Throwable? = null)
    fun w(tag: String, msg: String)
    fun i(tag: String, msg: String)
}
