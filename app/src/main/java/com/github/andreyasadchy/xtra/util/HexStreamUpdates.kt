package com.github.andreyasadchy.xtra.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Keep upstream update defaults from replacing this fork; preserve deliberate custom feeds. */
object HexStreamUpdates {
    const val DEFAULT_URL = "https://api.github.com/repos/ItsTripleSix/HexStream/releases?per_page=1"
    private const val LEGACY_URL = "https://api.github.com/repos/crackededed/xtra/releases/tags/latest"

    fun url(saved: String?): String = when {
        saved.isNullOrBlank() || saved.trim().trimEnd('/').equals(LEGACY_URL, ignoreCase = true) -> DEFAULT_URL
        else -> saved
    }

    /** GitHub's release list includes betas; custom single-release feeds remain compatible. */
    fun parse(raw: String): JsonObject = when (val value = Json.parseToJsonElement(raw)) {
        is JsonObject -> value
        is JsonArray -> value.filterIsInstance<JsonObject>()
            .firstOrNull { it["draft"]?.jsonPrimitive?.booleanOrNull != true } ?: JsonObject(emptyMap())
        else -> error("Invalid release response")
    }
}
