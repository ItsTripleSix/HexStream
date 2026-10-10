package com.github.andreyasadchy.xtra.util

import org.junit.Assert.*
import org.junit.Test

class FilterControlsTest {
    @Test fun wordExceptionsProtectOnlyCompleteTokens() {
        val engine = FilterEngine(FilterRules(channels = listOf("~mark"), titles = listOf("~mark"), tags = listOf("~mark"), allowedWords=listOf("Landmark","LandmarkOne","Benchmark")))
        assertTrue(engine.matches(FilterCard(title = "English LANDMARK!!!", tags = listOf("LandmarkOne"))).isEmpty())
        assertTrue(engine.matches(FilterCard(title = "Benchmark and Landmark Series X")).isEmpty())
        assertFalse(engine.matches(FilterCard(title = "Landmark and a bookmark")).isEmpty())
        assertFalse(engine.matches(FilterCard(title = "watermark gameplay")).isEmpty())
        assertFalse(engine.matches(FilterCard(login = "landmarkgamer")).isEmpty())
    }
    @Test fun protectedWordDoesNotHideLanguageEvidence() {
        val card = FilterCard(login="sample_channel_one", title="SampleHandle retornando de la niebla a LANDMARK!!!???")
        val matches = FilterEngine(FilterRules(titles=listOf("~mark"),englishOnly=true,detectTitleLanguage=true,allowedWords=listOf("Landmark"))).matches(card, "es")
        assertFalse(matches.any { it.cause == FilterCause.RULE })
        assertTrue(matches.any { it.cause == FilterCause.TITLE_LANGUAGE && it.language == "es" && it.value == card.title })
    }
    @Test fun ruleReasonRetainsTheExactTitle() {
        val title = "Playing a game with MyTeam tonight"
        val matches = FilterEngine(FilterRules(titles=listOf("~myteam"))).matches(FilterCard(login="sample_channel_two",title=title))
        assertTrue(matches.any { it.cause == FilterCause.RULE && it.rule == "~myteam" && it.field == "Title" && it.value == title })
    }
    @Test fun removingWordExceptionRestoresExplicitRule() {
        val rules = FilterRules(titles=listOf("~mark"),allowedWords=emptyList())
        assertTrue(FilterEngine(rules).matches(FilterCard(title="LANDMARK")).any { it.rule == "~mark" })
    }
    @Test fun emojiDefaultsAndSpecificOverridesStayNarrow() {
        val card = FilterCard(title="🔴 English games 🎮")
        assertTrue(FilterEngine(FilterRules()).matches(card).isEmpty())
        assertTrue(FilterEngine(FilterRules(blockedEmojis=listOf("🔴"))).matches(card).any { it.cause == FilterCause.EMOJI })
        assertTrue(FilterEngine(FilterRules(blockedEmojis=listOf("🔴"),allowedEmojis=listOf("🔴"))).matches(card).isEmpty())
        assertTrue(FilterEngine(FilterRules(blockEmojis=true,allowedEmojis=listOf("🔴"))).matches(card).any { it.evidence == "🎮" })
        assertTrue(FilterEngine(FilterRules(blockEmojis=true,allowedEmojis=listOf("🔴","🎮"))).matches(card).isEmpty())
        assertFalse(FilterEngine(FilterRules(allowedEmojis=listOf("🔴"),blockNonEnglishCharacters=true)).matches(FilterCard(title="🔴 日本語")).isEmpty())
    }
    @Test fun emojiSequencesAreCompleteAndDigitsAreOrdinaryText() {
        for (emoji in listOf("🔴","❤️","❤","👨‍👩‍👧‍👦","🏳️‍🌈","👍🏽","🇺🇸","1️⃣","🫩")) {
            assertTrue(emoji,EmojiCatalog.isSingle(emoji))
            assertEquals(emoji,EmojiCatalog.tokens("A $emoji Z").single().value)
        }
        assertFalse(EmojiCatalog.isSingle("🔴🎮"))
        assertTrue(EmojiCatalog.tokens("100 English words!? 2026").isEmpty())
        assertTrue(FilterEngine(FilterRules(blockEmojis=true,allowedEmojis=listOf("❤"))).matches(FilterCard(title="English ❤️")).isEmpty())
        assertFalse(FilterEngine(FilterRules(blockEmojis=true,allowedEmojis=listOf("👍"))).matches(FilterCard(title="English 👍🏽")).isEmpty())
    }
    @Test fun importedEmojiOnlyRulesMoveAndAllowWinsImportConflict() {
        val rules = ContentFilters.parse("""{"channels":["~🔴"],"tags":["~🔴","~spoiler"],"titles":["*🔴*","~myteam"],"allowedEmojis":["🔴"],"blockedEmojis":["🔴","🎮"]}""")
        assertEquals(emptyList<String>(),rules.channels)
        assertEquals(listOf("~spoiler"),rules.tags)
        assertEquals(listOf("~myteam"),rules.titles)
        assertEquals(listOf("🎮"),rules.blockedEmojis)
        assertEquals(listOf("🔴"),rules.allowedEmojis)
    }
    @Test fun emojiAllowDoesNotCancelOtherCustomWords() {
        val rules = FilterRules(titles=listOf("~🔴", "~spoiler"),allowedEmojis=listOf("🔴"))
        assertTrue(FilterEngine(rules).matches(FilterCard(title="🔴 English gameplay")).isEmpty())
        assertTrue(FilterEngine(rules).matches(FilterCard(title="🔴 spoiler tonight")).any { it.rule == "~spoiler" })
    }
    @Test fun emojisRespectFieldAndCreatorExceptions() {
        val rules = FilterRules(blockEmojis=true,allowedTitles=listOf("~safe"))
        assertTrue(FilterEngine(rules).matches(FilterCard(title="safe 🔴")).isEmpty())
        assertFalse(FilterEngine(rules).matches(FilterCard(title="safe 🔴",tags=listOf("🎮"))).isEmpty())
        assertTrue(FilterEngine(rules).matches(FilterCard(title="🔴"),followed=true).isEmpty())
    }
    @Test fun invalidEmojiEntriesRejectedBeforeSaveAndFullSettingsRoundTrip() {
        for (bad in listOf("English","🔴🎮","~🔴","1","")) {
            try { ContentFilters.parse("""{"tags":[],"blockedEmojis":["$bad"]}"""); fail("Invalid emoji accepted: $bad") } catch (_: IllegalArgumentException) {}
        }
        val rules = FilterRules(blockEmojis=true, blockedEmojis=listOf("🔴"), allowedEmojis=listOf("👨‍👩‍👧‍👦"))
        assertEquals(rules, ContentFilters.parse(ContentFilters.json.encodeToString(FilterRules.serializer(),rules)))
    }
    @Test fun changingSpecificEmojiDecisionMovesItWithoutChangingOtherRules() {
        val original = FilterRules(blockedEmojis=listOf("🔴","🎮"), titles=listOf("~myteam"))
        val allowed = original.withEmojiList(listOf("🔴"),allow=true)
        assertEquals(listOf("🎮"),allowed.blockedEmojis)
        assertTrue(FilterEngine(allowed).matches(FilterCard(title="🔴 English")).isEmpty())
        assertTrue(FilterEngine(allowed).matches(FilterCard(title="🔴 MyTeam")).any { it.rule == "~myteam" })
        val blocked = allowed.withEmojiList(allowed.blockedEmojis + "🔴",allow=false)
        assertTrue(blocked.allowedEmojis.isEmpty())
        assertTrue(FilterEngine(blocked).matches(FilterCard(title="🔴 English")).any { it.cause == FilterCause.EMOJI })
    }
}
