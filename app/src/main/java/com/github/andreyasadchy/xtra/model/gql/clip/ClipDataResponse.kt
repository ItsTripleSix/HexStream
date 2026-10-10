package com.github.andreyasadchy.xtra.model.gql.clip

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class ClipDataResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val clip: Clip,
    )

    @Serializable
    class Clip(
        val broadcaster: User? = null,
        val videoOffsetSeconds: Int? = null,
    )

    @Serializable
    class User(
        val id: String? = null,
        val login: String? = null,
        val displayName: String? = null,
        val profileImageURL: String? = null,
    )
}