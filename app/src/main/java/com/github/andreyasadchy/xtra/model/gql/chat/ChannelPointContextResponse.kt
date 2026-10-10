package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ChannelPointContextResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val community: Community,
    )

    @Serializable
    class Community(
        val channel: Channel,
    )

    @Serializable
    class Channel(
        val self: Self,
    )

    @Serializable
    class Self(
        val communityPoints: Points,
    )

    @Serializable
    class Points(
        val balance: Int? = null,
        val availableClaim: Claim? = null,
    )

    @Serializable
    class Claim(
        val id: String? = null,
    )
}