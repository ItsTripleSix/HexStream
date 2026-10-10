package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import com.github.andreyasadchy.xtra.model.gql.PageInfo
import kotlinx.serialization.Serializable

@Serializable
class UserEmotesResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val channel: Channel,
    )

    @Serializable
    class Channel(
        val self: Self,
    )

    @Serializable
    class Self(
        val availableEmoteSetsPaginated: Sets,
    )

    @Serializable
    class Sets(
        val edges: List<Item>,
        val pageInfo: PageInfo? = null,
    )

    @Serializable
    class Item(
        val node: Set,
        val cursor: String? = null,
    )

    @Serializable
    class Set(
        val emotes: List<Emote>,
        val owner: User? = null,
    )

    @Serializable
    class Emote(
        val token: String? = null,
        val id: String? = null,
        val setID: String? = null,
        val type: String? = null,
    )

    @Serializable
    class User(
        val id: String? = null,
    )
}