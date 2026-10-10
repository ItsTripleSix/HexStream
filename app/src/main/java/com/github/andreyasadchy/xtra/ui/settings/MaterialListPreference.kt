package com.github.andreyasadchy.xtra.ui.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.preference.ListPreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreference : ListPreferenceDialogFragmentCompat() {

    private var mWhichButtonClicked = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText, this)
            .setNegativeButton(preference.negativeButtonText, this)
        val contentView = onCreateDialogView(requireContext())
        if (contentView != null) {
            onBindDialogView(contentView)
            builder.setView(contentView)
        } else {
            builder.setMessage(preference.dialogMessage)
        }
        onPrepareDialogBuilder(builder)
        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
    }
}