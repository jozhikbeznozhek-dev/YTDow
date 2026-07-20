package com.hermes.downloader.core

/** Abstract logger — swap android.util.Log ↔ Timber easily. */
interface Logger {
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String, throwable: Throwable? = null)
    fun w(tag: String, msg: String)
    fun i(tag: String, msg: String)
}

/** Default implementation using android.util.Log. */
class AndroidLogger : Logger {
    override fun d(tag: String, msg: String) { android.util.Log.d(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) android.util.Log.e(tag, msg, throwable) else android.util.Log.e(tag, msg)
    }
    override fun w(tag: String, msg: String) { android.util.Log.w(tag, msg) }
    override fun i(tag: String, msg: String) { android.util.Log.i(tag, msg) }
}
