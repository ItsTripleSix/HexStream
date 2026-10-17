/*
 * Floating chat adapts ThystTV's movable/resizable, reparented-chat approach.
 * Credit: tzii (Simone Perini) and ThystTV contributors, https://github.com/tzii/ThystTV
 * Reference: ae3b1c6c784c1ebc4b8096c3a8df5791827787e4. GNU AGPL v3; see CREDITS.md.
 */
package com.github.andreyasadchy.xtra.ui.player

import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import kotlin.math.max
import com.github.andreyasadchy.xtra.databinding.FloatingChatBinding

/** Owns views only; never creates a second chat fragment or connection. */
internal class FloatingChatController(
    private val host: FrameLayout,
    private val dock: FrameLayout,
    private val prefs: SharedPreferences,
    private val styleChat: (Boolean, Boolean) -> Unit,
    private val controls: View? = null,
) {
    companion object {
        const val ENABLED = "hex_floating_chat"
        const val OPACITY = "hex_floating_chat_opacity"
        const val CONTRAST = "hex_floating_chat_contrast"
        const val SIDE = "hex_fullscreen_chat_side"
    }

    private val binding = FloatingChatBinding.inflate(LayoutInflater.from(host.context), host, false)
    internal val overlay: View get() = binding.root
    private var chat: View? = null
    private var originalParams: ViewGroup.LayoutParams? = null
    private val density get() = host.resources.displayMetrics.density
    private var bottomInset = 0
    var active = false
        private set
    private val hostLayoutListener = View.OnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
        if (active && (r - l != or - ol || b - t != ob - ot)) applyBounds()
    }

    init {
        host.addView(binding.root, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START))
        binding.root.playerControls = controls
        host.addOnLayoutChangeListener(hostLayoutListener)
    }

    fun update(enabled: Boolean, chatView: View?) {
        if (chat !== chatView) {
            restoreChat()
            chat = chatView
            originalParams = chatView?.layoutParams
        }
        active = enabled && chatView != null
        val target = if (active) binding.floatingMessages else dock
        chatView?.let { view ->
            if (view.parent !== target) {
                (view.parent as? ViewGroup)?.removeView(view)
                target.addView(view, if (active) FrameLayout.LayoutParams(-1, -1) else originalParams ?: FrameLayout.LayoutParams(-1, -1))
            }
        }
        binding.root.isVisible = active
        if (active) applyBounds()
        updateInputInsets()
        applyStyle()
    }

    fun setBottomInset(pixels: Int) {
        bottomInset = pixels.coerceAtLeast(0)
        updateInputInsets()
    }

    fun updateInputInsets() {
        val controlSpace = if (controls?.isVisible == true) (64 * density).toInt() else 0
        binding.root.updatePadding(bottom = max(bottomInset, controlSpace))
    }

    private fun applyStyle() {
        val opacity = prefs.getInt(OPACITY, 0).coerceIn(0, 100)
        binding.floatingMessages.setBackgroundColor(ColorUtils.setAlphaComponent(Color.BLACK, opacity * 255 / 100))
        val contrast = prefs.getBoolean(CONTRAST, true)
        styleChat(active, contrast)
    }

    private fun applyBounds() {
        if (host.width <= 0 || host.height <= 0) return
        val rect = FloatingChatGeometry.fit(host.width, host.height, density, prefs.getString(SIDE, "right") == "left")
        binding.root.updateLayoutParams<FrameLayout.LayoutParams> {
            width = rect.width; height = rect.height; leftMargin = rect.left; topMargin = rect.top
            marginStart = if (host.layoutDirection == View.LAYOUT_DIRECTION_RTL) host.width - rect.width - rect.left else rect.left
        }
    }

    private fun restoreChat() {
        chat?.takeIf { it.parent === binding.floatingMessages }?.let {
            binding.floatingMessages.removeView(it)
            dock.addView(it, originalParams ?: FrameLayout.LayoutParams(-1, -1))
        }
    }

    fun dispose() {
        restoreChat()
        styleChat(false, false)
        binding.root.playerControls = null
        host.removeOnLayoutChangeListener(hostLayoutListener)
        host.removeView(binding.root)
        active = false
        chat = null
    }
}
