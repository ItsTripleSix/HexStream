package com.github.andreyasadchy.xtra.model.helix.video

import com.github.andreyasadchy.xtra.model.helix.Pagination
import kotlinx.serialization.Serializable

@Serializable
class VideosResponse(
    val data: List<Video>,
    val pagination: Pagination? = null,
)