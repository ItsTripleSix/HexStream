package com.github.andreyasadchy.xtra.ui.player

import org.junit.Assert.*
import org.junit.Test

class SwipeControlPolicyTest {
    @Test fun centerRemainsAvailableForMinimize() {
        assertNull(SwipeControlPolicy.control(500f, 200f, 1000f, 500f, 24f, true, true))
    }
    @Test fun sidesSelectIndependentControls() {
        assertEquals(SwipeControlPolicy.Control.BRIGHTNESS, SwipeControlPolicy.control(120f, 200f, 1000f, 500f, 24f, true, true))
        assertEquals(SwipeControlPolicy.Control.VOLUME, SwipeControlPolicy.control(880f, 200f, 1000f, 500f, 24f, true, true))
        assertNull(SwipeControlPolicy.control(120f, 200f, 1000f, 500f, 24f, false, true))
    }
    @Test fun edgesRemainAvailableForSystemAndPlayerControls() {
        assertNull(SwipeControlPolicy.control(120f, 10f, 1000f, 500f, 24f, true, true))
        assertNull(SwipeControlPolicy.control(880f, 490f, 1000f, 500f, 24f, true, true))
        assertNull(SwipeControlPolicy.control(5f, 200f, 1000f, 500f, 24f, true, true))
    }
    @Test fun tapAndHorizontalMovementCannotAdjustLevels() {
        assertFalse(SwipeControlPolicy.vertical(0f, 10f, 24f))
        assertFalse(SwipeControlPolicy.vertical(100f, 30f, 24f))
        assertTrue(SwipeControlPolicy.vertical(5f, -60f, 24f))
    }
    @Test fun directionAndLimitsArePredictable() {
        assertEquals(.75f, SwipeControlPolicy.level(.5f, -25f, 100f), .001f)
        assertEquals(.25f, SwipeControlPolicy.level(.5f, 25f, 100f), .001f)
        assertEquals(1f, SwipeControlPolicy.level(.5f, -500f, 100f), .001f)
        assertEquals(0f, SwipeControlPolicy.level(.5f, 500f, 100f), .001f)
    }
}
