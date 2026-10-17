package com.github.andreyasadchy.xtra.ui.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FloatingChatDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun fixture(block: (SettingsActivity, FrameLayout, FrameLayout, ScrollView, FloatingChatController) -> Unit) {
        val context = instrumentation.targetContext
        assertEquals("Use the isolated QA package", "com.github.andreyasadchy.xtra.qa", context.packageName)
        val activity = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        lateinit var host: FrameLayout
        lateinit var dock: FrameLayout
        lateinit var chat: ScrollView
        lateinit var controller: FloatingChatController
        var cleanup: (() -> Unit)? = null
        try {
            instrumentation.runOnMainSync {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val prefs = context.getSharedPreferences("floating_chat_fixture", 0)
                prefs.edit().clear().commit()
                val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                host = FrameLayout(activity).apply {
                    background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xff476982.toInt(), 0xff15151c.toInt()))
                }
                dock = FrameLayout(activity)
                chat = ScrollView(activity).apply {
                    addView(TextView(activity).apply {
                        text = (1..100).joinToString("\n") { "QA chat message $it · existing session" }
                        textSize = 16f; setTextColor(Color.WHITE)
                    })
                }
                root.addView(host, LinearLayout.LayoutParams(-1, 0, 1f))
                root.addView(dock, LinearLayout.LayoutParams(-1, (120 * activity.resources.displayMetrics.density).toInt()))
                dock.addView(chat, FrameLayout.LayoutParams(-1, -1))
                activity.setContentView(root)
                controller = FloatingChatController(host, dock, prefs, { controller.update(false, chat) }, { _, _ -> })
                cleanup = { controller.dispose() }
            }
            awaitLayout(host)
            awaitLayout(chat)
            block(activity, host, dock, chat, controller)
        } finally {
            instrumentation.runOnMainSync { cleanup?.invoke(); activity.finish() }
        }
    }

    private fun awaitLayout(view: View) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (true) {
            var ready = false
            instrumentation.runOnMainSync { ready = view.isLaidOut && !view.isLayoutRequested && view.width > 0 && view.height > 0 }
            if (ready) return
            assertTrue("QA view was not measured: ${view.javaClass.simpleName}", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(30)
        }
    }

    @Test fun reparentingPreservesChatIdentityAndScrollState() = fixture { _, _, dock, chat, controller ->
        instrumentation.runOnMainSync { chat.scrollTo(0, 300) }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync { assertEquals("Initial fixture scroll", 300, chat.scrollY) }
        for (index in 1..5) {
            instrumentation.runOnMainSync { controller.update(true, chat) }
            awaitLayout(controller.overlay)
            instrumentation.runOnMainSync {
                assertTrue(controller.active)
                assertSame(chat, controller.overlay.findViewById<FrameLayout>(R.id.floatingMessages).getChildAt(0))
                assertEquals(300, chat.scrollY)
                controller.update(false, chat)
            }
            awaitLayout(chat)
            instrumentation.runOnMainSync {
                assertSame(dock, chat.parent)
                assertEquals(300, chat.scrollY)
                assertEquals(1, dock.childCount)
            }
        }
    }

    @Test fun draggingIsBoundedAndCancelRestoresThePreviousBounds() = fixture { _, host, _, chat, controller ->
        instrumentation.runOnMainSync { controller.update(true, chat) }
        awaitLayout(controller.overlay)
        instrumentation.runOnMainSync {
            val handle = controller.overlay.findViewById<View>(R.id.moveChat)
            val before = controller.overlay.layoutParams as FrameLayout.LayoutParams
            val beforeLeft = before.leftMargin; val beforeTop = before.topMargin
            fun event(action: Int, x: Float, y: Float) {
                MotionEvent.obtain(0, 10, action, x, y, 0).also { handle.dispatchTouchEvent(it); it.recycle() }
            }
            event(MotionEvent.ACTION_DOWN, 10f, 10f)
            event(MotionEvent.ACTION_MOVE, -10000f, -10000f)
            val moved = controller.overlay.layoutParams as FrameLayout.LayoutParams
            assertEquals(0, moved.leftMargin); assertEquals(0, moved.topMargin)
            assertTrue(moved.width <= host.width && moved.height <= host.height)
            event(MotionEvent.ACTION_CANCEL, -10000f, -10000f)
            val restored = controller.overlay.layoutParams as FrameLayout.LayoutParams
            assertEquals(beforeLeft, restored.leftMargin); assertEquals(beforeTop, restored.topMargin)
        }
    }

    @Test fun controlsResizeToggleContrastAndCloseWithoutReplacingChat() = fixture { activity, host, dock, chat, controller ->
        instrumentation.runOnMainSync { controller.update(true, chat) }
        awaitLayout(controller.overlay)
        instrumentation.runOnMainSync {
            val prefs = instrumentation.targetContext.getSharedPreferences("floating_chat_fixture", 0)
            controller.overlay.findViewById<View>(R.id.resizeChat).performClick()
            val sized = controller.overlay.layoutParams as FrameLayout.LayoutParams
            assertTrue(sized.width <= host.width && sized.height <= host.height)
            controller.overlay.findViewById<View>(R.id.contrastChat).performClick()
            assertFalse(prefs.getBoolean(FloatingChatController.CONTRAST, true))
            val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
            host.draw(Canvas(bitmap))
            File(activity.externalCacheDir, "floating-chat-qa.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            controller.overlay.findViewById<View>(R.id.closeChat).performClick()
            assertFalse(controller.active)
            assertSame(dock, chat.parent)
        }
    }
}
