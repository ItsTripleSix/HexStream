package com.github.andreyasadchy.xtra.util

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FilterEngineCacheTest {
    private fun rules(tag: String) = FilterRules(tags = listOf(tag), englishOnly = false,
        blockNonEnglishCharacters = false, detectTitleLanguage = false)

    @Test fun equivalentRulesReuseCompiledEngineAndEditsReplaceIt() {
        val cache = FilterEngineCache()
        val first = cache.get(rules("blocked"))
        assertSame(first, cache.get(rules("blocked")))
        val edited = cache.get(rules("different"))
        assertNotSame(first, edited)
        assertTrue(edited.matches(FilterCard(tags = listOf("blocked"))).isEmpty())
        assertFalse(edited.matches(FilterCard(tags = listOf("different"))).isEmpty())
        cache.clear()
        assertNotSame(edited, cache.get(rules("different")))
    }

    @Test fun parallelLoadsNeverReceiveAnEngineForAnotherRuleSet() {
        val cache = FilterEngineCache()
        val pool = Executors.newFixedThreadPool(6)
        try {
            val futures = pool.invokeAll((0 until 6).map { worker -> Callable {
                repeat(500) { iteration ->
                    val tag = if ((worker + iteration) % 2 == 0) "first" else "second"
                    val requested = rules(tag)
                    val engine = cache.get(requested)
                    assertEquals(requested, engine.rules)
                    assertFalse(engine.matches(FilterCard(tags = listOf(tag))).isEmpty())
                    assertTrue(engine.matches(FilterCard(tags = listOf(if (tag == "first") "second" else "first"))).isEmpty())
                }
            } })
            futures.forEach { it.get(15, TimeUnit.SECONDS) }
        } finally { pool.shutdownNow() }
    }
}
