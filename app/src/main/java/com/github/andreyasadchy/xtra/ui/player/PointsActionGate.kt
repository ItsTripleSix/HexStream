package com.github.andreyasadchy.xtra.ui.player

/** A dialog may confirm or submit one action at a time, for the account that opened it. */
internal class PointsActionGate(private val accountId: String) {
    private enum class State { IDLE, CONFIRMING, SUBMITTING }
    private var state = State.IDLE
    val isBusy get() = state != State.IDLE

    fun confirm(currentAccount: String?): Boolean {
        if (state != State.IDLE || !sameAccount(currentAccount)) return false
        state = State.CONFIRMING
        return true
    }

    fun submit(currentAccount: String?): Boolean {
        if (state != State.CONFIRMING || !sameAccount(currentAccount)) return false
        state = State.SUBMITTING
        return true
    }

    fun claimBonus(currentAccount: String?): Boolean {
        if (state != State.IDLE || !sameAccount(currentAccount)) return false
        state = State.SUBMITTING
        return true
    }

    fun cancelConfirmation() { if (state == State.CONFIRMING) state = State.IDLE }
    fun finish() { state = State.IDLE }
    private fun sameAccount(current: String?) = accountId.isNotBlank() && current == accountId
}
