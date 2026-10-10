package com.github.andreyasadchy.xtra.model.chat

class TwitchBadge(
    val setId: String,
    val version: String,
    val localData: Pair<Long, Int>? = null,
    val url1x: String? = null,
    val url2x: String? = null,
    val url3x: String? = null,
    val url4x: String? = null,
    val title: String? = null,
)
