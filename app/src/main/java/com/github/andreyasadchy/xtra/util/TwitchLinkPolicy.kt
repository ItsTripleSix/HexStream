package com.github.andreyasadchy.xtra.util

import java.net.URI
import java.util.Locale

object TwitchLinkPolicy {
    fun isTrusted(url: String): Boolean = try {
        val uri = URI(url)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)
        scheme in listOf("http", "https") && host != null &&
            (host == "twitch.tv" || host.endsWith(".twitch.tv")) &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == if (scheme == "https") 443 else 80)
    } catch (_: Exception) {
        false
    }
}
