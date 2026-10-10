package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class SearchVideosResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val searchFor: Search,
    )

    @Serializable
    class Search(
        val videos: Videos,
    )

    @Serializable
    class Videos(
        val edges: List<Item>,
        val cursor: String? = null,
    )

    @Serializable
    class Item(
        val item: Video,
    )

    @Serializable
    class Video(
        val id: String? = null,
        val owner: User? = null,
        val game: Game? = null,
        val title: String? = null,
        val createdAt: String? = null,
        val previewThumbnailURL: String? = null,
        val viewCount: Int? = null,
        val lengthSeconds: Int? = null,
    )

    @Serializable
    class User(
        val id: String? = null,
        val login: String? = null,
        val displayName: String? = null,
    )

    @Serializable
    class Game(
        val id: String? = null,
        val slug: String? = null,
        val displayName: String? = null,
    )
}