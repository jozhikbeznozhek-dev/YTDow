package com.hermes.downloader

object VersionComparator {
    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = parse(latest) ?: return false
        val currentParts = parse(current) ?: return false
        val width = maxOf(latestParts.size, currentParts.size)
        for (index in 0 until width) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val normalized = version.trim().removePrefix("v").substringBefore('-')
        if (normalized.isBlank()) return null
        return normalized.split('.').map { part -> part.toIntOrNull() ?: return null }
    }
}
