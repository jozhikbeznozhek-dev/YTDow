package com.hermes.downloader.data.repository

import com.hermes.downloader.domain.repository.UpdateInfo
import com.hermes.downloader.domain.repository.UpdateRepository
import org.json.JSONObject

class UpdateRepositoryImpl(
    private val currentVersion: String
) : UpdateRepository {
    override suspend fun checkForUpdate(): UpdateInfo {
        return try {
            val conn = java.net.URL("https://api.github.com/repos/jozhikbeznozhek-dev/YTDow/releases/latest")
                .openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/json")
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val latest = json.optString("tag_name", "").removePrefix("v")
            val assets = json.optJSONArray("assets") ?: org.json.JSONArray()
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                if (assets.getJSONObject(i).optString("name", "").endsWith(".apk")) {
                    downloadUrl = assets.getJSONObject(i).optString("browser_download_url", "")
                    break
                }
            }
            UpdateInfo(latest, downloadUrl, currentVersion)
        } catch (e: Exception) {
            UpdateInfo("", "", currentVersion)
        }
    }
}
