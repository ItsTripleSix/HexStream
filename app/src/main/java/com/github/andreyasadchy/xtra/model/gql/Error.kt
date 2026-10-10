package com.github.andreyasadchy.xtra.model.gql

import kotlinx.serialization.Serializable

@Serializable
class Error(
    val message: String? = null,
)