package com.github.andreyasadchy.xtra.ui.main

import com.github.andreyasadchy.xtra.R

import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.github.andreyasadchy.xtra.util.ContentFilters
import com.github.andreyasadchy.xtra.util.FilterRules
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/** Global settings, deliberately outside the account vault and per-account database. */
class FiltersActivity : AppCompatActivity() {
    private lateinit var rules: FilterRules
    private lateinit var layout: LinearLayout
    private var exportCompatible = false
    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = contentResolver.openInputStream(uri)!!.bufferedReader().use { reader ->
                val data = reader.readText(); require(data.length <= 2_000_000) { "File is too large" }; data
            }
            val imported = ContentFilters.parse(raw)
            AlertDialog.Builder(this).setTitle("Import filters?")
                .setMessage("Replace the current rules with ${imported.channels.size} channels, ${imported.tags.size} tags, ${imported.titles.size} titles, ${imported.categories.size} categories and ${imported.blockedEmojis.size} blocked emojis? Saved accounts are unaffected.")
                .setNegativeButton("Cancel", null).setPositiveButton("Import") { _, _ -> update(imported) }.show()
        } catch (e: Exception) { error(e) }
    }
    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) try {
            val data = if (exportCompatible) buildJsonObject {
                put("categories", JsonArray(rules.categories.map(::JsonPrimitive)))
                val emojis = rules.blockedEmojis.map { "~$it" }
                put("channels", JsonArray((rules.channels + emojis).distinct().map(::JsonPrimitive)))
                put("tags", JsonArray((rules.tags + emojis).distinct().map(::JsonPrimitive)))
                put("titles", JsonArray((rules.titles + emojis).distinct().map(::JsonPrimitive)))
            }.toString() else ContentFilters.json.encodeToString(rules)
            contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(data) }
            Toast.makeText(this, "Filters exported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { error(e) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rules = ContentFilters.read()
        render()
    }
    override fun onResume() { super.onResume(); rules = ContentFilters.read(); render() }
    private fun openRules(section: String) { startActivity(Intent(this, FilterRulesActivity::class.java).putExtra("section", section)) }
    private fun error(e: Exception) { AlertDialog.Builder(this).setTitle("Could not apply filters").setMessage(e.message).setPositiveButton("OK", null).show() }
    private fun update(next: FilterRules) {
        try { ContentFilters.save(next); rules = ContentFilters.read(); render() } catch (e: Exception) { error(e) }
    }
    private fun text(value: String, size: Float = 16f) { layout.addView(TextView(this).apply { text = value; textSize = size; setPadding(0, 16, 0, 12) }) }
    private fun button(label: String, action: () -> Unit) { layout.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
    private fun toggle(label: String, value: Boolean, enabled: Boolean = true, action: (Boolean) -> FilterRules) {
        layout.addView(CheckBox(this).apply { text = label; isChecked = value; isEnabled = enabled; setOnCheckedChangeListener { _, checked -> update(action(checked)) } })
    }
    private fun explain(label: String, details: String) {
        val body = TextView(this).apply { text = details; textSize = 15f; setPadding(12,12,12,24); visibility = android.view.View.GONE }
        val heading = TextView(this).apply {
            text = getString(R.string.hex_details_show); textSize = 15f
            contentDescription = "Explain $label"
            setPadding(12,8,12,20)
            isClickable = true; isFocusable = true
            setOnClickListener {
                val open = body.visibility != android.view.View.VISIBLE
                body.visibility = if (open) android.view.View.VISIBLE else android.view.View.GONE
                text = getString(if (open) R.string.hex_details_hide else R.string.hex_details_show)
            }
        }
        layout.addView(heading); layout.addView(body)
    }
    private fun edit(label: String, values: List<String>, action: (List<String>) -> FilterRules) {
        val editor = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 6; maxLines = 15; setText(values.joinToString("\n")); setSelection(0)
        }
        val dialog = AlertDialog.Builder(this).setTitle(label).setMessage("One rule per line. Exact: tagname · Contains: ~text · Regex: /pattern/i · Case exact: 'Name'. Commas remain part of a rule.")
            .setView(editor).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val next = action(editor.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct())
                    ContentFilters.save(next); rules = next; dialog.dismiss(); render()
                } catch (e: Exception) { editor.error = e.message }
            }
        }
        dialog.show()
    }
    private fun render() {
        val padding = (20 * resources.displayMetrics.density).toInt()
        layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(padding, padding + 28, padding, padding + 48) }
        setContentView(ScrollView(this).apply { addView(layout); clipToPadding = false })
        text("Filters · all accounts", 24f)
        text("Changes save immediately. Close this screen to refresh listings.")
        toggle("Enable filtering", rules.enabled) { rules.copy(enabled = it) }
        toggle("Always allow followed channels", rules.allowFollowedChannels) { rules.copy(allowFollowedChannels = it) }
        explain("Always allow followed channels", "When ON, channels followed by your active Twitch account, plus its local follows, bypass channel, title, tag and language blocking. Existing follows are included automatically. Following a new channel adds the exception; unfollowing removes it on refreshed listings.\n\nSwitching accounts uses that account's follows. It does not put names into your manual whitelist. Manual exceptions remain until you remove them.\n\nChanges made on another device are picked up during browsing, normally within a minute. If Twitch cannot be reached, the last successful list is used until it can refresh. Category cards are separate: a blocked game/category card can still be hidden even when you follow someone playing it.")
        text("Language", 20f)
        toggle("English only (tags and title)", rules.englishOnly) { rules.copy(englishOnly = it) }
        explain("English only", "When ON, a known non-English language tag such as Spanish, Français or Deutsch can hide a stream. It also enables the title-language and declared-language checks below when those switches are ON. Every supplied tag is checked, including tags not shown on the card.\n\nExample: an English-looking title with a Spanish language tag is blocked. A stream tagged English can still be blocked by a foreign-language title or another rule; English is not an automatic whitelist.\n\nA missing language tag does not block by itself. This cannot prove what language someone is speaking. Turning this OFF disables these language-evidence checks, but the separate character switch and your custom tag/title/channel rules still apply.")
        toggle("Block foreign letters and accents", rules.blockNonEnglishCharacters) { rules.copy(blockNonEnglishCharacters = it) }
        explain("Non-English letters and accents", "When ON, even one foreign-script letter or accented letter in a channel login/display name, stream title or supplied tag can hide the item. This check works independently of English only.\n\nBlocked examples: 日本語, Русский, 한국어, Arabic letters, café and naïve. That is deliberately aggressive: an English streamer quoting a Japanese name or using an accented word may also be hidden.\n\nAllowed by this check: plain English letters, numbers, emoji, punctuation and styled English letters such as ＤＢＤ or 𝒜. Styled letters are converted to their ordinary equivalent first; accents are not removed. Emoji blocking is managed separately under Emojis; an imported emoji-only rule is shown there.\n\nTo allow another familiar accented word, open Allow lists → Allowed words and add it, such as café. Only that complete word is protected; other text remains checked.\n\nUse Allowed tags, Allowed titles or Allowed usernames to exempt a whole matching field, or Allowed channels to exempt a creator completely. Those lists support Exact, Contains and Regex. A field exception does not override matches in other fields.\n\nGame/category names are not checked for foreign characters here. Category rules have their own list.")
        toggle("Detect the title's language", rules.detectTitleLanguage, enabled = rules.englishOnly) { rules.copy(detectTitleLanguage = it) }
        explain("Detect title language", "Works when English only is ON. The bundled on-device language model reads the stream title to catch languages that use ordinary Latin letters, such as Spanish, French or German. It does not listen to audio, inspect chat or upload the title for analysis.\n\nA title needs at least 20 letters, and the model must report at least 90% confidence. Example: a sufficiently long Spanish sentence can be blocked even when Twitch says English. An ordinary English sentence stays visible unless another rule matches.\n\nShort examples such as hola, a game name or an emote code are not enough for this check. Mixed-language titles, names and slang can be uncertain or misclassified. Missing titles, uncertain results and model errors do not block by themselves. The character switch may still block an accent or foreign letter in a short title.")
        toggle("Trust Twitch's language setting", rules.useDeclaredLanguageAlone, enabled = rules.englishOnly) { rules.copy(useDeclaredLanguageAlone = it) }
        explain("Declared non-English language alone", "Works when English only is ON. This is OFF by default because Twitch's broadcaster language setting can be stale or incorrect.\n\nWhen ON, a supplied language code such as es (Spanish), fr (French), de (German) or ja (Japanese) is enough to block, even if the visible title and tags look English. When OFF, that setting alone does not hide the stream; tags, title detection and custom rules can still block it.\n\nMissing, unknown, other and English language values do not trigger this check. Some API routes do not supply the language field, so this switch cannot enforce it on every listing.\n\nA followed-channel exemption or Allowed channels rule overrides all the language options. Other allow lists protect only their matching field or word. Use Why blocked? to see which checks actually matched.")
        text("Content rules", 20f)
        button("Channels · ${rules.channels.size}") { openRules("channels") }
        button("Titles · ${rules.titles.size}") { openRules("titles") }
        button("Tags · ${rules.tags.size}") { openRules("tags") }
        button("Categories · ${rules.categories.size}") { openRules("categories") }
        text("Emojis", 20f)
        toggle("Block emojis by default", rules.blockEmojis) { rules.copy(blockEmojis = it) }
        explain("Emoji rules", "OFF: emojis are allowed unless you put one in Blocked emojis. ON: emojis are blocked unless you put one in Allowed emojis.\n\nA specific allowed emoji wins over the default setting and custom matches for that emoji. Adding one to Allowed emojis removes it from Blocked emojis; adding one to Blocked emojis removes its specific exception. The lists never keep the same emoji in both places.\n\nEmoji-only rules imported from channel/title/tag lists are moved here automatically. A rule for 🔴 appears in Blocked emojis. Allow 🔴 to keep an English title such as 🔴 DROPS ON visible, while unrelated title rules, tags and language checks still apply. A rule containing other text, such as ~spoiler, can still block.\n\nPaste one complete emoji per entry. Flags, skin tones and joined emojis such as 👨‍👩‍👧‍👦 count as one. 👍 and 👍🏽 are separate choices; ♥ and ♥️ are the same choice. This includes emoji-capable symbols such as ©; ordinary punctuation and plain digits are allowed.\n\nThese checks apply to usernames, display names, titles and tags. A whole-field allow rule protects that field; an Allowed channels or followed-channel exception protects the whole creator.\n\nFull app exports preserve the default and exceptions. Unwanted Twitch exports preserve specific blocked emojis as its ordinary blocking rules, but cannot express block-all or allowed emojis.")
        button("Blocked emojis · ${rules.blockedEmojis.size}") { openRules("blockedEmojis") }
        button("Allowed emojis · ${rules.allowedEmojis.size}") { openRules("allowedEmojis") }
        text("Allow lists", 20f)
        button("Allowed words · ${rules.allowedWords.size}") { openRules("allowedWords") }
        button("Allowed channels · ${rules.allowedChannels.size}") { openRules("allowedChannels") }
        button("Allowed usernames · ${rules.allowedUsernames.size}") { openRules("allowedUsernames") }
        button("Allowed titles · ${rules.allowedTitles.size}") { openRules("allowedTitles") }
        button("Allowed tags · ${rules.allowedTags.size}") { openRules("allowedTags") }
        explain("Allow lists", "Allowed channels exempts a whole creator. Allowed usernames, titles and tags skip checks only for the matching field; other fields can still block. Exact, Contains, Regex and case-sensitive matching are available.\n\nFor a familiar accented word, use Allowed words. The lists start empty. Adding Xbox prevents that complete word from matching a contains-box rule. Adding café permits that word without allowing unrelated accented text. Word exceptions protect only that complete word from custom matches too; the rest of each field remains checked.\n\nFull app exports save all allow lists. Export for Unwanted Twitch only saves blocking lists because the extension format has no matching allow-list feature.")
        button("Why blocked? · current session") {
            startActivity(Intent(this, BlockedReasonsActivity::class.java))
        }
        button("Import Unwanted Twitch / app JSON") { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        button("Export for Unwanted Twitch") { exportCompatible = true; exportFile.launch("UnwantedTwitch_Blacklist.json") }
        button("Export full app settings") { exportCompatible = false; exportFile.launch("Xtra-Filters.json") }
        button("Done") { finish() }
    }
}
