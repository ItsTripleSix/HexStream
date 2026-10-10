package com.github.andreyasadchy.xtra.ui.player
import org.junit.Assert.*
import org.junit.Test
class LiveBufferPolicyTest {
    @Test fun liveStartsAtHalfASecondDespiteLargeLegacyThresholds() {
        assertEquals(false, LiveBufferPolicy.shouldStart(true, true, 499999, 1f, false))
        assertEquals(true, LiveBufferPolicy.shouldStart(true, true, 500000, 1f, false))
    }
    @Test fun recoveryNeedsOneSecond() {
        assertEquals(false, LiveBufferPolicy.shouldStart(true, true, 999999, 1f, true))
        assertEquals(true, LiveBufferPolicy.shouldStart(true, true, 1000000, 1f, true))
    }
    @Test fun vodAndDisabledModeRetainOriginalControl() {
        assertNull(LiveBufferPolicy.shouldStart(true, false, 500000, 1f, false))
        assertNull(LiveBufferPolicy.shouldStart(false, true, 500000, 1f, false))
    }
    @Test fun catchUpSpeedRequiresEnoughPlayoutTime() {
        assertEquals(false, LiveBufferPolicy.shouldStart(true, true, 500000, 1.08f, false))
        assertEquals(true, LiveBufferPolicy.shouldStart(true, true, 550000, 1.08f, false))
    }
    @Test fun invalidSpeedDoesNotTriggerPrematureStart() {
        assertEquals(false, LiveBufferPolicy.shouldStart(true, true, -1, Float.NaN, false))
        assertEquals(true, LiveBufferPolicy.shouldStart(true, true, 500000, 0f, false))
    }
}
