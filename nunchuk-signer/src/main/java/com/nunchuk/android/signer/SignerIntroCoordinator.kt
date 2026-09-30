package com.nunchuk.android.signer

import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.type.SignerTag

/** Routing policy only: no Activity, Intent, navigation controller or asynchronous work. */
class SignerIntroCoordinator(val request: SignerIntroRequest) {
    private val flow = request.flow
    val isInheritanceSetup = flow is SignerIntroFlow.OffChainInheritanceKey
    val verifyingKeyType = (flow as? SignerIntroFlow.VerifyBackup)?.currentSigner?.toReAddKeyType()

    private val isManagedKeyFlow = when (flow) {
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey -> false
        is SignerIntroFlow.OnChainTimelockKey, is SignerIntroFlow.OffChainInheritanceKey,
        is SignerIntroFlow.ClaimInheritance, is SignerIntroFlow.VerifyBackup -> true
    }

    fun select(keyType: KeyType): SignerIntroAction {
        if (shouldOfferExisting(keyType)) return SignerIntroAction.OfferExisting(keyType)
        return openNew(keyType)
    }

    private fun shouldOfferExisting(keyType: KeyType): Boolean = when (flow) {
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
        is SignerIntroFlow.VerifyBackup -> false
        is SignerIntroFlow.OffChainInheritanceKey -> true
        is SignerIntroFlow.OnChainTimelockKey -> keyType == KeyType.TAPSIGNER
        is SignerIntroFlow.ClaimInheritance -> when (keyType) {
            KeyType.TAPSIGNER, KeyType.SOFTWARE -> true
            KeyType.COLDCARD, KeyType.JADE, KeyType.SEEDSIGNER, KeyType.KEYSTONE,
            KeyType.FOUNDATION, KeyType.KRUX -> !flow.isOnChain
            KeyType.LEDGER, KeyType.BITBOX, KeyType.TREZOR, KeyType.PORTAL,
            KeyType.GENERIC_AIRGAP, KeyType.PLATFORM_KEY -> false
        }
    }

    /** Continue after the reuse offer, without running selection policy a second time. */
    fun createNew(keyType: KeyType): SignerIntroAction =
        if (keyType == KeyType.JADE && flow is SignerIntroFlow.ClaimInheritance && !flow.isOnChain) {
            // This guide completes the claim hand-off itself, like the firmware guide below.
            jadeGuide()
        } else openNew(keyType)

    fun firmwareChecked(device: SignerFirmwareDevice): SignerDeviceAction = when (device) {
        SignerFirmwareDevice.COLDCARD -> SignerIntroAction.Coldcard(returnResult = true)
        SignerFirmwareDevice.JADE -> jadeGuide()
    }

    private fun openNew(keyType: KeyType): SignerIntroAction = when (keyType) {
        KeyType.TAPSIGNER -> SignerIntroAction.Tapsigner(isManagedKeyFlow)
        KeyType.COLDCARD -> if (needsFirmwareCheck) {
            SignerIntroAction.CheckFirmware(SignerFirmwareDevice.COLDCARD)
        } else SignerIntroAction.Coldcard(isManagedKeyFlow)
        KeyType.JADE -> if (needsFirmwareCheck) {
            SignerIntroAction.CheckFirmware(SignerFirmwareDevice.JADE)
        } else airgap(SignerTag.JADE)
        KeyType.SEEDSIGNER, KeyType.KEYSTONE, KeyType.FOUNDATION, KeyType.KRUX ->
            airgap(keyType.toSignerTypeAndTag().second)
        KeyType.GENERIC_AIRGAP -> airgap(
            tag = null,
            forceHandoff = !isInheritanceSetup && verifyingKeyType == null,
        )
        KeyType.LEDGER -> hardware(SignerHardwareDevice.LEDGER)
        KeyType.BITBOX -> hardware(SignerHardwareDevice.BITBOX)
        KeyType.TREZOR -> hardware(SignerHardwareDevice.TREZOR)
        KeyType.PORTAL -> SignerIntroAction.Portal
        KeyType.SOFTWARE -> if (flow is SignerIntroFlow.ClaimInheritance) {
            SignerIntroAction.RecoverSoftware
        } else SignerIntroAction.CreateSoftware
        KeyType.PLATFORM_KEY -> SignerIntroAction.ReturnPlatformKey
    }

    private val needsFirmwareCheck: Boolean
        get() = flow is SignerIntroFlow.OnChainTimelockKey ||
            (flow is SignerIntroFlow.ClaimInheritance && flow.isOnChain)

    private fun hardware(device: SignerHardwareDevice): SignerIntroAction {
        val tag = device.tag
        return when (flow) {
            is SignerIntroFlow.VerifyBackup -> {
                val xfp = flow.currentSigner?.fingerPrint.orEmpty()
                // A missing fingerprint must never turn verification into accepting any device.
                if (xfp.isEmpty()) SignerIntroAction.ReturnHardwareTag(tag)
                else SignerIntroAction.Hardware(device, HardwareMode.Verify(xfp))
            }
            is SignerIntroFlow.ClaimInheritance -> SignerIntroAction.Hardware(
                device, HardwareMode.Claim(flow.keyIndex.coerceAtLeast(0))
            )
            is SignerIntroFlow.OffChainInheritanceKey, is SignerIntroFlow.OnChainTimelockKey ->
                SignerIntroAction.ReturnHardwareTag(tag)
            SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
            SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey ->
                SignerIntroAction.Hardware(device, HardwareMode.Add)
        }
    }

    private fun airgap(tag: SignerTag?, forceHandoff: Boolean = false): SignerIntroAction.Airgap {
        val replacedXfp = when (flow) {
            is SignerIntroFlow.OnChainTimelockKey -> flow.replaceInfo?.replacedXfp
            is SignerIntroFlow.OffChainInheritanceKey -> flow.replaceInfo?.replacedXfp
            SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
            SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
            is SignerIntroFlow.ClaimInheritance, is SignerIntroFlow.VerifyBackup -> null
        }?.takeIf { it.isNotEmpty() }
        // Replacement is owned by the device screen; returning a signer would replace it twice.
        return SignerIntroAction.Airgap(
            tag = tag,
            returnResult = !forceHandoff && isManagedKeyFlow && replacedXfp == null,
            replacedXfp = replacedXfp,
        )
    }

    private fun jadeGuide() = SignerIntroAction.Airgap(
        tag = SignerTag.JADE,
        returnResult = false,
        replacedXfp = (flow as? SignerIntroFlow.OnChainTimelockKey)?.replaceInfo?.replacedXfp,
    )
}

sealed interface HardwareMode {
    data object Add : HardwareMode
    data class Claim(val accountIndex: Int) : HardwareMode
    data class Verify(val expectedXfp: String) : HardwareMode
}

sealed interface SignerIntroAction {
    data class OfferExisting(val keyType: KeyType) : SignerIntroAction
    data class CheckFirmware(val device: SignerFirmwareDevice) : SignerIntroAction
    data class Tapsigner(val returnResult: Boolean) : SignerDeviceAction
    data class Coldcard(val returnResult: Boolean) : SignerDeviceAction
    data class Airgap(val tag: SignerTag?, val returnResult: Boolean, val replacedXfp: String?) : SignerDeviceAction
    data class Hardware(val device: SignerHardwareDevice, val mode: HardwareMode) : SignerDeviceAction
    data class ReturnHardwareTag(val tag: SignerTag) : SignerIntroAction
    data object Portal : SignerDeviceAction
    data object CreateSoftware : SignerDeviceAction
    data object RecoverSoftware : SignerIntroAction
    data object ReturnPlatformKey : SignerIntroAction
}

sealed interface SignerDeviceAction : SignerIntroAction

/** Only devices with an in-app pairing implementation can be passed to the launcher. */
enum class SignerHardwareDevice(val tag: SignerTag) {
    LEDGER(SignerTag.LEDGER), BITBOX(SignerTag.BITBOX), TREZOR(SignerTag.TREZOR),
}

/** Only these devices have a Miniscript firmware gate. */
enum class SignerFirmwareDevice(val tag: SignerTag) {
    COLDCARD(SignerTag.COLDCARD), JADE(SignerTag.JADE),
}
