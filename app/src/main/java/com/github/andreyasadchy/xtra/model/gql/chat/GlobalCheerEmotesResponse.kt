package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class GlobalCheerEmotesResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val cheerConfig: CheerConfig,
    )

    @Serializable
    class CheerConfig(
        val displayConfig: DisplayConfig,
        val groups: List<Item>,
    )

    @Serializable
    class DisplayConfig(
        val backgrounds: List<String>? = null,
        val colors: List<Color>,
        val scales: List<String>? = null,
        val types: List<Type>? = null,
    )

    @Serializable
    class Color(
        val bits: Int,
        val color: String? = null,
    )

    @Serializable
    class Type(
        val animation: String? = null,
        val extension: String? = null,
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