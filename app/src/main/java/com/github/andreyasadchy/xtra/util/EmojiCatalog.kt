package com.github.andreyasadchy.xtra.util

/** Unicode Emoji 17.0 data; license is included in assets/UNICODE-LICENSE.txt. */
object EmojiCatalog {
    data class Token(val value: String, val key: String, val start: Int, val end: Int)
    private class Node(val children: MutableMap<Char, Node> = HashMap(), var terminal: Boolean = false)
    fun normalize(value: String) = value.replace("\uFE0F", "").replace("\uFE0E", "")
    private val root by lazy {
        Node().also { root -> EmojiSequences.values.forEach { emoji ->
            var node = root
            normalize(emoji).forEach { node = node.children.getOrPut(it) { Node() } }
            node.terminal = true
        } }
    }
    /** Longest sequence wins: a joined family or a flag is one emoji, not its components. */
    fun tokens(value: String): List<Token> {
        val result = mutableListOf<Token>()
        var start = 0
        while (start < value.length) {
            var node = root
            var cursor = start
            var end = -1
            while (cursor < value.length) {
                val char = value[cursor]
                if (char == '\uFE0F' || char == '\uFE0E') {
                    if (cursor == start) break
                    cursor++
                    if (node.terminal) end = cursor
                    continue
                }
                node = node.children[char] ?: break
                cursor++
                if (node.terminal) end = cursor
            }
            if (end > start) {
                val text = value.substring(start, end)
                result += Token(text, normalize(text), start, end)
                start = end
            } else start++
        }
        return result
    }
    fun isSingle(value: String): Boolean = tokens(value).singleOrNull()?.let { it.start == 0 && it.end == value.length } == true
    fun mask(value: String, allowed: Set<String>? = null): String {
        if (allowed?.isEmpty() == true) return value
        val result = StringBuilder(value)
        tokens(value).filter { allowed == null || it.key in allowed }.asReversed().forEach {
            result.replace(it.start, it.end, " ")
        }
        return result.toString()
    }
    /** Only literal, emoji-only rules move; text/regex rules keep their original meaning. */
    fun literalRule(rule: String): String? {
        val text = rule.trim().let {
            when {
                it.startsWith("~") -> it.drop(1)
                it.startsWith("*") && it.endsWith("*") -> it.substring(1, it.length - 1)
                it.startsWith("'") && it.endsWith("'") -> it.substring(1, it.length - 1)
                else -> it
            }
        }
        return text.takeIf(::isSingle)
    }
}
