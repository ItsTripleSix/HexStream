package com.github.andreyasadchy.xtra.ui.player

import org.junit.Assert.*
import org.junit.Test

class FloatingChatGeometryTest {
    private fun assertFits(rect: FloatingChatRect, width: Int, height: Int) {
        assertTrue(rect.left >= 0 && rect.top >= 0)
        assertTrue(rect.width > 0 && rect.height > 0)
        assertTrue(rect.left + rect.width <= width.coerceAtLeast(1))
        assertTrue(rect.top + rect.height <= height.coerceAtLeast(1))
    }

    @Test fun defaultFitsLandscape() {
        val rect = FloatingChatGeometry.fit(FloatingChatPosition(), 2400, 1080, 3f)
        assertFits(rect, 2400, 1080)
        assertEquals(2400, rect.left + rect.width)
    }
    @Test fun tinyViewportCannotInvertMinimumAndMaximumSizes() {
        for (w in listOf(1, 50, 100, 300)) for (h in listOf(1, 40, 100)) {
            assertFits(FloatingChatGeometry.fit(FloatingChatPosition(), w, h, 4f), w, h)
        }
    }
    @Test fun offscreenSavedValuesAreClamped() {
        val rect = FloatingChatGeometry.fit(FloatingChatPosition(-20f, 100f, 5f, -1f), 1000, 500, 1f)
        assertFits(rect, 1000, 500)
        assertEquals(0, rect.left)
        assertEquals(500, rect.top + rect.height)
    }
    @Test fun corruptNonFinitePreferencesDoNotCrash() {
        assertFits(FloatingChatGeometry.fit(FloatingChatPosition(Float.NaN, Float.POSITIVE_INFINITY, Float.NaN, Float.NEGATIVE_INFINITY), 800, 400, Float.NaN), 800, 400)
    }
    @Test fun normalizedPositionSurvivesViewportChanges() {
        val position = FloatingChatPosition(0.8f, 0.4f, 0.4f, 0.7f)
        val original = FloatingChatGeometry.fit(position, 2000, 1000, 1f)
        val normalized = FloatingChatGeometry.fromRect(original, 2000, 1000)
        val smaller = FloatingChatGeometry.fit(normalized, 1000, 500, 1f)
        assertEquals(original.left / 2, smaller.left)
        assertEquals(original.top / 2, smaller.top)
    }
    @Test fun resizingKeepsTheTopLeftWhenUnclamped() {
        val rect = FloatingChatRect(100, 50, 400, 300)
        val resized = rect.copy(width = 500, height = 350)
        val fitted = FloatingChatGeometry.fit(FloatingChatGeometry.fromRect(resized, 1600, 800), 1600, 800, 1f)
        assertEquals(resized, fitted)
    }
    @Test fun notYetMeasuredViewportIsSafe() {
        assertFits(FloatingChatGeometry.fit(FloatingChatPosition(), 0, 0, 1f), 0, 0)
    }
    @Test fun overlayOnlyAppearsInAnOpenMaximizedLandscapeVideo() {
        assertTrue(FloatingChatGeometry.shouldFloat(true, false, true, true, false, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(false, false, true, true, false, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, true, true, true, false, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, false, true, false, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, false, false, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, true, true, true, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, true, false, false, false))
        assertFalse(FloatingChatGeometry.shouldFloat(true, false, true, true, false, true, true))
    }
    @Test fun manySizesAndSavedPositionsStayInsideVideo() {
        for (w in listOf(120, 320, 720, 1080, 2400)) for (h in listOf(60, 180, 480, 1000)) {
            for (offset in listOf(-5f, 0f, 0.5f, 1f, 9f)) {
                assertFits(FloatingChatGeometry.fit(FloatingChatPosition(offset, offset, offset, offset), w, h, 2.5f), w, h)
            }
        }
    }
}
