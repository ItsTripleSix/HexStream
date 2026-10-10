package com.github.andreyasadchy.xtra.model.gql.search

import com.github.andreyasadchy.xtra.model.gql.Error
import kotlinx.serialization.Serializable

@Serializable
class SearchStreamTagsResponse(
    val errors: List<Error>? = null,
    val data: Data? = null,
) {
    @Serializable
    class Data(
        val searchFreeformTags: Tags,
    )

    @Serializable
    class Tags(
        val edges: List<Item>,
    )

    @Serializable
    class Item(
        val node: Tag,
    )

    @Serializable
    class Tag(
        val tagName: String? = null,
    )
}