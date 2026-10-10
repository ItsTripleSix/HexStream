package com.github.andreyasadchy.xtra.model.gql.stream

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ViewerCountResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val user: User,
    )

    @Serializable
    class User(
        val stream: Stream? = null,
    )

    @Serializable
    class Stream(
        val id: String? = null,
        val viewersCount: Int? = null,
    )
}