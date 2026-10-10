package com.github.andreyasadchy.xtra.model.helix.stream

import com.github.andreyasadchy.xtra.model.helix.Pagination
import kotlinx.serialization.Serializable

@Serializable
class StreamsResponse(
    val data: List<Stream>,
    val pagination: Pagination? = null,
)