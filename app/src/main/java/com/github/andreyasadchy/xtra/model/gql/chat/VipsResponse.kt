package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class VipsResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val user: User,
    )

    @Serializable
    class User(
        val vips: Vips,
    )

    @Serializable
    class Vips(
        val edges: List<Item>,
    )

    @Serializable
    class Item(
        val node: Vip,
    )

    @Serializable
    class Vip(
        val id: String? = null,
        val login: String? = null,
    )
}