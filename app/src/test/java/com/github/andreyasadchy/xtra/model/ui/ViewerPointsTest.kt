package com.github.andreyasadchy.xtra.model.ui

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ViewerPointsTest {
    private fun context(reward: String = "", self: String = "null") = Json.parseToJsonElement("""{"data":{"community":{"channel":{"self":$self,"communityPointsSettings":{"customRewards":[$reward]}}}}}""")
    @Test fun readsBalanceBonusAndRequiredInput() {
        val p = ViewerPoints.parse(context("""{"id":"r","title":"Hello","cost":50,"prompt":"Message","isUserInputRequired":true,"isEnabled":true,"isInStock":true}""", """{"communityPoints":{"balance":100,"availableClaim":{"id":"bonus"}}}"""))
        assertEquals(100, p.balance); assertEquals("bonus", p.claimId)
        assertTrue(p.rewards.single().inputRequired); assertTrue(p.rewards.single().available)
    }
    @Test fun pausedRewardCannotBeSelected() {
        assertFalse(ViewerPoints.parse(context("""{"id":"r","title":"Paused","cost":50,"isEnabled":true,"isPaused":true}""")).rewards.single().available)
    }
    @Test fun outOfStockRewardCannotBeSelected() {
        assertFalse(ViewerPoints.parse(context("""{"id":"r","title":"Gone","cost":50,"isEnabled":true,"isInStock":false}""")).rewards.single().available)
    }
    @Test fun missingViewerDoesNotInventBalanceOrBonus() {
        val p = ViewerPoints.parse(context()); assertNull(p.balance); assertNull(p.claimId); assertTrue(p.rewards.isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun graphqlFailureIsNotAnEmptySuccess() {
        ViewerPoints.parse(Json.parseToJsonElement("""{"errors":[{"message":"Denied"}]}"""))
    }
    @Test fun onlyConfirmedRedemptionIsSuccess() {
        assertTrue(ViewerPoints.redemptionConfirmed(Json.parseToJsonElement("""{"data":{"redeemCommunityPointsCustomReward":{"error":null,"redemption":{"id":"done"}}}}""")))
        assertFalse(ViewerPoints.redemptionConfirmed(Json.parseToJsonElement("""{"data":{"redeemCommunityPointsCustomReward":{"error":{"code":"DENIED"},"redemption":null}}}""")))
        assertFalse(ViewerPoints.redemptionConfirmed(Json.parseToJsonElement("""{"errors":[{"message":"Unauthenticated"}]}""")))
        assertFalse(ViewerPoints.redemptionConfirmed(Json.parseToJsonElement("""{"data":null}""")))
    }
}
