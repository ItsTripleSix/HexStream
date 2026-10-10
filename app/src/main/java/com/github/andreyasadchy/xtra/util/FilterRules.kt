package com.github.andreyasadchy.xtra.util

import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

@Serializable
data class FilterRules(
    val categories: List<String> = emptyList(),
    val channels: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val titles: List<String> = emptyList(),
    val allowedChannels: List<String> = emptyList(),
    val allowedUsernames: List<String> = emptyList(),
    val allowedTitles: List<String> = emptyList(),
    val allowedTags: List<String> = emptyList(),
    val allowedWords: List<String> = emptyList(),
    val blockEmojis: Boolean = false,
    val blockedEmojis: List<String> = emptyList(),
    val allowedEmojis: List<String> = emptyList(),
    val enabled: Boolean = true,
    val englishOnly: Boolean = false,
    val blockNonEnglishCharacters: Boolean = false,
    val detectTitleLanguage: Boolean = false,
    val useDeclaredLanguageAlone: Boolean = false,
    val allowFollowedChannels: Boolean = true,
)

fun FilterRules.withSeparateEmojiRules(): FilterRules {
    val emojiRules = (channels + titles + tags).mapNotNull(EmojiCatalog::literalRule)
    return copy(channels = channels.filter { EmojiCatalog.literalRule(it) == null },
        titles = titles.filter { EmojiCatalog.literalRule(it) == null },
        tags = tags.filter { EmojiCatalog.literalRule(it) == null },
        blockedEmojis = (blockedEmojis + emojiRules).distinctBy(EmojiCatalog::normalize)
            .filter { blocked -> allowedEmojis.none { EmojiCatalog.normalize(it) == EmojiCatalog.normalize(blocked) } })
}

/** Editing either list moves matching emojis out of the other list, atomically on save. */
fun FilterRules.withEmojiList(values: List<String>, allow: Boolean): FilterRules {
    val distinct = values.distinctBy(EmojiCatalog::normalize)
    val keys = distinct.map(EmojiCatalog::normalize).toSet()
    return if (allow) copy(allowedEmojis = distinct, blockedEmojis = blockedEmojis.filter { EmojiCatalog.normalize(it) !in keys })
        else copy(blockedEmojis = distinct, allowedEmojis = allowedEmojis.filter { EmojiCatalog.normalize(it) !in keys })
}

data class FilterCard(
    val login: String? = null, val name: String? = null,
    val title: String? = null, val category: String? = null,
    val tags: List<String> = emptyList(), val language: String? = null,
)

enum class FilterCause { RULE, CHARACTERS, LANGUAGE_TAG, DECLARED_LANGUAGE, TITLE_LANGUAGE, EMOJI }

data class FilterMatch(
    val cause: FilterCause, val field: String, val value: String,
    val rule: String? = null, val evidence: String? = null, val language: String? = null,
) {
    fun description(): String = when (cause) {
        FilterCause.RULE -> "$field: $rule → $value"
        FilterCause.CHARACTERS -> "Non-English letters or accents: $value"
        FilterCause.LANGUAGE_TAG -> "Language tag: $value"
        FilterCause.DECLARED_LANGUAGE -> "Declared language: $language"
        FilterCause.TITLE_LANGUAGE -> "Title language: $language"
        FilterCause.EMOJI -> "Emoji in $field: $evidence"
    }
}

data class BlockedItem(val card: FilterCard, val matches: List<FilterMatch>, val recordedAt: Long = System.currentTimeMillis()) {
    val label get() = card.login ?: card.name ?: card.category ?: card.title ?: "Item"
    val hasLanguage get() = matches.any { it.cause != FilterCause.RULE && it.cause != FilterCause.EMOJI }
    val hasRules get() = matches.any { it.cause == FilterCause.RULE }
    val hasEmoji get() = matches.any { it.cause == FilterCause.EMOJI }
    val key get() = card
}

/** Compatibility with Unwanted Twitch: exact, ~contains, /regex/flags, 'case exact'. */
class FilterEngine(val rules: FilterRules) {
    private val compiled = listOf("Category" to rules.categories, "Channel" to rules.channels,
        "Tag" to rules.tags, "Title" to rules.titles).map { (kind, list) ->
        kind to list.map { it to matcher(it) }
    }
    private val allowed = rules.allowedChannels.map(::matcher)
    private val allowedNames = rules.allowedUsernames.map(::matcher)
    private val allowedTitles = rules.allowedTitles.map(::matcher)
    private val allowedTags = rules.allowedTags.map(::matcher)
    private val words = rules.allowedWords.map(::normalize).toSet()
    private val allowedEmoji = rules.allowedEmojis.map(EmojiCatalog::normalize).toSet()
    private val blockedEmoji = rules.blockedEmojis.map(EmojiCatalog::normalize).toSet()
    private val wordTokens = Regex("[\\p{L}\\p{M}\\p{N}_]+")
    /** Ignore complete allowed words; never exempt other words or the whole creator. */
    fun withoutAllowedWords(value: String): String = wordTokens.replace(value) { match ->
        if (normalize(match.value) in words) " " else match.value
    }
    private fun checked(card: FilterCard) = card.copy(
        login = card.login?.takeUnless { value -> allowedNames.any { it(value) } },
        name = card.name?.takeUnless { value -> allowedNames.any { it(value) } },
        title = card.title?.takeUnless { value -> allowedTitles.any { it(value) } },
        tags = card.tags.filterNot { value -> allowedTags.any { it(value) } },
    )
    fun titleForDetection(card: FilterCard): String? = checked(card).title?.let { EmojiCatalog.mask(withoutAllowedWords(it)) }
    fun exemptsCreator(card: FilterCard, followed: Boolean = false) = !rules.enabled ||
        (rules.allowFollowedChannels && followed) || allowed.any { it(card.login.orEmpty()) || it(card.name.orEmpty()) }

    fun reasons(card: FilterCard, detectedLanguage: String? = null, followed: Boolean = false): List<String> =
        matches(card, detectedLanguage, followed).map(FilterMatch::description)

    fun matches(card: FilterCard, detectedLanguage: String? = null, followed: Boolean = false): List<FilterMatch> {
        if (exemptsCreator(card, followed)) return emptyList()
        val checked = checked(card)
        val reasons = mutableListOf<FilterMatch>()
        val fields = listOf(listOfNotNull(checked.category), listOfNotNull(checked.login, checked.name),
            checked.tags, listOfNotNull(checked.title))
        compiled.forEachIndexed { i, (kind, patterns) ->
            val protected = fields[i].associateWith { if (kind == "Category") it else EmojiCatalog.mask(withoutAllowedWords(it), allowedEmoji) }
            patterns.forEach { (rule, match) ->
                fields[i].firstOrNull { value ->
                    match(value) && match(protected.getValue(value))
                }?.let { reasons += FilterMatch(FilterCause.RULE, kind, it, rule = rule) }
            }
        }
        val values = listOfNotNull(checked.login?.let { "Username" to it },
            checked.name?.let { "Display name" to it }, checked.title?.let { "Title" to it }) + checked.tags.map { "Tag" to it }
        for ((field, value) in values) {
            EmojiCatalog.tokens(value).firstOrNull { it.key !in allowedEmoji && (rules.blockEmojis || it.key in blockedEmoji) }?.let {
                reasons += FilterMatch(FilterCause.EMOJI, field, value, evidence = it.value,
                    rule = if (it.key in blockedEmoji) it.value else null)
            }
        }
        if (rules.blockNonEnglishCharacters) {
            for ((field, value) in values) {
                val offending = Normalizer.normalize(withoutAllowedWords(value), Normalizer.Form.NFKC).codePoints().filter { cp -> cp > 127 && (Character.isLetter(cp) ||
                    (Character.getType(cp) == Character.NON_SPACING_MARK.toInt() && cp !in 0xFE00..0xFE0F && cp !in 0xE0100..0xE01EF)) }
                    .findFirst()
                if (offending.isPresent) {
                    reasons += FilterMatch(FilterCause.CHARACTERS, field, value,
                        evidence = String(Character.toChars(offending.asInt)))
                    break
                }
            }
        }
        if (rules.englishOnly) {
            checked.tags.firstOrNull { normalize(it) in languageTags }?.let { reasons += FilterMatch(FilterCause.LANGUAGE_TAG, "Tag", it) }
            if (rules.useDeclaredLanguageAlone && card.language.nonEnglish()) reasons += FilterMatch(FilterCause.DECLARED_LANGUAGE, "Twitch language", card.language.orEmpty(), language = card.language)
            if (rules.detectTitleLanguage && !titleForDetection(card).isNullOrBlank() && detectedLanguage.nonEnglish()) reasons += FilterMatch(FilterCause.TITLE_LANGUAGE, "Title", checked.title.orEmpty(), language = detectedLanguage)
        }
        return reasons.distinct()
    }

    companion object {
        private val marks = Regex("\\p{M}+")
        private val normalized = object : LinkedHashMap<String, String>(256, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 512
        }
        fun normalize(value: String): String = synchronized(normalized) {
            normalized.getOrPut(value) { Normalizer.normalize(value.trim(), Normalizer.Form.NFKD)
                .replace(marks, "").lowercase(Locale.ROOT) }
        }
        private fun String?.nonEnglish() = !isNullOrBlank() && lowercase(Locale.ROOT) !in setOf("und", "en", "other")
        val languageTags = ("spanish espanol castellano french francais german deutsch portuguese portugues " +
            "italian italiano russian japanese korean chinese mandarin cantonese arabic turkish turkce " +
            "polish polski dutch nederlands thai vietnamese tiengviet indonesian bahasaindonesia " +
            "bahasamelayu hindi hebrew ukrainian ukrainisch svenska swedish norwegian norsk danish dansk " +
            "finnish suomi hungarian magyar romanian romana czech cestina slovak slovencina greek " +
            "русский 日本語 한국어 中文 العربية עברית فارسی हिंदी").split(' ').toSet()
        fun matcher(input: String): (String) -> Boolean {
            val rule = input.trim()
            require(rule.length <= 4096) { "Rule is too long" }
            if (rule.startsWith('/')) {
                val end = rule.lastIndexOf('/')
                require(end > 0) { "Regex needs a closing /: $rule" }
                val flags = rule.substring(end + 1)
                require(flags.all { it in "gimsuyd" }) { "Unsupported regex flags: $flags" }
                var options = 0
                if ('i' in flags) options = options or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
                if ('m' in flags) options = options or Pattern.MULTILINE
                if ('s' in flags) options = options or Pattern.DOTALL
                val pattern = Pattern.compile(rule.substring(1, end), options)
                return { pattern.matcher(it).find() }
            }
            if (rule.length >= 2 && rule.startsWith('\'') && rule.endsWith('\'')) return { it == rule.substring(1, rule.length - 1) }
            if (rule.startsWith('~')) { val needle = normalize(rule.drop(1)); return { normalize(it).contains(needle) } }
            // Accept the user's *text* title entries as explicit contains patterns.
            if (rule.length > 2 && rule.startsWith('*') && rule.endsWith('*')) {
                val needle = normalize(rule.trim('*')); return { normalize(it).contains(needle) }
            }
            val exact = normalize(rule)
            return { normalize(it) == exact }
        }
    }
}
