package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ChannelCheerEmotesResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val channel: Channel,
    )

    @Serializable
    class Channel(
        val cheer: CheerConfig,
    )

    @Serializable
    class CheerConfig(
        val cheerGroups: List<Item>,
    )

    @Serializable
    class Item(
        val nodes: List<Group>,
        val templateURL: String,
    )

    @Serializable
    class Group(
        val tiers: List<Tier>,
        val prefix: String,
    )

    @Serializable
    class Tier(
        val bits: Int,
    )
}