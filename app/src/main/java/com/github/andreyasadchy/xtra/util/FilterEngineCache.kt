package com.github.andreyasadchy.xtra.util

/** Publish rules and their compiled engine together; parallel page loads may use different rules. */
internal class FilterEngineCache {
    private data class Entry(val rules: FilterRules, val engine: FilterEngine)
    @Volatile private var entry: Entry? = null

    fun get(rules: FilterRules): FilterEngine {
        entry?.takeIf { it.rules == rules }?.let { return it.engine }
        return synchronized(this) {
            entry?.takeIf { it.rules == rules }?.engine ?: FilterEngine(rules).also {
                entry = Entry(rules, it)
            }
        }
    }

    fun clear() = synchronized(this) { entry = null }
}
