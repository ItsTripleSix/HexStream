package com.github.andreyasadchy.xtra.model.misc

import kotlinx.serialization.Serializable

@Serializable
class RecentMessagesResponse(
    val messages: List<String>,
)