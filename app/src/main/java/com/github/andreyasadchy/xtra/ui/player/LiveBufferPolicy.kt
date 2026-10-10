package com.github.andreyasadchy.xtra.ui.player

/** Returns null when the user's original load-control settings should decide. */
internal object LiveBufferPolicy {
    fun shouldStart(lowDelay: Boolean, live: Boolean, bufferedUs: Long, speed: Float, rebuffering: Boolean): Boolean? {
        if (!lowDelay || !live) return null
        val safeSpeed = speed.takeIf { it.isFinite() && it > 0f } ?: 1f
        val requiredUs = if (rebuffering) 1_000_000L else 500_000L
        return bufferedUs.toDouble() / safeSpeed >= requiredUs
    }
}
