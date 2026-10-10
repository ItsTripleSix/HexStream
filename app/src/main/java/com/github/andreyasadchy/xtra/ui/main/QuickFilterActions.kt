package com.github.andreyasadchy.xtra.ui.main

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.andreyasadchy.xtra.util.*

/** Selecting an action only previews it. Saving always requires the final Confirm button. */
object QuickFilterActions {
    fun show(context: Context, card: FilterCard, allowOnly: Boolean = false, onSaved: () -> Unit = {}) {
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        fun choice(label: String, section: String, value: String, allow: Boolean) {
            actions += label to { confirm(context, section, value, allow, onSaved) }
        }
        (card.login ?: card.name)?.let {
            if (!allowOnly) choice("Block channel · $it", "channels", it, false)
            choice("Allow channel · $it", "allowedChannels", it, true)
        }
        card.title?.takeIf { it.isNotBlank() }?.let {
            if (!allowOnly) choice("Block this exact title", "titles", it, false)
            choice("Allow this exact title", "allowedTitles", it, true)
        }
        val tags = card.tags.distinct()
        fun tagChoice(allow: Boolean) {
            AlertDialog.Builder(context).setTitle(if (allow) "Choose a tag to allow" else "Choose a tag to block")
                .setItems(tags.toTypedArray()) { _, at -> confirm(context, if (allow) "allowedTags" else "tags", tags[at], allow, onSaved) }
                .setNegativeButton("Cancel", null).show()
        }
        if (tags.isNotEmpty()) {
            if (!allowOnly) actions += "Block a tag…" to { tagChoice(false) }
            actions += "Allow a tag…" to { tagChoice(true) }
        }
        val emojis = (listOfNotNull(card.login, card.name, card.title) + card.tags).flatMap(EmojiCatalog::tokens).map { it.value }.distinctBy(EmojiCatalog::normalize)
        fun emojiChoice(allow: Boolean) {
            AlertDialog.Builder(context).setTitle(if (allow) "Choose an emoji to allow" else "Choose an emoji to block")
                .setItems(emojis.toTypedArray()) { _, at -> confirm(context, if (allow) "allowedEmojis" else "blockedEmojis", emojis[at], allow, onSaved) }
                .setNegativeButton("Cancel", null).show()
        }
        if (emojis.isNotEmpty()) {
            if (!allowOnly) actions += "Block an emoji…" to { emojiChoice(false) }
            actions += "Allow an emoji…" to { emojiChoice(true) }
        }
        AlertDialog.Builder(context).setTitle("Filter · ${card.login ?: card.name ?: "this card"}")
            .setItems(actions.map { it.first }.toTypedArray()) { _, at -> actions[at].second() }
            .setNegativeButton("Cancel", null).show()
    }
    private fun confirm(context: Context, section: String, value: String, allow: Boolean, onSaved: () -> Unit) {
        val kind = when (section) { "channels", "allowedChannels" -> "channel"; "titles", "allowedTitles" -> "title"; "blockedEmojis", "allowedEmojis" -> "emoji"; else -> "tag" }
        val scope = when (section) {
            "allowedChannels" -> "This creator will bypass all stream filters."
            "allowedTitles" -> "Only this matching title is exempt. Names, tags and Twitch's language setting remain checked."
            "allowedTags" -> "Only this tag is exempt. Other tags, the title and names remain checked."
            "allowedEmojis" -> "Only this complete emoji is exempt. Other emojis and text remain checked. This removes its matching blocked-emoji entry."
            "blockedEmojis" -> "This complete emoji will be blocked in names, titles and tags. This removes its matching allowed-emoji exception. Whole-field and channel exceptions still take priority."
            else -> "Add an exact match. Followed-channel and manual exceptions still take priority."
        }
        AlertDialog.Builder(context).setTitle("${if (allow) "Allow" else "Block"} this $kind?")
            .setMessage("$value\n\n$scope\n\nApplies to all saved accounts. No change is made until you confirm.")
            .setNegativeButton("Cancel", null).setPositiveButton("Confirm ${if (allow) "allow" else "block"}") { _, _ ->
                try {
                    val current = ContentFilters.read()
                    // Escape leading pattern syntax so a card's literal title/tag cannot become a regex.
                    val exact = if (kind == "emoji") value else if (value.startsWith("~") || value.startsWith("/") || value.startsWith("'") || value.startsWith("*"))
                        "/^${java.util.regex.Pattern.quote(value)}$/i" else value
                    val next = when (section) {
                        "channels" -> current.copy(channels = (current.channels + exact).distinct())
                        "allowedChannels" -> current.copy(allowedChannels = (current.allowedChannels + exact).distinct())
                        "titles" -> current.copy(titles = (current.titles + exact).distinct())
                        "allowedTitles" -> current.copy(allowedTitles = (current.allowedTitles + exact).distinct())
                        "tags" -> current.copy(tags = (current.tags + exact).distinct())
                        "allowedTags" -> current.copy(allowedTags = (current.allowedTags + exact).distinct())
                        "allowedEmojis" -> current.withEmojiList(current.allowedEmojis + value, allow = true)
                        else -> current.withEmojiList(current.blockedEmojis + value, allow = false)
                    }
                    ContentFilters.save(next); onSaved()
                    Toast.makeText(context, "Saved ${if (allow) "allow" else "block"} rule. Refresh browsing to apply.", Toast.LENGTH_LONG).show()
                } catch (error: Exception) {
                    AlertDialog.Builder(context).setTitle("Could not save rule").setMessage(error.message).setPositiveButton("OK", null).show()
                }
            }.show()
    }
}
