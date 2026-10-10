package com.github.andreyasadchy.xtra.model.helix.chat

import kotlinx.serialization.Serializable

@Serializable
class EmoteSetsResponse(
    val template: String,
    val data: List<EmoteTemplate>,
)