package com.github.andreyasadchy.xtra.model.helix.user

import kotlinx.serialization.Serializable

@Serializable
class UsersResponse(
    val data: List<User>,
)