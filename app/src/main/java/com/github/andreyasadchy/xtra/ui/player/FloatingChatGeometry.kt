package com.github.andreyasadchy.xtra.ui.player

import kotlin.math.min
import kotlin.math.roundToInt

internal data class FloatingChatPosition(val x: Float = 1f, val y: Float = 0.1f, val width: Float = 0.32f, val height: Float = 0.68f)
internal data class FloatingChatRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** Normalized geometry stays usable after a rotation, resize or density change. */
internal object FloatingChatGeometry {
    private fun Float.safe(fallback: Float) = if (isFinite()) this else fallback

    fun fit(position: FloatingChatPosition, viewportWidth: Int, viewportHeight: Int, density: Float): FloatingChatRect {
        val vw = viewportWidth.coerceAtLeast(1)
        val vh = viewportHeight.coerceAtLeast(1)
        val scale = density.safe(1f).coerceIn(0.5f, 8f)
        val minWidth = min((192 * scale).roundToInt(), vw)
        val minHeight = min((112 * scale).roundToInt(), vh)
        val width = (position.width.safe(0.32f).coerceIn(0f, 1f) * vw).roundToInt()
            .coerceIn(minWidth, maxOf(minWidth, (vw * 0.75f).roundToInt()))
        val height = (position.height.safe(0.68f).coerceIn(0f, 1f) * vh).roundToInt()
            .coerceIn(minHeight, maxOf(minHeight, (vh * 0.9f).roundToInt()))
        return FloatingChatRect(
            ((vw - width) * position.x.safe(1f).coerceIn(0f, 1f)).roundToInt(),
            ((vh - height) * position.y.safe(0.1f).coerceIn(0f, 1f)).roundToInt(), width, height,
        )
    }

    fun fromRect(rect: FloatingChatRect, viewportWidth: Int, viewportHeight: Int): FloatingChatPosition {
        val vw = viewportWidth.coerceAtLeast(1)
        val vh = viewportHeight.coerceAtLeast(1)
        return FloatingChatPosition(
            if (vw > rect.width) rect.left.toFloat() / (vw - rect.width) else 0f,
            if (vh > rect.height) rect.top.toFloat() / (vh - rect.height) else 0f,
            rect.width.toFloat() / vw, rect.height.toFloat() / vh,
        )
    }

    fun shouldFloat(selected: Boolean, portrait: Boolean, maximized: Boolean, chatOpen: Boolean,
                    pictureInPicture: Boolean, videoVisible: Boolean, keyboardShown: Boolean): Boolean =
        selected && !portrait && maximized && chatOpen && !pictureInPicture && videoVisible && !keyboardShown
}
