package com.github.andreyasadchy.xtra.ui.settings

import android.os.Bundle
import android.annotation.SuppressLint
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.prefs

abstract class MaterialPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsHelp.attach(preferenceScreen)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> =
        object : PreferenceGroupAdapter(preferenceScreen) {
            override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                // Long help must remain readable with large accessibility text.
                (holder.findViewById(android.R.id.summary) as? TextView)?.apply {
                    maxLines = Int.MAX_VALUE
                    ellipsize = null
                }
            }
        }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (requireContext().prefs().getBoolean(C.UI_THEME_MATERIAL3, true)) {
            when (preference) {
                is ListPreference -> showPreferenceDialog(preference, MaterialListPreference())
                is MultiSelectListPreference -> showPreferenceDialog(preference, MaterialMultiSelectListPreference())
                is EditTextPreference -> showPreferenceDialog(preference, MaterialEditTextPreference())
                else -> super.onDisplayPreferenceDialog(preference)
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    @Suppress("DEPRECATION")
    private fun showPreferenceDialog(preference: Preference, fragment: DialogFragment) {
        fragment.arguments = bundleOf("key" to preference.key)
        fragment.setTargetFragment(this, 0)
        fragment.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
    }
}
