package com.github.andreyasadchy.xtra.ui.player

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerStatusBarDeviceTest {
    @Test fun statusBarCallbacksIgnoreAPlayerWithoutAnAttachedActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertEquals("Use the isolated QA package", "com.github.andreyasadchy.xtra.qa", instrumentation.targetContext.packageName)
        instrumentation.runOnMainSync {
            val fragment = ExoPlayerFragment()
            assertFalse(fragment.isAdded)
            for (name in listOf("showStatusBar", "hideStatusBar")) {
                PlayerFragment::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(fragment)
            }
        }
    }
}
