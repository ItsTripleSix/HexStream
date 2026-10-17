package com.github.andreyasadchy.xtra.ui.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentChatBinding
import com.github.andreyasadchy.xtra.ui.chat.ChatOverlayComposer
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
                val controls = FrameLayout(activity).apply { id = R.id.playerControls; visibility = View.GONE }
                controls.addView(Button(activity).apply {
                    id = R.id.toggleChat
                    text = "QA player control"
                    setOnClickListener { controls.tag = "clicked" }
                }, FrameLayout.LayoutParams(200, 120, Gravity.BOTTOM or Gravity.END))
                host.addView(controls, FrameLayout.LayoutParams(-1, -1))
                controller = FloatingChatController(host, dock, prefs, { active, contrast -> chat.tag = active to contrast }, controls)
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

    @Test fun transparentChatHasNoToolbarAndDoesNotShrinkVideo() = fixture { activity, host, _, chat, controller ->
        val hostWidth = host.width
        val hostHeight = host.height
        instrumentation.runOnMainSync { controller.update(true, chat) }
        awaitLayout(controller.overlay)
        instrumentation.runOnMainSync {
            val bounds = controller.overlay.layoutParams as FrameLayout.LayoutParams
            assertEquals(hostWidth, host.width)
            assertEquals(hostHeight, host.height)
            assertEquals(hostHeight, bounds.height)
            assertEquals(0, bounds.topMargin)
            assertEquals(hostWidth, bounds.leftMargin + bounds.width)
            val messages = controller.overlay.findViewById<FrameLayout>(R.id.floatingMessages)
            assertEquals(0, Color.alpha((messages.background as ColorDrawable).color))
            assertEquals(1, (controller.overlay as ViewGroup).childCount)
            assertEquals(0, messages.top)
            assertSame(chat, messages.getChildAt(0))
            val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
            host.draw(Canvas(bitmap))
            File(activity.externalCacheDir, "transparent-chat-qa.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun opacityAndContrastChangesKeepTheSameChatAndRestoreTheDock() = fixture { _, _, dock, chat, controller ->
        instrumentation.runOnMainSync {
            controller.update(true, chat)
            assertEquals(true to true, chat.tag)
            val prefs = instrumentation.targetContext.getSharedPreferences("floating_chat_fixture", 0)
            prefs.edit().putInt(FloatingChatController.OPACITY, 70).putBoolean(FloatingChatController.CONTRAST, false).commit()
            prefs.edit().putString(FloatingChatController.SIDE, "left").commit()
            controller.update(true, chat)
            assertEquals(0, (controller.overlay.layoutParams as FrameLayout.LayoutParams).leftMargin)
            val messages = controller.overlay.findViewById<FrameLayout>(R.id.floatingMessages)
            assertEquals(178, Color.alpha((messages.background as ColorDrawable).color))
            assertEquals(true to false, chat.tag)
            assertSame(chat, messages.getChildAt(0))
            controller.update(false, chat)
            assertFalse(controller.active)
            assertSame(dock, chat.parent)
            assertEquals(false to false, chat.tag)
        }
    }

    @Test fun visiblePlayerControlsReceiveTapsAboveTransparentChat() = fixture { _, host, _, chat, controller ->
        instrumentation.runOnMainSync {
            controller.update(true, chat)
            host.findViewById<View>(R.id.playerControls).visibility = View.VISIBLE
        }
        awaitLayout(controller.overlay)
        awaitLayout(host.findViewById(R.id.toggleChat))
        instrumentation.runOnMainSync {
            assertTrue("Keep the original player layer order", host.indexOfChild(host.findViewById(R.id.playerControls)) < host.indexOfChild(controller.overlay))
            val time = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                MotionEvent.obtain(time, time + 10, action, host.width - 20f, host.height - 20f, 0).also {
                    host.dispatchTouchEvent(it)
                    it.recycle()
                }
            }
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals("clicked", host.findViewById<View>(R.id.playerControls).tag)
            assertTrue(controller.active)
            assertSame(chat, controller.overlay.findViewById<FrameLayout>(R.id.floatingMessages).getChildAt(0))
        }
    }

    @Test fun composerSupportsDraftInputAboveInsetsAndSignedOutHint() = fixture { activity, host, dock, chat, controller ->
        lateinit var input: FragmentChatBinding
        instrumentation.runOnMainSync {
            dock.removeView(chat)
            input = FragmentChatBinding.inflate(activity.layoutInflater, dock, false)
            dock.addView(input.root)
            ChatOverlayComposer.show(input, canSend = true)
            host.findViewById<View>(R.id.playerControls).visibility = View.VISIBLE
            controller.setBottomInset(220)
            controller.update(true, input.root)
        }
        awaitLayout(input.editText)
        instrumentation.runOnMainSync {
            assertTrue(input.messageView.isShown)
            assertTrue(input.editText.isEnabled)
            assertEquals(activity.getString(R.string.hex_chat_message_hint), input.editText.hint)
            val bounds = Rect()
            input.editText.getDrawingRect(bounds)
            host.offsetDescendantRectToMyCoords(input.editText, bounds)
            assertTrue("Composer stays above the keyboard inset", bounds.bottom <= host.height - 220)
            val time = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                MotionEvent.obtain(time, time + 10, action, bounds.exactCenterX(), bounds.exactCenterY(), 0).also {
                    host.dispatchTouchEvent(it); it.recycle()
                }
            }
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertTrue("Composer remains reachable while player controls are shown", input.editText.hasFocus())
            input.editText.setText("QA draft")
            assertEquals("QA draft", input.editText.text.toString())
            input.editText.text.clear()
            ChatOverlayComposer.show(input, canSend = false)
            assertFalse(input.editText.isEnabled)
            assertEquals(activity.getString(R.string.hex_chat_sign_in_hint), input.editText.hint)
        }
    }

}
