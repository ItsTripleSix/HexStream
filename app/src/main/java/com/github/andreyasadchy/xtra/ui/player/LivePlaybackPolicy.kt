package com.github.andreyasadchy.xtra.ui.player

/** Live-only preset. Disable it to use Xtra's existing advanced playback settings. */
internal object LivePlaybackPolicy {
    data class Settings(val targetMs: Long, val minSpeed: Float?, val maxSpeed: Float?)
    fun settings(lowDelay: Boolean, target: String?, min: String?, max: String?): Settings {
        if (lowDelay) return Settings(1000L, .97f, 1.08f)
        val minimum = min?.toFloatOrNull()?.takeIf { it.isFinite() && it in .5f..2f }
        val maximum = max?.toFloatOrNull()?.takeIf { it.isFinite() && it in .5f..2f }
        return Settings(target?.toLongOrNull()?.takeIf { it >= 0 } ?: 2000L,
            minimum?.takeIf { maximum == null || it <= maximum },
            maximum?.takeIf { minimum == null || it >= minimum })
    }
}
