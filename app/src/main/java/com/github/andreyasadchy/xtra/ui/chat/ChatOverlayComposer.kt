package com.github.andreyasadchy.xtra.ui.chat

import androidx.core.view.isVisible
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.databinding.FragmentChatBinding

/** Uses the existing chat controls; account authorization and send behavior stay in ChatFragment. */
internal object ChatOverlayComposer {
    fun show(binding: FragmentChatBinding, canSend: Boolean, showInput: Boolean = true) {
        binding.messageView.isVisible = showInput
        binding.editText.isEnabled = canSend
        binding.editText.setHint(if (canSend) R.string.hex_chat_message_hint else R.string.hex_chat_sign_in_hint)
        binding.emotes.isVisible = canSend
    }
}
