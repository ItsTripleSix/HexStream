/*
 * Floating chat adapts ThystTV's movable/resizable, reparented-chat approach.
 * Credit: tzii (Simone Perini) and ThystTV contributors, https://github.com/tzii/ThystTV
 * Reference: ae3b1c6c784c1ebc4b8096c3a8df5791827787e4. GNU AGPL v3; see CREDITS.md.
 */
package com.github.andreyasadchy.xtra.ui.player

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FloatingChatBinding
import com.github.andreyasadchy.xtra.util.getAlertDialogBuilder
import kotlin.math.abs

/** Owns views only; never creates a second chat fragment or connection. */
internal class FloatingChatController(
    private val host: FrameLayout,
    private val dock: FrameLayout,
    private val prefs: SharedPreferences,
    private val hideChat: () -> Unit,
    private val styleChat: (Boolean, Boolean) -> Unit,
) {
    companion object {
        const val ENABLED = "hex_floating_chat"
        const val OPACITY = "hex_floating_chat_opacity"
        const val CONTRAST = "hex_floating_chat_contrast"
        private const val X = "hex_floating_chat_x"
        private const val Y = "hex_floating_chat_y"
        private const val WIDTH = "hex_floating_chat_width"
        private const val HEIGHT = "hex_floating_chat_height"
    }

    private val binding = FloatingChatBinding.inflate(LayoutInflater.from(host.context), host, false)
    internal val overlay: View get() = binding.root
    private var chat: View? = null
    private var originalParams: ViewGroup.LayoutParams? = null
    private var position = FloatingChatPosition(prefs.getFloat(X, 1f), prefs.getFloat(Y, 0.1f), prefs.getFloat(WIDTH, 0.32f), prefs.getFloat(HEIGHT, 0.68f))
    private val density get() = host.resources.displayMetrics.density
    var active = false
        private set
    private val hostLayoutListener = View.OnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
        if (active && (r - l != or - ol || b - t != ob - ot)) applyBounds()
    }

    init {
        host.addView(binding.root, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START))
        host.addOnLayoutChangeListener(hostLayoutListener)
        setupGesture(binding.moveChat, resize = false)
        setupGesture(binding.resizeChat, resize = true)
        binding.closeChat.setOnClickListener { hideChat() }
        binding.contrastChat.setOnClickListener {
            prefs.edit { putBoolean(CONTRAST, !prefs.getBoolean(CONTRAST, true)) }
            applyStyle()
        }
        binding.moveChat.setOnClickListener {
            host.context.getAlertDialogBuilder().setTitle(R.string.hex_floating_move)
                .setItems(R.array.hex_floating_move_actions) { _, which ->
                    position = when (which) {
                        0 -> position.copy(x = position.x - 0.1f)
                        1 -> position.copy(x = position.x + 0.1f)
                        2 -> position.copy(y = position.y - 0.1f)
                        3 -> position.copy(y = position.y + 0.1f)
                        else -> FloatingChatPosition()
                    }
                    applyBounds(); savePosition()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        binding.resizeChat.setOnClickListener {
            position = if (position.width < 0.45f) position.copy(width = 0.55f, height = 0.8f)
                else position.copy(width = 0.32f, height = 0.68f)
            applyBounds(); savePosition()
        }
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
        applyStyle()
    }

    private fun applyStyle() {
        val opacity = prefs.getInt(OPACITY, 35).coerceIn(0, 100)
        binding.floatingMessages.setBackgroundColor(ColorUtils.setAlphaComponent(Color.BLACK, opacity * 255 / 100))
        val contrast = prefs.getBoolean(CONTRAST, true)
        binding.contrastChat.isSelected = contrast
        binding.contrastChat.contentDescription = host.context.getString(
            if (contrast) R.string.hex_floating_contrast_on else R.string.hex_floating_contrast_off,
        )
        styleChat(active, contrast)
    }

    private fun applyBounds() {
        if (host.width <= 0 || host.height <= 0) return
        val rect = FloatingChatGeometry.fit(position, host.width, host.height, density)
        binding.root.updateLayoutParams<FrameLayout.LayoutParams> {
            width = rect.width; height = rect.height; leftMargin = rect.left; topMargin = rect.top
            marginStart = if (host.layoutDirection == View.LAYOUT_DIRECTION_RTL) host.width - rect.width - rect.left else rect.left
        }
    }

    private fun savePosition() {
        position = FloatingChatGeometry.fromRect(FloatingChatGeometry.fit(position, host.width, host.height, density), host.width, host.height)
        prefs.edit { putFloat(X, position.x); putFloat(Y, position.y); putFloat(WIDTH, position.width); putFloat(HEIGHT, position.height) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGesture(handle: View, resize: Boolean) {
        var start: FloatingChatPosition? = null
        var initial: FloatingChatRect? = null
        var downX = 0f
        var downY = 0f
        var moved = false
        val slop = ViewConfiguration.get(host.context).scaledTouchSlop
        handle.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    start = position
                    initial = FloatingChatGeometry.fit(position, host.width, host.height, density)
                    downX = event.rawX; downY = event.rawY; moved = false
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val rect = initial
                    if (rect != null && event.pointerCount == 1) {
                        val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                        moved = moved || abs(dx) > slop || abs(dy) > slop
                        if (moved) {
                            val desired = if (resize) rect.copy(width = rect.width + dx, height = rect.height + dy)
                                else rect.copy(left = rect.left + dx, top = rect.top + dy)
                            position = FloatingChatGeometry.fromRect(desired, host.width, host.height)
                            applyBounds()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    if (initial != null) {
                        if (moved) savePosition() else view.performClick()
                    }
                    initial = null; start = null
                    true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    start?.let { position = it; applyBounds() }
                    initial = null; start = null
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
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
        host.removeOnLayoutChangeListener(hostLayoutListener)
        host.removeView(binding.root)
        active = false
        chat = null
    }
}
