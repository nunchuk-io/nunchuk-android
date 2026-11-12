package com.nunchuk.android.signer

import com.nunchuk.android.core.signer.OnChainInheritanceGuide
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.type.SignerTag

/** Routing policy only: no Activity, Intent, navigation controller or asynchronous work. */
class SignerIntroCoordinator(val request: SignerIntroRequest) {
    private val flow = request.flow
    val isInheritanceSetup = flow is SignerIntroFlow.OffChainInheritanceKey
    val verifyingKeyType = (flow as? SignerIntroFlow.VerifyBackup)?.currentSigner?.toReAddKeyType()

    private val claimAccountDevice = (flow as? SignerIntroFlow.ClaimInheritance)?.accountDevice

    /**
     * Key type whose flow opens straight away, without the picker: the key being verified, or the
     * device a claim needs another account of.
     */
    val directKeyType: KeyType? = verifyingKeyType ?: claimAccountDevice?.toReAddKeyType()

    private val isManagedKeyFlow = when (flow) {
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
        is SignerIntroFlow.OffChainAssistedKey -> false
        is SignerIntroFlow.OnChainTimelockKey, is SignerIntroFlow.OffChainInheritanceKey,
        is SignerIntroFlow.ClaimInheritance, is SignerIntroFlow.VerifyBackup -> true
    }

    private val onChainKey = flow as? SignerIntroFlow.OnChainTimelockKey
    private val isAddingInheritanceKey = isInheritanceSetup || onChainKey?.isInheritanceKey == true

    /** A slot of a configurable on-chain draft (primary or fallback), not the legacy inheritance step. */
    val isConfigurableDraft: Boolean = onChainKey != null && onChainKey.inheritanceGuide != OnChainInheritanceGuide.LEGACY

    /**
     * A configurable draft's inheritance slot whose guide has not run yet: the inheritance key intro,
     * the passphrase notice and "Before you use this device" (KEY-03 → KEY-04 → KEY-02) open before
     * the key-type picker, as general information that does not depend on the key picked.
     */
    val showsGuidesBeforePicker: Boolean =
        isConfigurableDraft && onChainKey?.isInheritanceKey == true && onChainKey.showInheritanceGuide

    /** Whether the pre-picker guides end with "Before you use this device". */
    val guidesBeforePickerIncludeDeviceSafety: Boolean = showsGuidesBeforePicker && onChainKey?.showDeviceSafety == true

    /**
     * Elsewhere, adding an inheritance key with a seed phrase shows the seed-phrase and passphrase
     * guides after the pick; [guideCompleted] then resumes with the same key. A key without one
     * (TAPSIGNER) has nothing to back up for the Beneficiary, so it goes straight on.
     */
    fun select(keyType: KeyType): SignerIntroAction = when {
        // The guides, device safety included, already ran before the picker.
        showsGuidesBeforePicker -> if (showsDeviceSafety(keyType)) {
            deviceSafetyCompleted(keyType)
        } else guideCompleted(keyType)
        isAddingInheritanceKey && keyType.hasSeedPhrase && onChainKey?.showInheritanceGuide != false ->
            SignerIntroAction.ShowInheritanceGuide(keyType)
        else -> guideCompleted(keyType)
    }

    /** Continue after the inheritance guides, without showing them a second time. */
    fun guideCompleted(keyType: KeyType): SignerIntroAction {
        if (showsDeviceSafety(keyType)) return SignerIntroAction.ShowDeviceSafety(keyType)
        if (shouldOfferExisting(keyType)) return SignerIntroAction.OfferExisting(keyType)
        return openNew(keyType)
    }

    /**
     * "Before you use this device" is done: offer the keys of that type already on this phone on
     * top of it, so the owner never goes back to the key list for them; none found opens the device.
     */
    fun deviceSafetyCompleted(keyType: KeyType): SignerIntroAction = SignerIntroAction.OfferExisting(keyType)

    /** The device-safety notice comes first for a device when the caller asks for it. */
    private fun showsDeviceSafety(keyType: KeyType): Boolean =
        onChainKey?.showDeviceSafety == true && keyType.isDevice

    private fun shouldOfferExisting(keyType: KeyType): Boolean = when (flow) {
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
        is SignerIntroFlow.VerifyBackup -> false
        // The key list checks software keys itself (two on one device), so the offer skips them.
        is SignerIntroFlow.OffChainAssistedKey -> keyType != KeyType.SOFTWARE && keyType != KeyType.PLATFORM_KEY
        is SignerIntroFlow.OffChainInheritanceKey -> true
        is SignerIntroFlow.OnChainTimelockKey -> keyType == KeyType.TAPSIGNER
        // Another account of a device already added has to come off that device, not a saved key.
        is SignerIntroFlow.ClaimInheritance -> if (flow.accountDevice != null) false else when (keyType) {
            KeyType.TAPSIGNER, KeyType.SOFTWARE -> true
            KeyType.COLDCARD, KeyType.JADE, KeyType.SEEDSIGNER, KeyType.KEYSTONE,
            KeyType.FOUNDATION, KeyType.KRUX -> !flow.isOnChain
            KeyType.LEDGER, KeyType.BITBOX, KeyType.TREZOR, KeyType.PORTAL,
            KeyType.GENERIC_AIRGAP, KeyType.PLATFORM_KEY -> false
        }
    }

    /**
     * Continue after the reuse offer, without running selection policy a second time. The
     * device-safety notice, when shown, already came before the offer.
     */
    fun createNew(keyType: KeyType): SignerIntroAction =
        if (keyType == KeyType.JADE && flow is SignerIntroFlow.ClaimInheritance && !flow.isOnChain) {
            // This guide completes the claim hand-off itself, like the firmware guide below.
            jadeGuide()
        } else openNew(keyType)

    fun firmwareChecked(device: SignerFirmwareDevice): SignerDeviceAction = when (device) {
        SignerFirmwareDevice.COLDCARD -> SignerIntroAction.Coldcard(returnResult = true)
        SignerFirmwareDevice.JADE -> jadeGuide()
    }

    private fun openNew(keyType: KeyType): SignerIntroAction = if (flow is SignerIntroFlow.OffChainAssistedKey) {
        // The key list owns the membership setup of a new key; this screen only picked its type.
        SignerIntroAction.ReturnKeyType(keyType)
    } else when (keyType) {
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
        // A claim account of a device already added skips it: that device passed it for account 0.
        get() = flow is SignerIntroFlow.OnChainTimelockKey ||
            (flow is SignerIntroFlow.ClaimInheritance && flow.isOnChain && flow.accountDevice == null)

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
            SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
            is SignerIntroFlow.OffChainAssistedKey ->
                SignerIntroAction.Hardware(device, HardwareMode.Add)
        }
    }

    private fun airgap(tag: SignerTag?, forceHandoff: Boolean = false): SignerIntroAction.Airgap {
        val replacedXfp = when (flow) {
            is SignerIntroFlow.OnChainTimelockKey -> flow.replaceInfo?.replacedXfp
            is SignerIntroFlow.OffChainInheritanceKey -> flow.replaceInfo?.replacedXfp
            SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
            SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
            is SignerIntroFlow.ClaimInheritance, is SignerIntroFlow.VerifyBackup,
            is SignerIntroFlow.OffChainAssistedKey -> null
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
    data class ShowInheritanceGuide(val keyType: KeyType) : SignerIntroAction
    data class ShowDeviceSafety(val keyType: KeyType) : SignerIntroAction
    data class OfferExisting(val keyType: KeyType) : SignerIntroAction
    data class CheckFirmware(val device: SignerFirmwareDevice) : SignerIntroAction
    data class Tapsigner(val returnResult: Boolean) : SignerDeviceAction
    data class Coldcard(val returnResult: Boolean) : SignerDeviceAction
    data class Airgap(val tag: SignerTag?, val returnResult: Boolean, val replacedXfp: String?) : SignerDeviceAction
    data class Hardware(val device: SignerHardwareDevice, val mode: HardwareMode) : SignerDeviceAction
    data class ReturnHardwareTag(val tag: SignerTag) : SignerIntroAction
    data class ReturnKeyType(val keyType: KeyType) : SignerIntroAction
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

/** A physical key the owner touches: software keys and the Platform key are not. */
private val KeyType.isDevice: Boolean
    get() = this != KeyType.SOFTWARE && this != KeyType.PLATFORM_KEY

/** TAPSIGNER never exposes its seed, and the platform key lives on the server. */
private val KeyType.hasSeedPhrase: Boolean
    get() = when (this) {
        KeyType.TAPSIGNER, KeyType.PLATFORM_KEY -> false
        KeyType.COLDCARD, KeyType.JADE, KeyType.PORTAL, KeyType.SEEDSIGNER, KeyType.KEYSTONE,
        KeyType.FOUNDATION, KeyType.LEDGER, KeyType.BITBOX, KeyType.TREZOR, KeyType.KRUX,
        KeyType.SOFTWARE, KeyType.GENERIC_AIRGAP -> true
    }
