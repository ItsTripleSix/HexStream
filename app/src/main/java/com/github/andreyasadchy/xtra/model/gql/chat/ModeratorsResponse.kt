package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ModeratorsResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val user: User,
    )

    @Serializable
    class User(
        val mods: Mods,
    )

    @Serializable
    class Mods(
        val edges: List<Item>,
    )

    @Serializable
    class Item(
        val node: Node,
    )

    @Serializable
    class Node(
        val id: String? = null,
        val login: String? = null,
    )
}