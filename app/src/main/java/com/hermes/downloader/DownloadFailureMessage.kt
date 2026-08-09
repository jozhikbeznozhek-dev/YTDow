package com.hermes.downloader

object DownloadFailureMessage {
    fun from(rawMessage: String?): String {
        val raw = rawMessage.orEmpty()
        val normalized = raw.lowercase()

        return when {
            "no address associated with hostname" in normalized ||
                "temporary failure in name resolution" in normalized ->
                "Не удалось найти YouTube в сети. Проверьте VPN, частный DNS или переключите Wi-Fi/мобильную сеть."

            "handshake operation timed out" in normalized ||
                "connection timed out" in normalized ||
                "read timed out" in normalized ->
                "YouTube не ответил вовремя. Приложение уже попробовало IPv4 — переключите Wi-Fi/мобильную сеть или проверьте VPN."

            "network is unreachable" in normalized ||
                "failed to establish a new connection" in normalized ->
                "Нет доступа к интернету. Проверьте подключение и повторите загрузку."

            else -> raw.lineSequence()
                .map(String::trim)
                .lastOrNull { it.startsWith("ERROR:", ignoreCase = true) }
                ?.removePrefix("ERROR:")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: raw.trim().takeIf(String::isNotEmpty)
                ?: "Ошибка загрузки"
        }
    }
}
