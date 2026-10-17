package com.github.andreyasadchy.xtra.ui.settings

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.forEach
import com.github.andreyasadchy.xtra.R

/** Keep the explanation visible when a preference's selected value changes. */
internal object SettingsHelp {
    // Only values already intended for display belong here. Account details,
    // credentials, request headers and arbitrary multi-line input stay private.
    private val visibleTextValues = setOf(
        "helix_client_id", "helix_redirect", "gql_client_id2",
        "player_buffer_min", "player_buffer_max", "player_buffer_playback", "player_buffer_rebuffer",
        "player_live_min_speed", "player_live_max_speed", "player_live_target_offset",
        "chat_text_size", "chat_emote_size", "chat_badge_size", "chat_gif_size",
        "download_stream_live_check", "download_stream_offline_check",
        "download_stream_start_wait", "download_stream_end_wait",
        "proxy_timeout", "token_supported_codecs", "playerRewindV2", "playerForwardV2",
        "update_url", "update_check_frequency",
    )

    fun attach(group: PreferenceGroup) {
        group.forEach { preference ->
            if (preference is PreferenceGroup) {
                attach(preference)
            } else {
                val help = preference.summary?.toString().orEmpty()
                when (preference) {
                    is ListPreference -> preference.summaryProvider = Preference.SummaryProvider<ListPreference> {
                        withValue(it, it.entry, help)
                    }
                    is EditTextPreference -> if (preference.key in visibleTextValues) {
                        preference.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                            withValue(it, it.text, help)
                        }
                    }
                }
            }
        }
    }

    private fun withValue(preference: Preference, value: CharSequence?, help: String): String =
        preference.context.getString(
            R.string.hex_setting_value_help,
            value?.takeIf { it.isNotBlank() } ?: preference.context.getString(R.string.hex_setting_not_set),
            help,
        )
}
