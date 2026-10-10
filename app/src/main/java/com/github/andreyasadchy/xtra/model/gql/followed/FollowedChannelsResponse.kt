package com.github.andreyasadchy.xtra.model.gql.followed

import com.github.andreyasadchy.xtra.model.gql.Error
import com.github.andreyasadchy.xtra.model.gql.PageInfo
import kotlinx.serialization.Serializable

@Serializable
class FollowedChannelsResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val user: FollowedData,
    )

    @Serializable
    class FollowedData(
        val follows: Users,
    )

    @Serializable
    class Users(
        val edges: List<Item>,
        val pageInfo: PageInfo? = null,
    )

    @Serializable
    class Item(
        val node: User,
        val cursor: String? = null,
    )

    @Serializable
    class User(
        val id: String? = null,
        val login: String? = null,
        val displayName: String? = null,
        val profileImageURL: String? = null,
        val self: Self? = null,
    )

    @Serializable
    class Self(
        val follower: Follower? = null,
    )

    @Serializable
    class Follower(
        val followedAt: String? = null,
    )
}