package com.github.andreyasadchy.xtra.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Android's Unicode matching; no credentials, follows or preferences are changed. */
@RunWith(AndroidJUnit4::class)
class AllowListsDeviceTest {
    @Test fun unicodeWordExceptionAndFieldScopingWorkOnAndroid() {
        val engine = FilterEngine(FilterRules(allowedWords=listOf("Pokémon"),blockNonEnglishCharacters=true))
        assertTrue(engine.reasons(FilterCard(title="Playing Pokémon ❤️", tags=listOf("Poke\u0301mon"))).isEmpty())
        assertFalse(engine.reasons(FilterCard(title="Pokémon 日本語")).isEmpty())
        val tagException = FilterEngine(FilterRules(tags=listOf("spanish"),allowedTags=listOf("Spanish"),blockNonEnglishCharacters=true))
        assertTrue(tagException.reasons(FilterCard(tags=listOf("Spanish"))).isEmpty())
        assertFalse(tagException.reasons(FilterCard(tags=listOf("Spanish","日本語"))).isEmpty())
    }
}
