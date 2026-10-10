package com.github.andreyasadchy.xtra.model.helix.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EmoteTemplate(
    val id: String? = null,
    val name: String? = null,
    val format: List<String>? = null,
    @SerialName("theme_mode")
    val theme: List<String>? = null,
    val scale: List<String>? = null,
    @SerialName("emote_set_id")
    val setId: String? = null,
    @SerialName("owner_id")
    val ownerId: String? = null,
    @SerialName("emote_type")
    val type: String? = null,
)