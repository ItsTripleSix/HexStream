package com.github.andreyasadchy.xtra.ui.player

import org.junit.Assert.*
import org.junit.Test

class FloatingChatGeometryTest {
    private fun assertFits(rect: FloatingChatRect, width: Int, height: Int) {
        assertTrue(rect.left >= 0)
        assertEquals(0, rect.top)
        assertTrue(rect.width > 0)
        assertEquals(height.coerceAtLeast(1), rect.height)
        assertEquals(width.coerceAtLeast(1), rect.left + rect.width)
    }

    @Test fun chatOccupiesTheRightThirdOfLandscapeVideo() {
        val rect = FloatingChatGeometry.fit(2400, 1080, 3f)
        assertEquals(FloatingChatRect(1632, 0, 768, 1080), rect)
    }
    @Test fun sideSelectionMirrorsTheSameChatWidth() {
        val left = FloatingChatGeometry.fit(2400, 1080, 3f, leftSide = true)
        val right = FloatingChatGeometry.fit(2400, 1080, 3f, leftSide = false)
        assertEquals(0, left.left)
        assertEquals(right.width, left.width)
        assertEquals(right.height, left.height)
        assertEquals(2400, right.left + right.width)
    }
    @Test fun chatUsesTheFullHeightWithoutAHeaderGap() {
        assertFits(FloatingChatGeometry.fit(1920, 1080, 2f), 1920, 1080)
    }
    @Test fun tinyViewportsStillFit() {
        for (w in listOf(1, 50, 100, 300)) for (h in listOf(1, 40, 100)) {
            assertFits(FloatingChatGeometry.fit(w, h, 4f), w, h)
        }
    }
    @Test fun nonFiniteDensityDoesNotCrash() {
        for (density in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertFits(FloatingChatGeometry.fit(800, 400, density), 800, 400)
        }
    }
    @Test fun aViewportChangeReanchorsChatToTheNewRightEdge() {
        val before = FloatingChatGeometry.fit(2400, 1080, 2f)
        val after = FloatingChatGeometry.fit(1600, 720, 2f)
        assertTrue(after.width < before.width)
        assertFits(after, 1600, 720)
    }
    @Test fun aDensityChangeKeepsSmallChatReadableAndInsideTheVideo() {
        val low = FloatingChatGeometry.fit(1200, 600, 1f)
        val high = FloatingChatGeometry.fit(1200, 600, 4f)
        assertTrue(high.width > low.width)
        assertFits(high, 1200, 600)
    }
    @Test fun notYetMeasuredOrInvalidViewportIsSafe() {
        assertFits(FloatingChatGeometry.fit(0, 0, 1f), 0, 0)
        assertFits(FloatingChatGeometry.fit(-100, -100, 1f), -100, -100)
    }
    @Test fun overlayOnlyAppearsInAnOpenMaximizedLandscapeVideo() {
        assertTrue(FloatingChatGeometry.shouldFloat(true, false, true, true, false, true))
        assertFalse(FloatingChatGeometry.shouldFloat(false, false, true, true, false, true))
        assertFalse(FloatingChatGeometry.shouldFloat(true, true, true, true, false, true))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, false, true, false, true))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, false, false, true))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, true, true, true))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, true, false, false))
    }
    @Test fun manyViewportSizesStayAnchoredWithoutCropping() {
        for (w in listOf(120, 320, 720, 1080, 2400)) for (h in listOf(60, 180, 480, 1000)) {
            assertFits(FloatingChatGeometry.fit(w, h, 2.5f), w, h)
        }
    }
}
