package com.github.andreyasadchy.xtra.model.gql.chat

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class EmoteCardResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val emote: Emote,
    )

    @Serializable
    class Emote(
        val type: String? = null,
        val subscriptionTier: String? = null,
        val bitsBadgeTierSummary: Tier? = null,
        val owner: User? = null,
    )

    @Serializable
    class Tier(
        val threshold: Int? = null,
    )

    @Serializable
    class User(
        val login: String? = null,
        val displayName: String? = null,
    )
}