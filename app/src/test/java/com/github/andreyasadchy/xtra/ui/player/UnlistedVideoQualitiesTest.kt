package com.github.andreyasadchy.xtra.ui.player

import com.github.andreyasadchy.xtra.model.VideoQuality
import org.junit.Assert.*
import org.junit.Test

class UnlistedVideoQualitiesTest {
    @Test fun excludesUnrequestedCodecsAndIncompatiblePortraitVariants() {
        assertFalse(UnlistedVideoQualities.supportsReason("FR_CODEC_NOT_REQUESTED"))
        assertFalse(UnlistedVideoQualities.supportsReason("FR_DUALFORMAT_INCOMPATIBLE"))
    }

    @Test fun doesNotDiscardRecoverableVariantsForOtherFilterReasons() {
        assertTrue(UnlistedVideoQualities.supportsReason(""))
        assertTrue(UnlistedVideoQualities.supportsReason("FR_VIDEO_BITRATE_TOO_HIGH"))
        assertTrue(UnlistedVideoQualities.supportsReason("FUTURE_REASON"))
    }

    @Test fun ignoresVariantsWithoutAnIdentity() {
        for (name in listOf(null, "", "  ")) {
            assertNull(UnlistedVideoQualities.variant(name, "1920x1080", 60f, 6000000, "avc1"))
        }
    }

    @Test fun missingOrZeroMetadataStaysUnknown() {
        val quality = UnlistedVideoQualities.variant("source", "", 0f, 0, " ")!!
        assertNull(quality.resolution)
        assertNull(quality.frameRate)
        assertNull(quality.bitrate)
        assertNull(quality.codecs)
        val absent = UnlistedVideoQualities.variant("other", null, null, -1, null)!!
        assertNull(absent.resolution)
        assertNull(absent.frameRate)
        assertNull(absent.bitrate)
        assertNull(absent.codecs)
    }

    @Test fun retainsValidMetadataAndDoesNotInventAPlayableUrl() {
        val quality = UnlistedVideoQualities.variant("1080p60", "1920x1080", 60f, 6000000, "avc1.64002a")!!
        assertEquals("1080p60", quality.name)
        assertEquals(1080, quality.resolution)
        assertEquals(60f, quality.frameRate)
        assertEquals(6000000, quality.bitrate)
        assertEquals("avc1.64002a", quality.codecs)
        assertNull(quality.url)
    }

    @Test fun invalidOptionalMetadataDoesNotDiscardTheVariant() {
        val quality = UnlistedVideoQualities.variant("unlisted", "unknown", Float.NaN, -2, "")!!
        assertNull(quality.resolution)
        assertNull(quality.frameRate)
        assertNull(quality.bitrate)
    }

    @Test fun recoveryPrefersResolutionThenFrameRateThenBitrate() {
        val qualities = listOf(
            VideoQuality("720-high-bitrate", 720, 60f, 9000000),
            VideoQuality("1080-30", 1080, 30f, 8000000),
            VideoQuality("1080-60-low", 1080, 60f, 4000000),
            VideoQuality("1080-60-high", 1080, 60f, 6000000),
        )
        assertEquals(listOf("1080-60-high", "1080-60-low", "1080-30", "720-high-bitrate"),
            UnlistedVideoQualities.ordered(qualities).map { it.name })
        assertEquals("720-high-bitrate", qualities.first().name)
    }

    @Test fun unknownMetadataSortsAfterKnownVideoAndAudioIsLast() {
        val qualities = listOf(VideoQuality("unknown"), VideoQuality("AUDIO_ONLY", bitrate = 128000),
            VideoQuality("720p", 720, 30f, 2000000))
        assertEquals(listOf("720p", "unknown", "AUDIO_ONLY"),
            UnlistedVideoQualities.ordered(qualities).map { it.name })
    }

    @Test fun equalMetadataPreservesInputOrder() {
        val first = VideoQuality("first", 1080, 60f, 6000000)
        val second = VideoQuality("second", 1080, 60f, 6000000)
        assertSame(first, UnlistedVideoQualities.ordered(listOf(first, second)).first())
        assertTrue(UnlistedVideoQualities.ordered(emptyList()).isEmpty())
    }
}
