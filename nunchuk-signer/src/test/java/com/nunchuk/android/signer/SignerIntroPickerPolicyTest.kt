package com.nunchuk.android.signer

import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.model.SupportedSignerConfig
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import org.junit.Assert.*
import org.junit.Test

class SignerIntroPickerPolicyTest {
    @Test fun `ordinary assisted add keeps default catalog and caller restrictions`() {
        val allowed = listOf(signer(SignerType.AIRGAP, SignerTag.JADE))
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.AddAssistedWalletKey, supportedSigners = allowed,
        ))
        assertFalse(policy.usesWalletConfigs)
        val cards = policy.displayInfos(SignerIntroState(supportedSigners = policy.initialSigners))
        assertFalse(cards.single { it.keyType == KeyType.JADE }.isDisabled)
        assertTrue(cards.single { it.keyType == KeyType.TAPSIGNER }.isDisabled)
        assertTrue(cards.single { it.keyType == KeyType.GENERIC_AIRGAP }.isDisabled)
    }

    @Test fun `wallet configs are scoped by wallet type before filtering inheritance slot`() {
        for (walletType in listOf(WalletType.MULTI_SIG, WalletType.MINISCRIPT)) {
            val policy = SignerIntroPickerPolicy(SignerIntroRequest(
                flow = SignerIntroFlow.OnChainTimelockKey(isInheritanceKey = true), walletType = walletType,
            ))
            val state = policy.applyConfigs(SignerIntroState(), listOf(
                config(walletType, true, SignerType.AIRGAP, SignerTag.JADE),
                config(walletType, false, SignerType.NFC),
                config(WalletType.LIQUID, true, SignerType.SOFTWARE),
            ))
            assertEquals(listOf(KeyType.JADE, KeyType.GENERIC_AIRGAP), policy.displayInfos(state).map { it.keyType })
            assertTrue(state.supportedSigners.none { it.type == SignerType.SOFTWARE })
        }
    }

    @Test fun `on-chain normal key uses non-inheritance configs`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.OnChainTimelockKey(), walletType = WalletType.MINISCRIPT,
        ))
        val state = policy.applyConfigs(SignerIntroState(), listOf(
            config(WalletType.MINISCRIPT, true, SignerType.AIRGAP, SignerTag.JADE),
            config(WalletType.MINISCRIPT, false, SignerType.NFC),
        ))
        assertEquals(listOf(KeyType.TAPSIGNER, KeyType.GENERIC_AIRGAP), policy.displayInfos(state).map { it.keyType })
    }

    private val oldSheetKeyTypes = listOf(
        KeyType.TAPSIGNER, KeyType.TREZOR, KeyType.JADE, KeyType.PORTAL, KeyType.SEEDSIGNER,
        KeyType.KEYSTONE, KeyType.FOUNDATION, KeyType.COLDCARD, KeyType.BITBOX, KeyType.LEDGER,
        KeyType.KRUX, KeyType.GENERIC_AIRGAP,
    )

    private fun offChainAssistedCards(flow: SignerIntroFlow.OffChainAssistedKey): List<SignerDisplayInfo> {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(flow = flow))
        assertFalse(policy.usesWalletConfigs)
        assertTrue(policy.loadsExistingSigners)
        return policy.displayInfos(SignerIntroState(supportedSigners = policy.initialSigners))
    }

    @Test fun `assisted off-chain regular key keeps every key type of the old option sheet`() {
        val cards = offChainAssistedCards(SignerIntroFlow.OffChainAssistedKey())
        assertEquals(oldSheetKeyTypes, cards.map { it.keyType })
        assertTrue(cards.none { it.isDisabled })
    }

    @Test fun `assisted off-chain regular key offers software only when allowed`() {
        val cards = offChainAssistedCards(SignerIntroFlow.OffChainAssistedKey(allowSoftwareKey = true))
        assertEquals((oldSheetKeyTypes + KeyType.SOFTWARE).toSet(), cards.map { it.keyType }.toSet())
        assertTrue(cards.none { it.isDisabled })
    }

    @Test fun `limited keyholder sees only the keys they can add`() {
        assertEquals(
            listOf(KeyType.TAPSIGNER, KeyType.TREZOR, KeyType.LEDGER),
            offChainAssistedCards(SignerIntroFlow.OffChainAssistedKey(isKeyHolderLimited = true)).map { it.keyType },
        )
        assertEquals(
            setOf(KeyType.TAPSIGNER, KeyType.TREZOR, KeyType.LEDGER, KeyType.SOFTWARE),
            offChainAssistedCards(SignerIntroFlow.OffChainAssistedKey(isKeyHolderLimited = true, allowSoftwareKey = true))
                .map { it.keyType }.toSet(),
        )
    }

    @Test fun `off-chain owner uses backend inheritance list in picker order`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.OffChainInheritanceKey(), walletType = WalletType.MULTI_SIG,
        ))
        assertTrue(policy.usesWalletConfigs)
        val state = policy.applyConfigs(SignerIntroState(supportedSigners = policy.initialSigners), listOf(
            config(WalletType.MULTI_SIG, true, SignerType.AIRGAP, SignerTag.KRUX),
            config(WalletType.MULTI_SIG, false, SignerType.SOFTWARE),
            config(WalletType.MULTI_SIG, true, SignerType.NFC),
        ))
        val cards = policy.displayInfos(state)
        assertEquals(listOf(KeyType.TAPSIGNER, KeyType.KRUX, KeyType.GENERIC_AIRGAP), cards.map { it.keyType })
        assertTrue(cards.last().isDisabled)
    }

    @Test fun `off-chain owner keeps fallback until matching server configs arrive`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(flow = SignerIntroFlow.OffChainInheritanceKey()))
        val initial = SignerIntroState(supportedSigners = policy.initialSigners)
        val afterEmpty = policy.applyConfigs(initial, emptyList())
        assertEquals(policy.displayInfos(initial), policy.displayInfos(afterEmpty))
        assertFalse(policy.displayInfos(afterEmpty).last().isDisabled)
        assertTrue(policy.displayInfos(afterEmpty).none { it.keyType == KeyType.SOFTWARE })
    }

    @Test fun `off-chain claim follows the server inheritance list for the claimed wallet type`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.ClaimInheritance("claim", false), walletType = WalletType.MULTI_SIG,
        ))
        assertTrue(policy.usesWalletConfigs)
        val state = policy.applyConfigs(SignerIntroState(supportedSigners = policy.initialSigners), listOf(
            config(WalletType.MULTI_SIG, true, SignerType.AIRGAP, SignerTag.KRUX),
            config(WalletType.MULTI_SIG, true, SignerType.AIRGAP),
            config(WalletType.MULTI_SIG, true, SignerType.NFC),
            // Not an inheritance key, and another wallet type: neither may add a card.
            config(WalletType.MULTI_SIG, false, SignerType.AIRGAP, SignerTag.SEEDSIGNER),
            config(WalletType.MINISCRIPT, true, SignerType.AIRGAP, SignerTag.SEEDSIGNER),
        ))
        val cards = policy.displayInfos(state)
        assertEquals(
            listOf(KeyType.TAPSIGNER, KeyType.KRUX, KeyType.SOFTWARE, KeyType.GENERIC_AIRGAP),
            cards.map { it.keyType },
        )
        assertTrue(cards.none { it.isDisabled })
    }

    @Test fun `off-chain claim keeps its fallback catalog until matching server configs arrive`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.ClaimInheritance("claim", false), walletType = WalletType.MULTI_SIG,
        ))
        val initial = SignerIntroState(supportedSigners = policy.initialSigners)
        val afterOtherWallet = policy.applyConfigs(initial, listOf(
            config(WalletType.MINISCRIPT, true, SignerType.AIRGAP, SignerTag.JADE),
        ))
        assertEquals(policy.displayInfos(initial), policy.displayInfos(afterOtherWallet))
        val cards = policy.displayInfos(afterOtherWallet)
        assertTrue(cards.any { it.keyType == KeyType.SOFTWARE && !it.isDisabled })
        assertTrue(cards.none { it.keyType == KeyType.PORTAL })
    }

    @Test fun `taproot caller restrictions retain address type and disable unsupported devices`() {
        val allowed = signer(SignerType.AIRGAP, SignerTag.JADE).copy(addressType = AddressType.TAPROOT)
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(
            flow = SignerIntroFlow.AddWalletKey, supportedSigners = listOf(allowed),
        ))
        assertEquals(AddressType.TAPROOT, policy.initialSigners.single().addressType)
        val cards = policy.displayInfos(SignerIntroState(supportedSigners = policy.initialSigners))
        assertFalse(cards.single { it.keyType == KeyType.JADE }.isDisabled)
        assertTrue(cards.single { it.keyType == KeyType.KEYSTONE }.isDisabled)
    }

    @Test fun `primary key flow keeps software enabled and disables device keys`() {
        val policy = SignerIntroPickerPolicy(SignerIntroRequest(keyFlow = KeyFlow.SIGN_UP))
        val cards = policy.displayInfos(SignerIntroState())
        assertFalse(cards.single { it.keyType == KeyType.SOFTWARE }.isDisabled)
        assertTrue(cards.filter { it.keyType != KeyType.SOFTWARE }.all { it.isDisabled })
    }

    // The inheritance entries the server returned on testnet: one list per wallet type.
    private val serverInheritanceConfigs = listOf(
        WalletType.MINISCRIPT to listOf(
            SignerType.AIRGAP to null, SignerType.AIRGAP to SignerTag.COLDCARD, SignerType.AIRGAP to SignerTag.JADE,
            SignerType.AIRGAP to SignerTag.KRUX, SignerType.COLDCARD_NFC to null, SignerType.HARDWARE to SignerTag.BITBOX,
            SignerType.HARDWARE to SignerTag.COLDCARD, SignerType.HARDWARE to SignerTag.JADE, SignerType.HARDWARE to SignerTag.LEDGER,
        ),
        WalletType.MULTI_SIG to listOf(
            SignerType.AIRGAP to null, SignerType.AIRGAP to SignerTag.COLDCARD, SignerType.AIRGAP to SignerTag.JADE,
            SignerType.AIRGAP to SignerTag.KEYSTONE, SignerType.AIRGAP to SignerTag.KRUX, SignerType.AIRGAP to SignerTag.PASSPORT,
            SignerType.COLDCARD_NFC to null, SignerType.HARDWARE to SignerTag.BITBOX, SignerType.HARDWARE to SignerTag.COLDCARD,
            SignerType.HARDWARE to SignerTag.JADE, SignerType.HARDWARE to SignerTag.LEDGER, SignerType.HARDWARE to SignerTag.TREZOR,
            SignerType.NFC to null,
        ),
    ).flatMap { (wallet, signers) -> signers.map { (type, tag) -> config(wallet, true, type, tag) } }

    @Test fun `inheritance pickers are scoped to their wallet type even when the caller passes none`() {
        val onChain = listOf(KeyType.COLDCARD, KeyType.JADE, KeyType.KRUX, KeyType.BITBOX, KeyType.LEDGER, KeyType.GENERIC_AIRGAP)
        val offChain = listOf(
            KeyType.TAPSIGNER, KeyType.COLDCARD, KeyType.JADE, KeyType.KEYSTONE, KeyType.KRUX, KeyType.FOUNDATION,
            KeyType.BITBOX, KeyType.LEDGER, KeyType.TREZOR, KeyType.GENERIC_AIRGAP,
        )
        for ((flow, expected) in listOf(
            SignerIntroFlow.OnChainTimelockKey(isInheritanceKey = true) to onChain,
            SignerIntroFlow.OffChainInheritanceKey() to offChain,
        )) {
            val policy = SignerIntroPickerPolicy(SignerIntroRequest(flow = flow))
            val state = policy.applyConfigs(SignerIntroState(supportedSigners = policy.initialSigners), serverInheritanceConfigs)
            val cards = policy.displayInfos(state).map { it.keyType }
            assertEquals("$flow", expected.sorted(), cards.sorted())
            assertEquals("$flow shows a device twice", cards.distinct(), cards)
        }
    }

    private fun signer(type: SignerType, tag: SignerTag? = null) = SupportedSigner(
        type, tag, WalletType.MULTI_SIG, AddressType.NATIVE_SEGWIT,
    )
    private fun config(wallet: WalletType, inheritance: Boolean, type: SignerType, tag: SignerTag? = null) =
        SupportedSignerConfig(wallet.name, inheritance, type.name, tag?.name)
}
