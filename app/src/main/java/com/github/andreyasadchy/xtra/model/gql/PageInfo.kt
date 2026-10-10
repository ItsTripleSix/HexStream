package com.github.andreyasadchy.xtra.model.gql

import kotlinx.serialization.Serializable

@Serializable
class PageInfo(
    val hasNextPage: Boolean? = null,
)