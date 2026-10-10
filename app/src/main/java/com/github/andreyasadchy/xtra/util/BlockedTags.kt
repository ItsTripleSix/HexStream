package com.github.andreyasadchy.xtra.util

import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.User
import java.util.Locale

object BlockedTags {
    const val KEY = "prototype_blocked_tags"
    fun normalize(tag: String) = tag.trim().lowercase(Locale.ROOT)
    fun parse(text: String): Set<String> = text.split('\n', ',').map(::normalize).filter { it.isNotBlank() }.toSet()
    fun matches(tags: List<String>?, blocked: Set<String>) = tags.orEmpty().any { normalize(it) in blocked }
    fun <T> filter(items: List<T>): List<T> {
        val blocked = XtraApp.INSTANCE.prefs().getStringSet(KEY, emptySet()).orEmpty()
        return filter(items, blocked)
    }
    fun <T> filter(items: List<T>, blocked: Set<String>): List<T> {
        if (blocked.isEmpty()) return items
        return items.filterNot { item ->
            matches(when (item) {
                is Stream -> item.tags
                is Game -> item.tags?.mapNotNull { it.name }
                is User -> item.tags
                else -> null
            }, blocked)
        }
    }
}
