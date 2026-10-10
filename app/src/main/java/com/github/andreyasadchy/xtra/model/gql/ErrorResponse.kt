package com.github.andreyasadchy.xtra.model.gql

import kotlinx.serialization.Serializable

@Serializable
class ErrorResponse(
    val errors: List<Error>? = null,
)