package com.github.andreyasadchy.xtra.ui.main

import com.github.andreyasadchy.xtra.R

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.github.andreyasadchy.xtra.util.ContentFilters
import com.github.andreyasadchy.xtra.util.FilterRules
import com.github.andreyasadchy.xtra.util.withEmojiList

class FilterRulesActivity : AppCompatActivity() {
    private val section get() = intent.getStringExtra("section") ?: "tags"
    private val emojiSection get() = section == "blockedEmojis" || section == "allowedEmojis"
    private lateinit var rules: FilterRules
    private lateinit var list: ListView
    private lateinit var count: TextView
    private lateinit var search: EditText
    private var visible: List<String> = emptyList()
    private fun entries() = when(section) {
        "channels" -> rules.channels; "titles" -> rules.titles; "categories" -> rules.categories
        "allowedChannels" -> rules.allowedChannels; "allowedUsernames" -> rules.allowedUsernames
        "allowedTitles" -> rules.allowedTitles; "allowedTags" -> rules.allowedTags
        "blockedEmojis" -> rules.blockedEmojis; "allowedEmojis" -> rules.allowedEmojis
        "allowedWords" -> rules.allowedWords; else -> rules.tags
    }
    private fun save(values: List<String>) {
        val next = when(section) {
            "channels" -> rules.copy(channels = values); "titles" -> rules.copy(titles = values)
            "categories" -> rules.copy(categories = values); "allowedChannels" -> rules.copy(allowedChannels = values)
            "allowedUsernames" -> rules.copy(allowedUsernames = values); "allowedTitles" -> rules.copy(allowedTitles = values)
            "allowedTags" -> rules.copy(allowedTags = values); "allowedWords" -> rules.copy(allowedWords = values)
            "blockedEmojis" -> rules.withEmojiList(values, allow = false)
            "allowedEmojis" -> rules.withEmojiList(values, allow = true)
            else -> rules.copy(tags = values)
        }
        ContentFilters.save(next); rules = ContentFilters.read(); refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); rules = ContentFilters.read()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad,pad + 28,pad,pad + 32) }
        root.addView(TextView(this).apply { text = when(section) {
            "allowedChannels" -> "Allowed channels"; "allowedUsernames" -> "Allowed usernames"
            "allowedTitles" -> "Allowed titles"; "allowedTags" -> "Allowed tags"
            "blockedEmojis" -> "Blocked emojis"; "allowedEmojis" -> "Allowed emojis"
            "allowedWords" -> "Allowed words"; else -> getString(R.string.hex_blocked_section, getString(when (section) { "channels" -> R.string.hex_rule_section_channels; "titles" -> R.string.hex_rule_section_titles; "categories" -> R.string.hex_rule_section_categories; else -> R.string.hex_rule_section_tags }))
        }; textSize = 24f })
        val description = when(section) {
            "allowedChannels" -> "A matching login or display name lets this creator bypass all filters."
            "allowedUsernames" -> "Ignore blocking matches in that login or display name. Titles, tags and other names are still checked."
            "allowedTitles" -> "Skip title blocking and title language checks for a matching title. Names, tags and Twitch's language setting are still checked. For one accented word, use Allowed words instead."
            "allowedTags" -> "Ignore a matching tag. Other tags, names and the title are still checked."
            "allowedWords" -> "Protect complete words from custom word matches, character and title-language checks. The list starts empty; add your own exceptions. Other words, language tags and Twitch's language setting remain checked."
            "blockedEmojis" -> "Block these specific emojis in names, titles and tags. Adding one removes its matching allowed-emoji exception. Other field/channel exceptions still take priority."
            "allowedEmojis" -> "Allow these specific emojis even when emojis are blocked by default. Adding one removes its matching block. Other emojis and text remain checked."
            else -> "Tap a rule to edit or remove it. Rules apply to every account."
        }
        root.addView(TextView(this).apply { text = description; setPadding(0,pad,0,pad) })
        search = EditText(this).apply { hint = getString(R.string.hex_search_rules); isSingleLine = true }
        root.addView(search)
        val buttons = LinearLayout(this)
        buttons.addView(Button(this).apply { text = if (emojiSection) "＋ Add emoji" else if(section == "allowedWords") "＋ Add word" else "＋ Add rule"; setOnClickListener { editor(null) } }, LinearLayout.LayoutParams(0,-2,1f))
        buttons.addView(Button(this).apply { text = getString(R.string.hex_done); setOnClickListener { finish() } }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(buttons)
        count = TextView(this); root.addView(count)
        list = ListView(this); root.addView(list, LinearLayout.LayoutParams(-1,0,1f))
        list.setOnItemClickListener { _, _, position, _ -> editor(visible[position]) }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
        setContentView(root); search.setText(intent.getStringExtra("search").orEmpty()); refresh()
    }
    private fun refresh() {
        visible = entries().filter { it.contains(search.text.toString(), ignoreCase = true) }
        count.text = getString(R.string.hex_rules_count, visible.size, entries().size)
        val labels = visible.map { rule -> when {
            section == "allowedWords" -> "Word · $rule"
            emojiSection -> "${if (section == "allowedEmojis") "Allowed" else "Blocked"} · $rule"
            rule.startsWith("~") -> "Contains · ${rule.drop(1)}"
            rule.startsWith("/") -> "Regex · $rule"
            rule.startsWith("'") -> "Case exact · $rule"
            else -> "Exact · $rule"
        } }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }
    private fun editor(old: String?) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,12,32,12) }
        val modes = arrayOf("Exact", "Contains", "Regex", "Case-sensitive exact")
        val mode = Spinner(this).apply { adapter = ArrayAdapter(this@FilterRulesActivity, android.R.layout.simple_spinner_dropdown_item, modes) }
        val input = EditText(this).apply { hint = when(section) { "blockedEmojis", "allowedEmojis" -> "Paste one complete emoji"; "allowedWords" -> "Word to allow"; "channels", "allowedChannels", "allowedUsernames" -> "Channel login or display name"; else -> "Text to match" }; setText(old.orEmpty()) }
        if(old != null) when {
            old.startsWith("~") -> { mode.setSelection(1); input.setText(old.drop(1)) }
            old.startsWith("/") -> mode.setSelection(2)
            old.startsWith("'") && old.endsWith("'") -> { mode.setSelection(3); input.setText(old.substring(1,old.length-1)) }
        }
        if(section != "allowedWords" && !emojiSection) panel.addView(mode)
        panel.addView(input)
        panel.addView(TextView(this).apply { text = if (emojiSection) "One complete emoji, such as 🔴, 👍🏽 or 👨‍👩‍👧‍👦. No words or patterns. Saving moves it out of the opposite emoji list."
            else if(section == "allowedWords")
            "One word, such as café or Xbox. Case and accent variants match too. No patterns or spaces. Only that complete word is protected; other text stays checked."
            else "Exact matches the whole field. Contains matches anywhere. Regex uses /pattern/ or /pattern/i. Tags are checked individually. Allow rules exempt only the matching field, except Allowed channels which exempts the whole creator." })
        val builder = AlertDialog.Builder(this).setTitle(if (emojiSection) { if(old == null) "Add emoji" else "Edit emoji" } else if(section == "allowedWords") { if(old == null) "Add word" else "Edit word" } else { if(old == null) "Add rule" else "Edit rule" }).setView(panel)
            .setNegativeButton("Cancel",null).setPositiveButton("Save",null)
        if(old != null) builder.setNeutralButton("Remove") { _, _ -> save(entries().filterNot { it == old }) }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val text = input.text.toString().trim(); require(text.isNotEmpty()) { "Enter text to match" }
                    val value = when(mode.selectedItemPosition) { 1 -> "~$text"; 2 -> { require(text.startsWith("/")) { "Use /pattern/ or /pattern/i" }; text }; 3 -> "'$text'"; else -> text }
                    save((entries().filterNot { it == old } + value).distinct()); dialog.dismiss()
                } catch(e: Exception) { input.error = e.message }
            }
        }; dialog.show()
    }
}
