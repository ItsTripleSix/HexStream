package com.github.andreyasadchy.xtra.ui.player

import kotlin.math.abs

/** Side zones are deliberately separate from Xtra's center drag-to-minimize gesture. */
internal object SwipeControlPolicy {
    enum class Control { BRIGHTNESS, VOLUME }
    fun control(x: Float, y: Float, width: Float, height: Float, edge: Float,
                brightness: Boolean, volume: Boolean): Control? {
        if (width <= 0 || height <= edge * 2 || y < edge || y > height - edge || x < edge || x > width - edge) return null
        return when {
            x < width * .25f && brightness -> Control.BRIGHTNESS
            x > width * .75f && volume -> Control.VOLUME
            else -> null
        }
    }
    fun vertical(dx: Float, dy: Float, threshold: Float) = abs(dy) >= threshold && abs(dy) > abs(dx) * 1.5f
    fun level(start: Float, dy: Float, height: Float) = (start - dy / height.coerceAtLeast(1f)).coerceIn(0f, 1f)
}
