package com.nunchuk.android.signer

import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import org.junit.Assert.*
import org.junit.Test

class SignerIntroCoordinatorTest {
    private val hardware = listOf(
        KeyType.LEDGER to SignerHardwareDevice.LEDGER,
        KeyType.BITBOX to SignerHardwareDevice.BITBOX,
        KeyType.TREZOR to SignerHardwareDevice.TREZOR,
    )
    private fun router(flow: SignerIntroFlow) = SignerIntroCoordinator(SignerIntroRequest(flow))

    @Test fun `ordinary assisted keys use the normal device flow without inheritance reuse`() {
        val normal = router(SignerIntroFlow.AddKey)
        for (flow in listOf(SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.AddWalletKey, SignerIntroFlow.ReplaceWalletKey)) {
            for (key in KeyType.entries) assertEquals("$flow / $key", normal.select(key), router(flow).select(key))
        }
        for ((key, device) in hardware) {
            assertEquals(SignerIntroAction.Hardware(device, HardwareMode.Add), normal.select(key))
        }
    }

    @Test fun `owner inheritance setup offers existing keys then opens device exactly once`() {
        val router = router(SignerIntroFlow.OffChainInheritanceKey())
        assertTrue(router.isInheritanceSetup)
        for (key in KeyType.entries) assertEquals(SignerIntroAction.OfferExisting(key), router.select(key))
        assertEquals(SignerIntroAction.Tapsigner(true), router.createNew(KeyType.TAPSIGNER))
        assertEquals(SignerIntroAction.Coldcard(true), router.createNew(KeyType.COLDCARD))
        assertEquals(SignerIntroAction.Airgap(SignerTag.JADE, true, null), router.createNew(KeyType.JADE))
        for ((key, device) in hardware) assertEquals(SignerIntroAction.ReturnHardwareTag(device.tag), router.createNew(key))
    }

    @Test fun `claim pairs every in-app hardware device using the plan account`() {
        for (onChain in listOf(true, false)) {
            val flow = SignerIntroFlow.ClaimInheritance("claim", onChain, keyIndex = 7)
            for ((key, device) in hardware) {
                assertEquals(SignerIntroAction.Hardware(device, HardwareMode.Claim(7)), router(flow).select(key))
            }
            assertFalse(router(flow).isInheritanceSetup)
        }
    }

    @Test fun `unspecified claim account defaults to zero`() {
        val router = router(SignerIntroFlow.ClaimInheritance("claim", false))
        for ((key, device) in hardware) {
            assertEquals(SignerIntroAction.Hardware(device, HardwareMode.Claim(0)), router.select(key))
        }
    }

    @Test fun `only on-chain flows require Coldcard and Jade firmware gates`() {
        val gated = listOf(SignerIntroFlow.OnChainTimelockKey(), SignerIntroFlow.ClaimInheritance("claim", true))
        for (flow in gated) {
            assertEquals(SignerIntroAction.CheckFirmware(SignerFirmwareDevice.COLDCARD), router(flow).select(KeyType.COLDCARD))
            assertEquals(SignerIntroAction.CheckFirmware(SignerFirmwareDevice.JADE), router(flow).select(KeyType.JADE))
        }
        val claim = router(SignerIntroFlow.ClaimInheritance("claim", false))
        assertEquals(SignerIntroAction.OfferExisting(KeyType.COLDCARD), claim.select(KeyType.COLDCARD))
        assertEquals(SignerIntroAction.Coldcard(true), claim.createNew(KeyType.COLDCARD))
        assertEquals(SignerIntroAction.Airgap(SignerTag.JADE, false, null), claim.createNew(KeyType.JADE))
    }

    @Test fun `off-chain claim offers reusable airgap keys and software recovery`() {
        val router = router(SignerIntroFlow.ClaimInheritance("claim", false))
        for (key in listOf(KeyType.JADE, KeyType.SEEDSIGNER, KeyType.KEYSTONE, KeyType.FOUNDATION, KeyType.KRUX, KeyType.SOFTWARE)) {
            assertEquals(SignerIntroAction.OfferExisting(key), router.select(key))
        }
        assertEquals(SignerIntroAction.RecoverSoftware, router.createNew(KeyType.SOFTWARE))
        assertEquals(SignerIntroAction.Airgap(SignerTag.KRUX, true, null), router.createNew(KeyType.KRUX))
    }

    @Test fun `backup verification never offers an already imported key`() {
        for ((key, device) in hardware) {
            val router = router(SignerIntroFlow.VerifyBackup(signer(SignerType.HARDWARE, listOf(device.tag))))
            assertEquals(key, router.verifyingKeyType)
            assertEquals(SignerIntroAction.Hardware(device, HardwareMode.Verify("1234abcd")), router.select(key))
        }
        val router = router(SignerIntroFlow.VerifyBackup(signer(SignerType.NFC)))
        assertEquals(KeyType.TAPSIGNER, router.verifyingKeyType)
        assertEquals(SignerIntroAction.Tapsigner(true), router.select(KeyType.TAPSIGNER))
    }

    @Test fun `verification without fingerprint does not accept an arbitrary hardware key`() {
        val router = router(SignerIntroFlow.VerifyBackup(null))
        assertNull(router.verifyingKeyType)
        assertEquals(SignerIntroAction.ReturnHardwareTag(SignerTag.LEDGER), router.select(KeyType.LEDGER))
    }

    @Test fun `replace is owned by airgap screen while verification returns a signer`() {
        val replace = OnChainAddSignerParam.ReplaceInfo("abcd1234", null)
        for (flow in listOf(SignerIntroFlow.OnChainTimelockKey(replaceInfo = replace), SignerIntroFlow.OffChainInheritanceKey(replaceInfo = replace))) {
            assertEquals(SignerIntroAction.Airgap(SignerTag.KEYSTONE, false, "abcd1234"), router(flow).createNew(KeyType.KEYSTONE))
        }
        val verify = router(SignerIntroFlow.VerifyBackup(signer(SignerType.AIRGAP, listOf(SignerTag.KEYSTONE)), replace))
        assertEquals(SignerIntroAction.Airgap(SignerTag.KEYSTONE, true, null), verify.select(KeyType.KEYSTONE))
    }

    @Test fun `generic airgap returns to owner setup and backup verification only`() {
        assertEquals(SignerIntroAction.Airgap(null, false, null), router(SignerIntroFlow.AddKey).select(KeyType.GENERIC_AIRGAP))
        assertEquals(SignerIntroAction.Airgap(null, true, null), router(SignerIntroFlow.OffChainInheritanceKey()).createNew(KeyType.GENERIC_AIRGAP))
        val verify = router(SignerIntroFlow.VerifyBackup(signer(SignerType.AIRGAP)))
        assertEquals(KeyType.GENERIC_AIRGAP, verify.verifyingKeyType)
        assertEquals(SignerIntroAction.Airgap(null, true, null), verify.select(KeyType.GENERIC_AIRGAP))
    }

    @Test fun `typed requests preserve downstream device parameters`() {
        val replace = OnChainAddSignerParam.ReplaceInfo("abcd1234", null)
        val existing = listOf(signer(SignerType.NFC))
        val flow = SignerIntroFlow.OnChainTimelockKey(true, 3, existing[0], existing, replace)
        assertEquals(OnChainAddSignerParam(
            flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
            keyIndex = 3, currentSigner = existing[0], existingSigners = existing, replaceInfo = replace,
        ), SignerIntroRequest(flow).deviceParams)
        val claim = SignerIntroRequest(SignerIntroFlow.ClaimInheritance("claim", false, 5)).deviceParams!!
        assertTrue(claim.isClaiming)
        assertTrue(claim.isAddInheritanceSigner())
        assertTrue(claim.isAddInheritanceOffChainSigner())
        assertEquals(5, claim.keyIndex)
        assertNull(SignerIntroRequest(SignerIntroFlow.AddAssistedWalletKey).deviceParams)
    }

    @Test fun `on-chain hardware selection returns tag to the wallet wizard`() {
        for (inheritance in listOf(false, true)) {
            val router = router(SignerIntroFlow.OnChainTimelockKey(isInheritanceKey = inheritance))
            for ((key, device) in hardware) {
                assertEquals(SignerIntroAction.ReturnHardwareTag(device.tag), router.select(key))
            }
        }
    }

    @Test fun `firmware continuation preserves Jade replacement ownership`() {
        val replace = OnChainAddSignerParam.ReplaceInfo("abcd1234", null)
        val router = router(SignerIntroFlow.OnChainTimelockKey(replaceInfo = replace))
        assertEquals(SignerIntroAction.Airgap(SignerTag.JADE, false, "abcd1234"), router.firmwareChecked(SignerFirmwareDevice.JADE))
        assertEquals(SignerIntroAction.Coldcard(true), router.firmwareChecked(SignerFirmwareDevice.COLDCARD))
    }

    @Test fun `verification adapter preserves sharing method and replacement context`() {
        val key = signer(SignerType.NFC)
        val replace = OnChainAddSignerParam.ReplaceInfo("abcd1234", null)
        for (option in listOf(null, ClaimOption.SEED_PHRASE, ClaimOption.ENCRYPTED_BACKUP)) {
            val params = SignerIntroRequest(SignerIntroFlow.VerifyBackup(key, replace, option)).deviceParams!!
            assertTrue(params.isVerifyBackupSeedPhrase())
            assertEquals(key, params.currentSigner)
            assertEquals(replace, params.replaceInfo)
            assertEquals(option, params.claimOption)
            assertFalse(params.isClaiming)
        }
    }

    @Test fun `every hardware key type routes to a supported device`() {
        val router = router(SignerIntroFlow.AddKey)
        val devices = KeyType.entries
            .filter { it.toSignerTypeAndTag().first == SignerType.HARDWARE }
            .map { key -> (router.select(key) as SignerIntroAction.Hardware).device }
        assertEquals(SignerHardwareDevice.entries.toSet(), devices.toSet())
    }

    private fun signer(type: SignerType, tags: List<SignerTag> = emptyList()) = SignerModel(
        id = "key", name = "Key", derivationPath = "m/48h/0h/0h/2h",
        fingerPrint = "1234abcd", type = type, tags = tags, isMasterSigner = true,
    )
}
