package com.github.andreyasadchy.xtra.model.ui

import kotlinx.serialization.json.*

data class ViewerReward(val id: String, val title: String, val cost: Int, val prompt: String?,
                        val inputRequired: Boolean, val available: Boolean)
data class ViewerPoints(val balance: Int?, val claimId: String?, val rewards: List<ViewerReward>) {
    companion object {
        fun parse(root: JsonElement): ViewerPoints {
            val response = root.jsonObject
            require(response["errors"]?.jsonArray.isNullOrEmpty()) { "Twitch could not load channel points" }
            val channel = response["data"]?.jsonObject?.get("community")?.jsonObject?.get("channel") as? JsonObject
                ?: error("Channel points are unavailable")
            val points = (channel["self"] as? JsonObject)?.get("communityPoints") as? JsonObject
            val settings = channel["communityPointsSettings"] as? JsonObject
            val rewards = (settings?.get("customRewards") as? JsonArray).orEmpty().mapNotNull {
                val r = it as? JsonObject ?: return@mapNotNull null
                val id = r["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = r["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val cost = r["cost"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                ViewerReward(id, title, cost, r["prompt"]?.jsonPrimitive?.contentOrNull,
                    r["isUserInputRequired"]?.jsonPrimitive?.booleanOrNull == true,
                    r["isEnabled"]?.jsonPrimitive?.booleanOrNull == true &&
                        r["isPaused"]?.jsonPrimitive?.booleanOrNull != true && r["isInStock"]?.jsonPrimitive?.booleanOrNull != false &&
                        r["cooldownExpiresAt"]?.jsonPrimitive?.contentOrNull == null)
            }
            return ViewerPoints(points?.get("balance")?.jsonPrimitive?.intOrNull,
                (points?.get("availableClaim") as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull, rewards)
        }
        fun redemptionConfirmed(root: JsonElement): Boolean {
            val r = root.jsonObject
            if (!r["errors"]?.jsonArray.isNullOrEmpty()) return false
            val result = (r["data"] as? JsonObject)?.get("redeemCommunityPointsCustomReward") as? JsonObject ?: return false
            if (result["error"] != null && result["error"] != JsonNull) return false
            return ((result["redemption"] as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull)?.isNotBlank() == true
        }
    }
}
