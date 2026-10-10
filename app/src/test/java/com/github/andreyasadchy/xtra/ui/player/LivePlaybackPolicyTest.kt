package com.github.andreyasadchy.xtra.ui.player
import org.junit.Assert.*
import org.junit.Test
class LivePlaybackPolicyTest {
    @Test fun presetOverridesLegacyValuesOnExistingInstalls() {
        val s = LivePlaybackPolicy.settings(true, "2000", "1", "1")
        assertEquals(1000L, s.targetMs); assertTrue(s.minSpeed!! < 1f); assertTrue(s.maxSpeed!! > 1f)
    }
    @Test fun disablingPresetRestoresUserSettings() {
        val s = LivePlaybackPolicy.settings(false, "5000", "0.98", "1.04")
        assertEquals(5000L, s.targetMs); assertEquals(.98f, s.minSpeed!!, 0f); assertEquals(1.04f, s.maxSpeed!!, 0f)
    }
    @Test fun invalidSettingsCannotProduceNegativeOffsetsOrNanSpeeds() {
        val s = LivePlaybackPolicy.settings(false, "-1", "NaN", "Infinity")
        assertEquals(2000L, s.targetMs); assertNull(s.minSpeed); assertNull(s.maxSpeed)
    }
    @Test fun reversedSpeedRangeFallsBackToEngineDefaults() {
        val s = LivePlaybackPolicy.settings(false, null, "1.2", "0.9")
        assertNull(s.minSpeed); assertNull(s.maxSpeed)
    }
}
