package com.github.andreyasadchy.xtra.util

import androidx.core.content.edit
import com.github.andreyasadchy.xtra.XtraApp
import com.github.andreyasadchy.xtra.model.ui.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.encodeToString
import kotlin.coroutines.resume

object ContentFilters {
    const val KEY = "prototype_content_filters_v2"
    val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val engineCache = FilterEngineCache()
    private val recent = LinkedHashMap<FilterCard, BlockedItem>()
    private val titleCache = object : LinkedHashMap<String, String>(256, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 512
    }
    private val identifier by lazy { LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder().setConfidenceThreshold(.90f).build()) }

    fun read(): FilterRules {
        val prefs = XtraApp.INSTANCE.prefs()
        val raw = prefs.getString(KEY, null)
        var rules = if (raw != null) json.decodeFromString<FilterRules>(raw) else {
            val starter = XtraApp.INSTANCE.assets.open("starter-filters.json").bufferedReader().use { it.readText() }
            val decoded = json.decodeFromString<FilterRules>(starter)
            decoded.copy(tags = (decoded.tags + prefs.getStringSet(BlockedTags.KEY, emptySet()).orEmpty()).distinct())
        }
        val splitEmoji = !prefs.getBoolean("prototype_emoji_rules_split_v5", false)
        if (splitEmoji) rules = rules.withSeparateEmojiRules()
        if (raw == null || splitEmoji) {
            save(rules)
            prefs.edit {
                putBoolean("prototype_emoji_rules_split_v5", true)
            }
        }
        return rules
    }
    fun parse(raw: String): FilterRules {
        require(raw.length <= 2_000_000) { "File is too large" }
        require(json.parseToJsonElement(raw).jsonObject.keys.any { it in listOf("categories", "channels", "tags", "titles") }) { "This is not an Unwanted Twitch or Xtra filters file" }
        val rules = json.decodeFromString<FilterRules>(raw).withSeparateEmojiRules()
        require(listOf(rules.categories, rules.channels, rules.tags, rules.titles, rules.allowedChannels,
            rules.allowedUsernames, rules.allowedTitles, rules.allowedTags, rules.allowedWords,
            rules.blockedEmojis, rules.allowedEmojis).sumOf { it.size } <= 10000) { "Too many rules" }
        require(rules.allowedWords.all { it.isNotBlank() && it.length <= 256 && it.matches(Regex("[\\p{L}\\p{M}\\p{N}_]+")) }) { "Allowed words must be individual words, without patterns or spaces" }
        require((rules.blockedEmojis + rules.allowedEmojis).all { it.length <= 64 && EmojiCatalog.isSingle(it) }) { "Use one complete emoji per entry, including its skin tone or joined parts" }
        FilterEngine(rules) // Validate every regex before replacing working settings.
        rules.allowedChannels.forEach { FilterEngine.matcher(it) }
        return rules
    }
    fun save(rules: FilterRules) {
        val raw = json.encodeToString(parse(json.encodeToString(rules)))
        check(XtraApp.INSTANCE.prefs().edit().putString(KEY, raw).commit()) { "Could not save filters" }
        engineCache.clear()
    }
    fun recentBlocks(): List<BlockedItem> = synchronized(recent) { recent.values.toList().asReversed() }
    fun clearReasons() = synchronized(recent) { recent.clear() }
    fun card(item: Any?): FilterCard = when (item) {
        is Stream -> FilterCard(item.channelLogin, item.channelName, item.title, item.gameName, item.tags.orEmpty(), item.language)
        is User -> FilterCard(item.login, item.name, item.title, item.gameName, item.tags.orEmpty(), item.language)
        is Game -> FilterCard(category = item.name, tags = item.tags.orEmpty().mapNotNull { it.name })
        is Video -> FilterCard(item.channelLogin, item.channelName, item.title, item.gameName)
        is Clip -> FilterCard(item.channelLogin, item.channelName, item.title, item.gameName)
        else -> FilterCard()
    }
    suspend fun detectTitle(title: String?): String? {
        // Short titles, emote codes and game names are poor language evidence.
        if (title == null || title.count { it.isLetter() } < 20) return null
        synchronized(titleCache) { titleCache[title]?.let { return it } }
        val result = withTimeoutOrNull(1500) {
            suspend fun identify(text: String): String = suspendCancellableCoroutine { cont ->
                identifier.identifyLanguage(text).addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else "und")
                }
            }
            // Decorations are not language evidence. A leading camel-case handle can dominate a
            // short sentence; only retry without it when the first result is uncertain.
            val text = title.take(1000).replace(Regex("[\\p{P}\\p{S}]+"), " ")
                .replace(Regex("\\s+"), " ").trim()
            val first = identify(text)
            val sentence = text.replaceFirst(Regex("^[A-Z][a-z]+[A-Z][A-Za-z0-9]*\\s+"), "")
            if (first == "und" && sentence != text && sentence.count { it.isLetter() } >= 20) identify(sentence) else first
        } ?: "und"
        synchronized(titleCache) { titleCache[title] = result }
        return result
    }
    suspend fun <T> filter(items: List<T>): List<T> {
        val rules = read()
        if (!rules.enabled) return items
        val engine = engineCache.get(rules)
        val followedIds = if (rules.allowFollowedChannels) FollowExemptions.current() else emptySet()
        val kept = ArrayList<T>()
        for (item in items) {
            val card = card(item)
            val channelId = when (item) { is Stream -> item.channelId; is User -> item.id; is Video -> item.channelId; is Clip -> item.channelId; else -> null }
            val followed = channelId != null && channelId in followedIds
            var reasons = engine.matches(card, followed = followed)
            // Record independent language evidence even when a custom rule already hides the item.
            if (rules.englishOnly && rules.detectTitleLanguage && !engine.exemptsCreator(card, followed)) {
                reasons = engine.matches(card, detectTitle(engine.titleForDetection(card)), followed)
            }
            if (reasons.isEmpty()) kept += item
            else synchronized(recent) {
                recent.remove(card)
                recent[card] = BlockedItem(card, reasons)
                while (recent.size > 100) recent.remove(recent.keys.first())
            }
        }
        return kept
    }
}
