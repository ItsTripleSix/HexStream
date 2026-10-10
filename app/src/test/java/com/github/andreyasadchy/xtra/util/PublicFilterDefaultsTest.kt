package com.github.andreyasadchy.xtra.util

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PublicFilterDefaultsTest {
    private fun assertNeutral(rules: FilterRules) {
        assertTrue(rules.categories.isEmpty())
        assertTrue(rules.channels.isEmpty())
        assertTrue(rules.tags.isEmpty())
        assertTrue(rules.titles.isEmpty())
        assertTrue(rules.allowedWords.isEmpty())
        assertTrue(rules.allowedChannels.isEmpty())
        assertTrue(rules.allowedUsernames.isEmpty())
        assertTrue(rules.allowedTags.isEmpty())
        assertTrue(rules.allowedTitles.isEmpty())
        assertTrue(rules.blockedEmojis.isEmpty())
        assertFalse(rules.blockEmojis)
        assertFalse(rules.englishOnly)
        assertFalse(rules.blockNonEnglishCharacters)
        assertFalse(rules.detectTitleLanguage)
        assertFalse(rules.useDeclaredLanguageAlone)
        assertTrue(FilterEngine(rules).matches(FilterCard(
            login = "sample_channel", title = "日本語 café 🎮", tags = listOf("Spanish"), language = "es"
        ), "es").isEmpty())
    }

    @Test fun freshInstallAssetAndModelAreNeutral() {
        assertNeutral(FilterRules())
        assertNeutral(ContentFilters.parse(File("src/main/assets/starter-filters.json").readText()))
    }

    @Test fun importingOnlyBlockingListsDoesNotEnableLanguageRestrictions() {
        val rules = ContentFilters.parse("""{"categories":[],"channels":[],"tags":[],"titles":[]}""")
        assertNeutral(rules)
    }

    @Test fun explicitlySavedLanguageSettingsAndRulesArePreserved() {
        val saved = ContentFilters.parse("""{"tags":["example-rule"],"englishOnly":true,"blockNonEnglishCharacters":true,"detectTitleLanguage":true}""")
        assertEquals(listOf("example-rule"), saved.tags)
        assertTrue(saved.englishOnly)
        assertTrue(saved.blockNonEnglishCharacters)
        assertTrue(saved.detectTitleLanguage)
    }
}
