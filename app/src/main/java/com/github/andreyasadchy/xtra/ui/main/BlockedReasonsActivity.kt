package com.github.andreyasadchy.xtra.ui.main

import com.github.andreyasadchy.xtra.R

import android.content.Intent
import androidx.core.graphics.toColorInt
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.github.andreyasadchy.xtra.util.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** A readable, searchable view of the actual checks recorded when an item was hidden. */
class BlockedReasonsActivity : AppCompatActivity() {
    private val ink = "#FAF7FF".toColorInt()
    private val muted = "#BEB6CB".toColorInt()
    private val accent = "#CEB9FF".toColorInt()
    private val surface = "#211D2A".toColorInt()
    private var items = emptyList<BlockedItem>()
    private val cards = Cards()
    private var mode = 0
    private lateinit var search: EditText
    private lateinit var count: TextView
    private lateinit var empty: TextView
    private lateinit var list: RecyclerView
    private val expanded = mutableSetOf<FilterCard>()
    private val tabs = mutableListOf<TextView>()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, border: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(18).toFloat(); setStroke(dp(1), border)
    }
    private fun label(value: String, size: Float = 14f, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun action(value: String, click: () -> Unit) = label(value, 14f, accent, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(12), 0, dp(12), 0)
        isClickable = true; isFocusable = true; setOnClickListener { click() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = savedInstanceState?.getInt("mode") ?: 0
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor("#121017".toColorInt()) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(8), dp(8), 0) }
        header.addView(action("‹ Back") { finish() })
        header.addView(label("Why blocked", 24f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(action("Clear") {
            AlertDialog.Builder(this).setTitle("Clear recent blocks?").setMessage("This clears the explanation history only. Your filtering rules stay saved.")
                .setNegativeButton("Cancel", null).setPositiveButton("Clear") { _, _ -> ContentFilters.clearReasons(); reload() }.show()
        })
        root.addView(header)
        count = label("", 14f, muted).apply { setPadding(dp(20), dp(10), dp(20), dp(12)) }; root.addView(count)
        search = EditText(this).apply {
            hint = getString(R.string.hex_search_blocked); textSize = 15f; isSingleLine = true
            setTextColor(ink); setHintTextColor(muted); setPadding(dp(16), dp(12), dp(16), dp(12))
            background = shape(surface, "#393142".toColorInt())
            setText(savedInstanceState?.getString("query").orEmpty())
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(12)) })
        val row = LinearLayout(this).apply { setPadding(dp(12), 0, dp(12), 0) }
        listOf("All", "Language", "Rules", "Emojis").forEachIndexed { index, title ->
            val tab = action(title) { mode = index; refresh() }.apply { isSingleLine = true; minHeight = dp(48); minWidth = dp(60) }; tabs += tab
            row.addView(tab, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) })
        root.addView(label("Tap a card to see the exact match.", 13f, muted).apply { setPadding(dp(20), dp(12), dp(20), dp(10)) })
        empty = label("", 16f, muted).apply { gravity = Gravity.CENTER; setPadding(dp(32), dp(40), dp(32), dp(40)) }
        root.addView(empty)
        list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@BlockedReasonsActivity); adapter = cards; clipToPadding = false; setPadding(dp(16), 0, dp(16), dp(16)) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(label("Recorded when hidden · last 100 items this session.\nRefresh browsing after changing a rule.", 12f, muted).apply { setPadding(dp(20), dp(8), dp(20), dp(12)) })
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
        setContentView(root); reload()
    }
    override fun onResume() { super.onResume(); if (::list.isInitialized) reload() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mode", mode); outState.putString("query", search.text.toString()); super.onSaveInstanceState(outState)
    }
    private fun reload() { items = ContentFilters.recentBlocks(); refresh() }
    private fun refresh() {
        if (!::list.isInitialized) return
        val query = search.text.toString().trim()
        val shown = items.filter { item ->
            (mode == 0 || mode == 1 && item.hasLanguage || mode == 2 && item.hasRules || mode == 3 && item.hasEmoji) &&
                (query.isEmpty() || (listOfNotNull(item.label, item.card.title, item.card.name) + item.matches.flatMap { listOf(it.description(), summary(it)) }).any { it.contains(query, true) })
        }
        count.text = getString(R.string.hex_blocks_count, shown.size, items.size)
        tabs.forEachIndexed { index, tab -> tab.background = shape(if (mode == index) "#493765".toColorInt() else surface); tab.setTextColor(if (mode == index) ink else muted) }
        empty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        empty.text = if (items.isEmpty()) "No blocks recorded yet.\nBrowse streams, then come back here." else "No matching blocks.\nTry another search or section."
        cards.submitList(shown)
    }
    private fun language(code: String?) = code?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH).ifBlank { it } } ?: "Unknown"
    private fun ruleLabel(rule: String?) = when {
        rule == null -> ""
        rule.startsWith("~") -> "Contains “${rule.drop(1)}”"
        rule.startsWith("*") && rule.endsWith("*") -> "Contains “${rule.trim('*')}”"
        rule.startsWith("/") -> "Regex: $rule"
        rule.startsWith("'") -> "Case-sensitive exact: $rule"
        else -> "Exactly “$rule”"
    }
    private fun summary(match: FilterMatch) = when (match.cause) {
        FilterCause.RULE -> "${match.field} · ${ruleLabel(match.rule)}"
        FilterCause.CHARACTERS -> "${match.field} has foreign letters or accents: ${match.evidence}"
        FilterCause.LANGUAGE_TAG -> "Language tag · ${match.value}"
        FilterCause.DECLARED_LANGUAGE -> "Twitch language · ${language(match.language)}"
        FilterCause.TITLE_LANGUAGE -> "Title detected as ${language(match.language)}"
        FilterCause.EMOJI -> "${match.field} · ${match.evidence} ${if (match.rule == null) "blocked by the default emoji setting" else "is in Blocked emojis"}"
    }
    private inner class Holder(val panel: LinearLayout) : RecyclerView.ViewHolder(panel)
    private inner class Cards : ListAdapter<BlockedItem, Holder>(object : DiffUtil.ItemCallback<BlockedItem>() {
        override fun areItemsTheSame(old: BlockedItem, new: BlockedItem) = old.key == new.key
        override fun areContentsTheSame(old: BlockedItem, new: BlockedItem) = old == new
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(LinearLayout(this@BlockedReasonsActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = shape(surface, "#3C3449".toColorInt())
            layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = getItem(position); val panel = holder.panel; panel.removeAllViews()
            val open = item.key in expanded
            val heading = LinearLayout(this@BlockedReasonsActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            heading.addView(label(item.card.name ?: item.label, 18f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            heading.addView(label(if (open) "▴" else "▾", 22f, accent))
            panel.addView(heading)
            item.card.login?.let { login -> panel.addView(label("Username: @$login", 13f, muted).apply { setPadding(0, dp(6), 0, 0) }) }
            panel.addView(label("Game: ${item.card.category ?: "Not provided"}", 13f, muted).apply { setPadding(0, dp(5), 0, dp(5)) })
            panel.addView(label("${item.matches.size} matching ${if (item.matches.size == 1) "check" else "checks"} · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.recordedAt))}", 12f, muted).apply { setPadding(0, dp(5), 0, dp(10)) })
            val badges = LinearLayout(this@BlockedReasonsActivity)
            listOf("Language" to item.hasLanguage, "Rules" to item.hasRules, "Emojis" to item.hasEmoji).filter { it.second }.forEach { (name, _) ->
                badges.addView(label(name, 12f, accent, true).apply { background = shape("#352B46".toColorInt()); setPadding(dp(10), dp(5), dp(10), dp(5)) }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
            }
            panel.addView(badges)
            val priority = item.matches.firstOrNull { it.cause != FilterCause.RULE && it.cause != FilterCause.EMOJI } ?: item.matches.first()
            panel.addView(label(summary(priority), 14f).apply { setPadding(0, dp(12), 0, 0); maxLines = if (open) Int.MAX_VALUE else 2 })
            panel.isClickable = true; panel.isFocusable = true
            panel.contentDescription = "${item.label}, ${item.matches.size} checks. ${if (open) "Collapse" else "Expand"} blocking details"
            panel.setOnClickListener { if (!expanded.add(item.key)) expanded.remove(item.key); val at = holder.bindingAdapterPosition; if (at != RecyclerView.NO_POSITION) notifyItemChanged(at) }
            if (open) {
                item.matches.groupBy { when (it.cause) { FilterCause.RULE -> "BLOCKING RULES"; FilterCause.EMOJI -> "EMOJI CHECKS"; else -> "LANGUAGE CHECKS" } }.forEach { (section, matches) ->
                    panel.addView(View(this@BlockedReasonsActivity).apply { setBackgroundColor("#42394F".toColorInt()) }, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(18); bottomMargin = dp(14) })
                    panel.addView(label(section, 12f, accent, true))
                    matches.forEach { match ->
                        panel.addView(label(summary(match), 15f, ink, true).apply { setPadding(0, dp(12), 0, dp(6)) })
                        if (match.cause == FilterCause.TITLE_LANGUAGE) panel.addView(label("On-device title detection · at least 90% confidence.", 12f, muted))
                        panel.addView(label("Matched ${match.field.lowercase(Locale.ROOT)}", 12f, muted).apply { setPadding(0, dp(4), 0, dp(4)) })
                        panel.addView(label(match.value, 14f).apply { setTextIsSelectable(true); background = shape("#17131D".toColorInt()); setPadding(dp(12), dp(10), dp(12), dp(10)) })
                        if (match.cause == FilterCause.RULE || match.cause == FilterCause.EMOJI) {
                            panel.addView(action("Edit this ${if (match.cause == FilterCause.EMOJI) "emoji rule" else "rule"}") {
                                val sectionKey = if (match.cause == FilterCause.EMOJI) "blockedEmojis" else when (match.field) { "Channel" -> "channels"; "Title" -> "titles"; "Category" -> "categories"; else -> "tags" }
                                startActivity(Intent(this@BlockedReasonsActivity, FilterRulesActivity::class.java).putExtra("section", sectionKey).putExtra("search", match.rule ?: match.evidence.orEmpty()))
                            })
                        }
                    }
                }
                panel.addView(action("Allow something on this card…") { QuickFilterActions.show(this@BlockedReasonsActivity, item.card, allowOnly = true) })
            }
        }
    }
}
