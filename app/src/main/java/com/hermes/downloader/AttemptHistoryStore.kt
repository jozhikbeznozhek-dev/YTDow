package com.hermes.downloader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AttemptHistoryStore {
    private const val PREFS_NAME = "ytdow"
    private const val KEY = "attempt_history"
    private const val MAX_ENTRIES = 100

    @Synchronized
    fun start(context: Context, taskId: String, url: String, format: String, quality: String) {
        update(context, taskId, defaults(taskId, url, format, quality)) { entry ->
            entry.put("status", "queued")
            entry.put("time", entry.optLong("time").takeIf { it > 0 } ?: System.currentTimeMillis())
            entry.remove("error")
            entry.remove("finishedAt")
        }
    }

    @Synchronized
    fun complete(
        context: Context,
        taskId: String,
        url: String,
        title: String,
        format: String,
        quality: String,
        filePath: String,
        sizeBytes: Long
    ) {
        update(context, taskId, defaults(taskId, url, format, quality)) { entry ->
            entry.put("status", "completed")
            entry.put("title", title.ifBlank { url })
            entry.put("filePath", filePath)
            entry.put("sizeBytes", sizeBytes)
            entry.put("finishedAt", System.currentTimeMillis())
            entry.remove("error")
        }
    }

    @Synchronized
    fun fail(
        context: Context,
        taskId: String,
        url: String,
        title: String,
        format: String,
        quality: String,
        error: String
    ) {
        update(context, taskId, defaults(taskId, url, format, quality)) { entry ->
            entry.put("status", "error")
            entry.put("title", title.ifBlank { url })
            entry.put("error", error)
            entry.put("finishedAt", System.currentTimeMillis())
        }
    }

    @Synchronized
    fun cancel(context: Context, taskId: String) {
        update(context, taskId, JSONObject().put("taskId", taskId).put("time", System.currentTimeMillis())) { entry ->
            entry.put("status", "cancelled")
            entry.put("finishedAt", System.currentTimeMillis())
        }
    }

    @Synchronized
    fun timeout(context: Context, taskId: String) {
        update(context, taskId, JSONObject().put("taskId", taskId).put("time", System.currentTimeMillis())) { entry ->
            entry.put("status", "error")
            entry.put("error", "Система остановила длительную фоновую загрузку")
            entry.put("finishedAt", System.currentTimeMillis())
        }
    }

    @Synchronized
    fun json(context: Context): String = preferences(context).getString(KEY, "[]") ?: "[]"

    private fun defaults(taskId: String, url: String, format: String, quality: String) = JSONObject().apply {
        put("taskId", taskId)
        put("url", url)
        put("title", url)
        put("format", format)
        put("quality", quality)
        put("time", System.currentTimeMillis())
    }

    private fun update(
        context: Context,
        taskId: String,
        fallback: JSONObject,
        change: (JSONObject) -> Unit
    ) {
        val current = runCatching { JSONArray(json(context)) }.getOrElse { JSONArray() }
        var entry: JSONObject? = null
        for (index in current.length() - 1 downTo 0) {
            val candidate = current.optJSONObject(index) ?: continue
            if (candidate.optString("taskId") == taskId) {
                entry = candidate
                break
            }
        }
        val target = entry ?: fallback.also { current.put(it) }
        change(target)

        val trimmed = JSONArray()
        val start = maxOf(0, current.length() - MAX_ENTRIES)
        for (index in start until current.length()) trimmed.put(current[index])
        preferences(context).edit().putString(KEY, trimmed.toString()).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
