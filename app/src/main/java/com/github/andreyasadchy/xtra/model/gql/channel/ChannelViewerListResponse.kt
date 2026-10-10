package com.github.andreyasadchy.xtra.model.gql.channel

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ChannelViewerListResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val user: User,
    )

    @Serializable
    class User(
        val channel: Channel,
    )

    @Serializable
    class Channel(
        val chatters: Chatters,
    )

    @Serializable
    class Chatters(
        val broadcasters: List<Chatter>? = null,
        val moderators: List<Chatter>? = null,
        val vips: List<Chatter>? = null,
        val viewers: List<Chatter>? = null,
        val count: Int? = null,
    )

    @Serializable
    class Chatter(
        val login: String? = null,
    )
}