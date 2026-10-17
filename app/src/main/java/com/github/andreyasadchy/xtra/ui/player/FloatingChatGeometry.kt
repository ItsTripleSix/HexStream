package com.github.andreyasadchy.xtra.ui.player

import kotlin.math.min
import kotlin.math.roundToInt

internal data class FloatingChatRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** A frameless right-hand chat column over the full-size video. */
internal object FloatingChatGeometry {
    private fun Float.safe(fallback: Float) = if (isFinite()) this else fallback

    fun fit(viewportWidth: Int, viewportHeight: Int, density: Float, leftSide: Boolean = false): FloatingChatRect {
        val vw = viewportWidth.coerceAtLeast(1)
        val vh = viewportHeight.coerceAtLeast(1)
        val scale = density.safe(1f).coerceIn(0.5f, 8f)
        val minWidth = min((192 * scale).roundToInt(), vw)
        val width = (0.32f * vw).roundToInt()
            .coerceIn(minWidth, maxOf(minWidth, (vw * 0.75f).roundToInt()))
        return FloatingChatRect(if (leftSide) 0 else vw - width, 0, width, vh)
    }

    fun shouldFloat(selected: Boolean, portrait: Boolean, maximized: Boolean, chatOpen: Boolean,
                    pictureInPicture: Boolean, videoVisible: Boolean): Boolean =
        selected && !portrait && maximized && chatOpen && !pictureInPicture && videoVisible
}
