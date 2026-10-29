package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.inheritance.ClaimValidKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimDataTest {

    private fun path(account: Int) = "m/48'/0'/$account'/2'"

    private fun signer(xfp: String, account: Int = 0, xpub: String = "xpub-$xfp-$account") = SignerModel(
        id = xfp,
        name = xfp,
        derivationPath = path(account),
        fingerPrint = xfp,
        isMasterSigner = false,
        xpub = xpub,
    )

    private fun validKey(xfp: String, account: Int = 0, xpub: String = "xpub-$xfp-$account") =
        ClaimValidKey(xfp = xfp, derivationPath = path(account), xpub = xpub)

    @Test
    fun `accounts of one device count as one independent key`() {
        val data = ClaimData(
            signers = setOf(signer("aaaa", 0), signer("aaaa", 2), signer("bbbb", 0)),
            requiredKeyCounts = listOf(1, 2),
        )
        assertEquals(2, data.addedKeyCount)
    }

    @Test
    fun `without required key counts every signer counts as before`() {
        val data = ClaimData(signers = setOf(signer("aaaa", 0), signer("aaaa", 1)))
        assertFalse(data.isIndependentKeyCount)
        assertEquals(2, data.addedKeyCount)
    }

    @Test
    fun `chooser only when conditions need different key counts`() {
        assertTrue(ClaimData(requiredKeyCounts = listOf(1, 2)).needsKeyCountChoice)
        assertFalse(ClaimData(requiredKeyCounts = listOf(2)).needsKeyCountChoice)
        assertFalse(ClaimData().needsKeyCountChoice)
    }

    @Test
    fun `download result keeps accepted keys and drops a rejected one`() {
        val data = ClaimData(
            signers = setOf(signer("aaaa"), signer("bbbb"), signer("cccc")),
            requiredKeyCounts = listOf(2),
        ).withDownloadResult(
            validKeys = listOf(validKey("aaaa"), validKey("bbbb")),
            origins = emptyList(),
        )
        assertEquals(listOf("aaaa", "bbbb"), data.signers.map { it.fingerPrint })
        assertTrue(data.pendingKeyOrigins.isEmpty())
    }

    @Test
    fun `download result compares the full tuple, not the xfp alone`() {
        val data = ClaimData(signers = setOf(signer("aaaa", 0, xpub = "wrong"))).withDownloadResult(
            validKeys = listOf(validKey("AAAA", 0, xpub = "right")),
            origins = emptyList(),
        )
        assertTrue(data.signers.isEmpty())
    }

    @Test
    fun `requested account is pending and matched by xfp and path`() {
        val origin = KeyOrigin(xfp = "aaaa", derivationPath = path(2))
        val data = ClaimData(signers = setOf(signer("aaaa"), signer("bbbb")), requiredKeyCounts = listOf(2))
            .withDownloadResult(validKeys = listOf(validKey("aaaa"), validKey("bbbb")), origins = listOf(origin))

        assertEquals(listOf(origin), data.pendingKeyOrigins)
        // The requested account belongs to device A only; a new device still starts at account 0.
        assertEquals(-1, data.nextKeyAccountIndex)
        assertEquals("aaaa", data.deviceOf(origin)?.fingerPrint)
        assertEquals(origin, data.pendingOriginOf(signer("aaaa", 2)))
        // Same device, wrong account: recognized, but not the requested origin.
        assertTrue(data.isPendingDevice(signer("aaaa", 1)))
        assertNull(data.pendingOriginOf(signer("aaaa", 1)))
        assertFalse(data.isPendingDevice(signer("cccc", 2)))
    }

    @Test
    fun `an origin already added is not pending again`() {
        val origin = KeyOrigin(xfp = "aaaa", derivationPath = path(2))
        val data = ClaimData(signers = setOf(signer("aaaa"), signer("aaaa", 2)), requiredKeyCounts = listOf(1))
            .withDownloadResult(validKeys = listOf(validKey("aaaa"), validKey("aaaa", 2)), origins = listOf(origin))
        assertTrue(data.pendingKeyOrigins.isEmpty())
    }

    @Test
    fun `claim transaction uses one account per device, the one asked for last`() {
        val data = ClaimData(
            signers = linkedSetOf(signer("aaaa", 0), signer("bbbb", 0), signer("aaaa", 2)),
            requiredKeyCounts = listOf(2),
        )
        assertEquals(listOf("bbbb" to path(0), "aaaa" to path(2)), data.requiredSigners.map { it.fingerPrint to it.derivationPath })
        assertEquals(listOf(path(0), path(2)), data.derivationPaths)
    }

    @Test
    fun `legacy claim passes every signer to the transaction`() {
        val data = ClaimData(signers = linkedSetOf(signer("aaaa", 0), signer("aaaa", 1)))
        assertEquals(2, data.requiredSigners.size)
    }
}
