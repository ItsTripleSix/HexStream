package com.github.andreyasadchy.xtra.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** No account, preference, follow, database or notification mutations. */
@RunWith(AndroidJUnit4::class)
class FilterControlsDeviceTest {
    @Test fun protectedWordStillGetsSpanishDetection() = runBlocking {
        val card = FilterCard(login="sample_channel",title="SampleHandle retornando de la niebla a LANDMARK!!!???")
        val engine = FilterEngine(FilterRules(titles=listOf("~mark"),allowedWords=listOf("Landmark"),englishOnly=true,detectTitleLanguage=true))
        val language = ContentFilters.detectTitle(engine.titleForDetection(card))
        assertEquals("es",language)
        val matches = engine.matches(card,language)
        assertTrue(matches.any { it.cause == FilterCause.TITLE_LANGUAGE })
        assertFalse(matches.any { it.rule == "~mark" })
    }
    @Test fun completeEmojiHandlingWorksOnAndroid() {
        val rules = FilterRules(blockEmojis=true,blockNonEnglishCharacters=true,allowedEmojis=listOf("❤️","👨‍👩‍👧‍👦","👍🏽","🇺🇸"))
        val engine = FilterEngine(rules)
        assertTrue(engine.matches(FilterCard(title="English ❤️ 👨‍👩‍👧‍👦 👍🏽 🇺🇸")).isEmpty())
        assertTrue(engine.matches(FilterCard(title="English ❤️ 🔴")).any { it.cause == FilterCause.EMOJI && it.evidence == "🔴" })
        assertFalse(engine.matches(FilterCard(title="❤️ 日本語")).isEmpty())
    }
}
