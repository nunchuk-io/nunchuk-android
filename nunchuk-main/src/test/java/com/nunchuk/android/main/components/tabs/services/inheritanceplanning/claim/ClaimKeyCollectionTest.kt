package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimKeyCollectionTest {

    private fun path(account: Int) = "m/48'/0'/$account'/2'"

    private fun signer(xfp: String, account: Int = 0, type: SignerType = SignerType.AIRGAP) = SignerModel(
        id = xfp,
        name = xfp,
        derivationPath = path(account),
        fingerPrint = xfp,
        type = type,
        tags = if (type == SignerType.AIRGAP) listOf(SignerTag.JADE) else emptyList(),
        isMasterSigner = type == SignerType.SOFTWARE,
    )

    private fun origin(xfp: String, account: Int) = KeyOrigin(xfp = xfp, derivationPath = path(account))

    /** A and B added at account 0; download-wallet asked for A/account 2 and B/account 3. */
    private val twoPending = ClaimData(
        signers = linkedSetOf(signer("aaaa"), signer("bbbb")),
        requiredKeyCounts = listOf(2),
        requiredKeyCount = 2,
        pendingKeyOrigins = listOf(origin("aaaa", 2), origin("bbbb", 3)),
    )

    @Test
    fun `a new device starts at account 0, not another device's requested account`() {
        assertEquals(-1, twoPending.nextKeyAccountIndex)
    }

    @Test
    fun `a new device is added as a new key, not as a pending account`() {
        val step = twoPending.addOnChainSigner(signer("cccc"))
        assertTrue(step is OnChainKeyStep.Download)
        val data = (step as OnChainKeyStep.Download).data
        assertEquals(twoPending.pendingKeyOrigins, data.pendingKeyOrigins)
    }

    @Test
    fun `each requested account is submitted at once, even with others still pending`() {
        val step = twoPending.addOnChainSigner(signer("aaaa", 2))
        assertTrue(step is OnChainKeyStep.Download)
        assertEquals(listOf(origin("bbbb", 3)), (step as OnChainKeyStep.Download).data.pendingKeyOrigins)
    }

    @Test
    fun `a pending device with the wrong account is asked again`() {
        assertEquals(OnChainKeyStep.WrongAccount(origin("aaaa", 2)), twoPending.addOnChainSigner(signer("aaaa", 1)))
    }

    @Test
    fun `collection continues until the chosen count`() {
        val data = ClaimData(requiredKeyCounts = listOf(1, 2), requiredKeyCount = 2)
        assertTrue(data.addOnChainSigner(signer("aaaa")) is OnChainKeyStep.AddMore)
    }

    @Test
    fun `software accounts are derived first, devices are asked for`() {
        val data = ClaimData(
            signers = linkedSetOf(signer("aaaa"), signer("ssss", type = SignerType.SOFTWARE)),
            pendingKeyOrigins = listOf(origin("aaaa", 2), origin("ssss", 2)),
        )
        assertEquals(AccountImportStep.Derive(origin("ssss", 2)), data.nextAccountImport(emptySet()))
        val ask = data.nextAccountImport(setOf(origin("ssss", 2)))
        assertEquals(AccountImportStep.AskDevice(origin("aaaa", 2), signer("aaaa"), 2), ask)
    }

    @Test
    fun `a failed derivation stays retryable, a rejected one is not derived again`() {
        val data = ClaimData(
            signers = linkedSetOf(signer("ssss", type = SignerType.SOFTWARE)),
            pendingKeyOrigins = listOf(origin("ssss", 2)),
        )
        // Not recorded as submitted (derivation failed): derive again.
        assertEquals(AccountImportStep.Derive(origin("ssss", 2)), data.nextAccountImport(emptySet()))
        // Submitted and asked for again: rejected, so another key is needed.
        assertEquals(AccountImportStep.AddAnotherKey, data.nextAccountImport(setOf(origin("ssss", 2))))
    }

    @Test
    fun `cancelling a requested account returns to adding keys with accepted keys kept`() {
        val onChain = twoPending.copy(walletType = WalletType.MINISCRIPT)
        assertTrue(onChain.returnsToKeyCollectionOnCancel)
        // Cancelling changes no claim data: the next-key screen counts the keys already accepted.
        assertEquals(listOf("aaaa", "bbbb"), onChain.signers.map { it.fingerPrint })
        assertEquals(2, onChain.addedKeyCount)
        // A cancelled new-key pick with nothing requested stays on the screen that launched it.
        assertFalse(onChain.copy(pendingKeyOrigins = emptyList()).returnsToKeyCollectionOnCancel)
    }

    @Test
    fun `add account opens the identified device at the requested account`() {
        val device = signer("aaaa")
        val request = AddAccountPrompt(account = 2, device = device)
            .signerIntroRequest(ClaimData(magic = "magic", walletType = WalletType.MINISCRIPT))
        assertEquals(
            SignerIntroFlow.ClaimInheritance(magic = "magic", isOnChain = true, keyIndex = 2, accountDevice = device),
            request.flow,
        )
        assertEquals(WalletType.MINISCRIPT, request.walletType)
    }
}
