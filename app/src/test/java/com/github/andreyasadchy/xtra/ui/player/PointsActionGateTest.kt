package com.github.andreyasadchy.xtra.ui.player

import org.junit.Assert.*
import org.junit.Test

class PointsActionGateTest {
    @Test fun repeatedTapsCannotOpenTwoConfirmationsOrSubmitTwice() {
        val gate = PointsActionGate("one")
        assertTrue(gate.confirm("one"))
        assertFalse(gate.confirm("one"))
        assertFalse(gate.claimBonus("one"))
        assertTrue(gate.submit("one"))
        assertFalse(gate.submit("one"))
        assertFalse(gate.claimBonus("one"))
        gate.finish()
        assertFalse(gate.isBusy)
        assertTrue(gate.claimBonus("one"))
    }

    @Test fun cancellingConfirmationAllowsAnotherChoiceButCannotCancelAnActiveRequest() {
        val gate = PointsActionGate("one")
        assertTrue(gate.confirm("one"))
        gate.cancelConfirmation()
        assertFalse(gate.isBusy)
        assertTrue(gate.claimBonus("one"))
        gate.cancelConfirmation()
        assertTrue(gate.isBusy)
        assertFalse(gate.confirm("one"))
    }

    @Test fun switchingAccountsOrSigningOutRejectsThePendingAction() {
        val gate = PointsActionGate("one")
        assertFalse(gate.confirm("two"))
        assertFalse(gate.claimBonus(null))
        assertFalse(gate.submit("one"))
        assertTrue(gate.confirm("one"))
        assertFalse(gate.submit("two"))
        assertFalse(gate.submit(null))
        assertFalse(PointsActionGate("").claimBonus(""))
    }
}
