package com.github.andreyasadchy.xtra.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.graphics.Rect
import com.github.andreyasadchy.xtra.R

/** Keep the player's original control/gesture layer reachable below transparent chat. */
class ChatOverlayLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    internal var playerControls: View? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Reject the initial touch so the host can deliver the whole gesture to the
        // existing layer beneath us. Do not interrupt a chat gesture already in progress.
        if (event.actionMasked == MotionEvent.ACTION_DOWN && playerControls?.visibility == View.VISIBLE) {
            val input = findViewById<View>(R.id.messageView)?.takeIf { it.isShown }
            val inputBounds = Rect()
            input?.let { it.getDrawingRect(inputBounds); offsetDescendantRectToMyCoords(it, inputBounds) }
            if (!inputBounds.contains(event.x.toInt(), event.y.toInt())) return false
        }
        return super.dispatchTouchEvent(event)
    }
}
