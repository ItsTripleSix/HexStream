package com.github.andreyasadchy.xtra.util

import org.junit.Assert.*
import org.junit.Test

class FilterRulesTest {
    @Test fun pokemonExceptionProtectsOnlyItsWord() {
        val engine = FilterEngine(FilterRules(blockNonEnglishCharacters=true,allowedWords=listOf("Pokémon")))
        assertTrue(engine.reasons(FilterCard(title="Playing Pokémon today",tags=listOf("Pokémon"))).isEmpty())
        assertTrue(engine.reasons(FilterCard(title="Playing Poke\u0301mon today")).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="Pokémon 日本語")).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="Pokémon café")).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="Pokémonish")).isEmpty())
        assertFalse(FilterEngine(FilterRules(allowedWords=emptyList(),blockNonEnglishCharacters=true)).reasons(FilterCard(title="Pokémon")).isEmpty())
        assertTrue(FilterEngine(FilterRules(titles=listOf("~pokemon"),allowedWords=listOf("Pokémon"))).reasons(FilterCard(title="Pokémon")).isEmpty())
    }
    @Test fun tagExceptionDoesNotExemptOtherTagsOrTheTitle() {
        val engine = FilterEngine(FilterRules(tags=listOf("spanish"),allowedTags=listOf("Spanish"),blockNonEnglishCharacters=true))
        assertTrue(engine.reasons(FilterCard(tags=listOf("Spanish"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(tags=listOf("Spanish","日本語"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(tags=listOf("Spanish"),title="café")).isEmpty())
        assertFalse(FilterEngine(FilterRules(allowedTags=listOf("Spanish"),useDeclaredLanguageAlone=true,englishOnly=true)).reasons(FilterCard(tags=listOf("Spanish"),language="es")).isEmpty())
    }
    @Test fun titleExceptionDoesNotExemptNamesOrTags() {
        val engine = FilterEngine(FilterRules(titles=listOf("~spoiler"),allowedTitles=listOf("~spoiler"),blockNonEnglishCharacters=true))
        assertTrue(engine.reasons(FilterCard(title="Pokémon spoiler café"),"fr").isEmpty())
        assertNull(engine.titleForDetection(FilterCard(title="Pokémon spoiler café")))
        assertFalse(engine.reasons(FilterCard(title="spoiler",tags=listOf("日本語"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="spoiler",name="café")).isEmpty())
    }
    @Test fun usernameExceptionIsNarrowAndChannelExceptionIsGlobal() {
        val card = FilterCard(login="friend",name="Beyoncé",tags=listOf("日本語"))
        val engine = FilterEngine(FilterRules(channels=listOf("beyonce"),allowedUsernames=listOf("Beyoncé"),blockNonEnglishCharacters=true))
        val reasons = engine.reasons(card)
        assertFalse(reasons.any { it.startsWith("Channel:") })
        assertTrue(reasons.any { it.contains("日本語") })
        assertTrue(FilterEngine(FilterRules(allowedChannels=listOf("friend"))).reasons(card).isEmpty())
    }
    @Test fun allowListsValidateAndRoundTrip() {
        val source = FilterRules(allowedUsernames=listOf("~friend"),allowedTags=listOf("/pokemon/i"),allowedTitles=listOf("~safe title"),allowedWords=listOf("Pokémon","Beyoncé"))
        assertEquals(source,ContentFilters.parse(ContentFilters.json.encodeToString(FilterRules.serializer(),source)))
        try { ContentFilters.parse("""{"tags":[],"allowedTags":["/[bad/"]}"""); fail("Invalid allow regex accepted") } catch(_: IllegalArgumentException) {}
        try { ContentFilters.parse("""{"tags":[],"allowedWords":["~pokemon"]}"""); fail("Pattern accepted as a word") } catch(_: IllegalArgumentException) {}
    }
    @Test fun followedExemptionIsDerivedAndDoesNotChangeManualRules() {
        val rules = FilterRules(channels=listOf("friend"),tags=listOf("blocked"))
        val engine = FilterEngine(rules)
        val card = FilterCard(login="friend",title="日本語",tags=listOf("blocked"))
        assertTrue(engine.reasons(card,"ja",followed=true).isEmpty())
        assertFalse(engine.reasons(card,"ja",followed=false).isEmpty())
        assertTrue(rules.allowedChannels.isEmpty())
        assertFalse(FilterEngine(rules.copy(allowFollowedChannels=false)).reasons(card,"ja",followed=true).isEmpty())
        assertTrue(FilterEngine(rules.copy(allowedChannels=listOf("friend"))).reasons(card,"ja",followed=false).isEmpty())
    }
    private fun rules() = FilterRules(englishOnly = false, blockNonEnglishCharacters = false)
    @Test fun independentFieldsAndHiddenTags() {
        val engine = FilterEngine(rules().copy(channels=listOf("name"), titles=listOf("~spoiler"), tags=listOf("blocked"), categories=listOf("game")))
        assertTrue(engine.reasons(FilterCard(login="NAME")).any { it.startsWith("Channel:") })
        assertTrue(engine.reasons(FilterCard(title="story spoiler tonight")).any { it.startsWith("Title:") })
        assertTrue(engine.reasons(FilterCard(tags=listOf("a","b","c","d","blocked"))).any { it.startsWith("Tag:") })
        assertTrue(engine.reasons(FilterCard(category="Game")).any { it.startsWith("Category:") })
        assertTrue(engine.reasons(FilterCard(title="name game blocked")).isEmpty())
    }
    @Test fun unwantedTwitchPatterns() {
        assertTrue(FilterEngine.matcher("Español")("ESPANOL"))
        assertTrue(FilterEngine.matcher("~cafe")("Enjoy café games"))
        assertTrue(FilterEngine.matcher("/hello, world/i")("HELLO, WORLD"))
        assertFalse(FilterEngine.matcher("'Name'")("name"))
        assertTrue(FilterEngine.matcher("'Name'")("Name"))
        assertTrue(FilterEngine.matcher("/[\\u0400-\\u052F]/")("English Я"))
        assertTrue(FilterEngine.matcher("*🌈*")("hello 🌈"))
        assertFalse(FilterEngine.matcher("ad")("adventure"))
    }
    @Test fun aggressiveCharactersButAllowPunctuationAndEmoji() {
        val engine = FilterEngine(FilterRules(englishOnly=false,blockNonEnglishCharacters=true))
        for(text in listOf("café", "abc Я", "日本語", "한국어", "中文", "العربية", "e\u0301")) {
            assertFalse(text,engine.reasons(FilterCard(title=text)).isEmpty())
        }
        assertTrue(engine.reasons(FilterCard(title="English — let’s play! 🎮 🌈 ❤️ ☀️")).isEmpty())
        assertTrue(engine.reasons(FilterCard(tags=listOf("ＤＢＤ", "𝒜"))).isEmpty())
    }
    @Test fun wordBoundaryRuleDoesNotMatchLongerWordsAndStyledEnglishIsAllowed() {
        val engine = FilterEngine(FilterRules(titles=listOf("/\\btest\\b/i")))
        assertTrue(engine.reasons(FilterCard(title="Trouble is here | TESTING WITH CAUTION",tags=listOf("ＤＢＤ"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="Playing on TEST servers")).isEmpty())
    }
    @Test fun metadataAloneAndUnknownDoNotBlockByDefault() {
        val engine = FilterEngine(FilterRules(englishOnly=true,detectTitleLanguage=true))
        assertTrue(engine.reasons(FilterCard(language="es",title="English gameplay")).isEmpty())
        assertTrue(engine.reasons(FilterCard(language=null)).isEmpty())
        assertTrue(engine.reasons(FilterCard(language="EN")).isEmpty())
        assertFalse(engine.reasons(FilterCard(tags=listOf("Spanish"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="some title"),"fr").isEmpty())
        assertTrue(engine.reasons(FilterCard(title="hola"),"und").isEmpty())
        assertFalse(FilterEngine(FilterRules(useDeclaredLanguageAlone=true,englishOnly=true)).reasons(FilterCard(language="ES")).isEmpty())
    }
    @Test fun exceptionsOverrideEveryBlock() {
        val engine = FilterEngine(FilterRules(channels=listOf("friend"), tags=listOf("blocked"), allowedChannels=listOf("friend")))
        assertTrue(engine.reasons(FilterCard(login="Friend",title="日本語",tags=listOf("blocked")),"ja").isEmpty())
    }
    @Test fun disabledReallyDisables() {
        assertTrue(FilterEngine(FilterRules(enabled=false,tags=listOf("bad"))).reasons(FilterCard(title="日本語",tags=listOf("bad")),"ja").isEmpty())
    }
    @Test fun commasInRegexArePreservedByImport() {
        val parsed = ContentFilters.parse("""{"categories":[],"channels":[],"tags":["/a,b/i"],"titles":["~test"]}""")
        assertEquals(listOf("/a,b/i"),parsed.tags)
        assertTrue(FilterEngine(parsed).reasons(FilterCard(tags=listOf("A,B"))).isNotEmpty())
    }
    @Test fun rejectInvalidRegexBeforeSaving() {
        try { ContentFilters.parse("""{"tags":["/[broken/"]}"""); fail("Invalid regex accepted") } catch(_: IllegalArgumentException) {}
    }
    @Test fun fullJsonRoundTrip() {
        val source = rules().copy(tags=listOf("~a", "/x/i"),allowedChannels=listOf("friend"))
        val raw = ContentFilters.json.encodeToString(FilterRules.serializer(),source)
        assertEquals(source,ContentFilters.parse(raw))
    }
}
