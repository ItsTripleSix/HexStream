package com.github.andreyasadchy.xtra.util

import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class HexStreamUpdatesTest {
    @Test fun defaultsAndLegacyFeedsResolveToHexStream() {
        listOf(null, "", "  ", "https://api.github.com/repos/crackededed/xtra/releases/tags/latest",
            "https://api.github.com/repos/CRACKEDEDED/XTRA/releases/tags/latest/").forEach {
            assertEquals(HexStreamUpdates.DEFAULT_URL, HexStreamUpdates.url(it))
        }
    }
    @Test fun customFeedIsPreserved() {
        val custom = "https://fixture.invalid/releases/stable"
        assertEquals(custom, HexStreamUpdates.url(custom))
    }
    @Test fun listIncludesBetaAndSkipsDraft() {
        val release = HexStreamUpdates.parse("""[{"draft":true,"tag_name":"draft"},{"draft":false,"prerelease":true,"tag_name":"v1.0.0-beta.1","assets":[]}]""")
        assertEquals("v1.0.0-beta.1", release["tag_name"]?.jsonPrimitive?.content)
    }
    @Test fun singleReleaseAndEmptyListRemainSupported() {
        assertEquals("v1", HexStreamUpdates.parse("""{"tag_name":"v1","assets":[]}""")["tag_name"]?.jsonPrimitive?.content)
        assertTrue(HexStreamUpdates.parse("[]").isEmpty())
    }
}
