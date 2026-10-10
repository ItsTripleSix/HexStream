package com.github.andreyasadchy.xtra.model.chat

class CheerEmote(
    val name: String,
    val localData: Pair<Long, Int>? = null,
    val url1x: String? = null,
    val url2x: String? = null,
    val url3x: String? = null,
    val url4x: String? = null,
    val format: String? = null,
    val isAnimated: Boolean = true,
    val minBits: Int,
    val color: String? = null,
)