package com.nunchuk.android.main.membership.signer

import org.junit.Assert.assertEquals
import org.junit.Test

class ClaimResultOutcomeTest {

    @Test
    fun `cancelling a device opened without the picker returns to the claim`() {
        // Ledger / BitBox / Trezor opened for a claim account (CLM-16): back must not leave an empty intro.
        assertEquals(
            ClaimResultOutcome.RETURN_TO_CALLER,
            claimResultOutcome(hasSigner = false, isDesktopHandoff = false, skipPicker = true),
        )
    }

    @Test
    fun `cancelling a device picked from the list stays on the picker`() {
        assertEquals(
            ClaimResultOutcome.STAY_ON_PICKER,
            claimResultOutcome(hasSigner = false, isDesktopHandoff = false, skipPicker = false),
        )
    }

    @Test
    fun `a signer or a desktop hand-off wins either way`() {
        for (skipPicker in listOf(true, false)) {
            assertEquals(ClaimResultOutcome.SIGNER, claimResultOutcome(true, false, skipPicker))
            assertEquals(ClaimResultOutcome.DESKTOP_HANDOFF, claimResultOutcome(false, true, skipPicker))
        }
    }
}
