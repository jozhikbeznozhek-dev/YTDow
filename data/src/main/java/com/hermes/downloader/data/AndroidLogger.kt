package com.hermes.downloader.data

import com.hermes.downloader.core.Logger

class AndroidLogger : Logger {
    override fun d(tag: String, msg: String) { android.util.Log.d(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) android.util.Log.e(tag, msg, throwable) else android.util.Log.e(tag, msg)
    }
    override fun w(tag: String, msg: String) { android.util.Log.w(tag, msg) }
    override fun i(tag: String, msg: String) { android.util.Log.i(tag, msg) }
}
