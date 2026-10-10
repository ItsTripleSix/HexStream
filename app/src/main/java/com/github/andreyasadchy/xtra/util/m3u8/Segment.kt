package com.github.andreyasadchy.xtra.util.m3u8

data class Segment(
    val uri: String,
    val duration: Float,
    val title: String?,
    val programDateTime: String?
)