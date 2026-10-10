package com.github.andreyasadchy.xtra.util

import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.Tag
import com.github.andreyasadchy.xtra.model.ui.User
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class BlockedTagsTest {
    @Test fun inputNormalizesAndDeduplicates() {
        assertEquals(setOf("english", "vtuber"), BlockedTags.parse(" English, VTuber\nenglish\n ,"))
    }
    @Test fun exactTagsRatherThanTitleOrSubstring() {
        val hidden = Stream(title = "safe title", tags = listOf(" VTUBER "))
        val visible = Stream(title = "VTuber", tags = listOf("notvtuber"))
        val unknown = Stream(tags = null)
        assertEquals(listOf(visible, unknown), BlockedTags.filter(listOf(hidden, visible, unknown), setOf("vtuber")))
    }
    @Test fun categoryTagsUseSamePolicy() {
        val hidden = Game(tags = listOf(Tag(name = "GAMBLING")))
        val visible = Game(tags = emptyList())
        assertEquals(listOf(visible), BlockedTags.filter(listOf(hidden, visible), setOf("gambling")))
    }
    @Test fun localeDoesNotChangeMatching() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertTrue(BlockedTags.matches(listOf("IRL"), setOf("irl")))
        } finally { Locale.setDefault(original) }
    }
    @Test fun emptyBlocklistPreservesAllItems() {
        val items = listOf(Stream(tags = listOf("IRL")))
        assertSame(items, BlockedTags.filter(items, emptySet()))
    }
    @Test fun liveChannelSearchUsesStreamTagsAndLeavesUnknownTagsVisible() {
        val hidden = User(isLive = true, tags = listOf("vtuber"))
        val unknown = User(isLive = false)
        assertEquals(listOf(unknown), BlockedTags.filter(listOf(hidden, unknown), setOf("vtuber")))
    }
}
