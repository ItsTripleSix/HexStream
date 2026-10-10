package com.github.andreyasadchy.xtra.util

import org.junit.Assert.*
import org.junit.Test

class CompleteTagListTest {
    @Test fun blockedTenthTagIsCheckedEvenWhenNotVisibleOnCard() {
        val tags = (1..9).map { "tag$it" } + "blocked"
        val engine = FilterEngine(FilterRules(tags = listOf("blocked"), englishOnly = false, blockNonEnglishCharacters = false))
        assertFalse(engine.reasons(FilterCard(tags = tags)).isEmpty())
    }
    @Test fun allowingOneTagDoesNotHideAnotherBlockedTag() {
        val engine = FilterEngine(FilterRules(tags = listOf("blocked"), allowedTags = listOf("allowed"), englishOnly = false, blockNonEnglishCharacters = false))
        assertFalse(engine.reasons(FilterCard(tags = listOf("allowed", "blocked"))).isEmpty())
    }
}
