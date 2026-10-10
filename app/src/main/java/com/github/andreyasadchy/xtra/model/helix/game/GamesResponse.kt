package com.github.andreyasadchy.xtra.model.helix.game

import com.github.andreyasadchy.xtra.model.helix.Pagination
import kotlinx.serialization.Serializable

@Serializable
class GamesResponse(
    val data: List<Game>,
    val pagination: Pagination? = null,
)