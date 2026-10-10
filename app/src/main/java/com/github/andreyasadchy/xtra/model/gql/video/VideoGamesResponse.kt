package com.github.andreyasadchy.xtra.model.gql.video

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class VideoGamesResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val video: Video,
    )

    @Serializable
    class Video(
        val moments: Moments,
    )

    @Serializable
    class Moments(
        val edges: List<Item>,
    )

    @Serializable
    class Item(
        val node: Moment,
    )

    @Serializable
    class Moment(
        val details: Details? = null,
        val positionMilliseconds: Int? = null,
        val durationMilliseconds: Int? = null,
    )

    @Serializable
    class Details(
        val game: Game? = null,
    )

    @Serializable
    class Game(
        val id: String? = null,
        val displayName: String? = null,
        val boxArtURL: String? = null,
    )
}