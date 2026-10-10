package com.github.andreyasadchy.xtra.model.helix.clip

import com.github.andreyasadchy.xtra.model.helix.Pagination
import kotlinx.serialization.Serializable

@Serializable
class ClipsResponse(
    val data: List<Clip>,
    val pagination: Pagination? = null,
)