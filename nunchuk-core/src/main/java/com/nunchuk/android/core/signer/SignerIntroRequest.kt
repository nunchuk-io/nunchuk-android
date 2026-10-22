package com.nunchuk.android.core.signer

import android.os.Parcelable
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.WalletType
import kotlinx.parcelize.Parcelize

/** Caller intent. Device screens still receive their existing args through [deviceParams]. */
@Parcelize
data class SignerIntroRequest(
    val flow: SignerIntroFlow = SignerIntroFlow.AddKey,
    val walletId: String = "",
    val groupId: String = "",
    val supportedSigners: List<SupportedSigner> = emptyList(),
    @KeyFlow.PrimaryFlowInfo val keyFlow: Int = KeyFlow.NONE,
    val walletType: WalletType? = null,
    /** Consecutive accounts needed by a wallet slot; supported by Ledger and BitBox. */
    val accountCount: Int = 1,
    /**
     * Whether the wizard menu may offer "Restart wizard". Only the wizard's key lists grant it, and
     * only to a role that may restart; elsewhere (replace key, backup verification) it would reset
     * the membership plan from outside the wizard.
     */
    val canRestartWizard: Boolean = false,
) : Parcelable {
    val deviceParams: OnChainAddSignerParam?
        get() = flow.deviceParams
}

sealed class SignerIntroFlow : Parcelable {
    @Parcelize data object AddKey : SignerIntroFlow()
    @Parcelize data object AddWalletKey : SignerIntroFlow()
    @Parcelize data object AddAssistedWalletKey : SignerIntroFlow()
    @Parcelize data object ReplaceWalletKey : SignerIntroFlow()

    /**
     * A regular (non-inheritance) key of an assisted off-chain wallet. The picker offers the owner's
     * existing keys of the picked type and hands back either that key or, to set up a new one, the
     * key type; the key list runs the membership setup for either, as it did from its old sheet.
     */
    @Parcelize
    data class OffChainAssistedKey(
        /** A limited keyholder may only add a TAPSIGNER, Ledger, Trezor or (if allowed) software key. */
        val isKeyHolderLimited: Boolean = false,
        val allowSoftwareKey: Boolean = false,
        /** Keys already on the wallet, which cannot fill another slot. */
        val existingSigners: List<SignerModel> = emptyList(),
    ) : SignerIntroFlow()

    /**
     * @property inheritanceGuide which "Your … inheritance key" copy the guide shows.
     * @property showInheritanceGuide false once the guide ran in this wallet setup (configurable draft).
     * @property showDeviceSafety show "Before you use this device" before a new device is used.
     * @property guideOnly only run the inheritance guide and return RESULT_OK — the key was picked
     *   from those already in the setup, so no device is involved.
     */
    @Parcelize
    data class OnChainTimelockKey(
        val isInheritanceKey: Boolean = false,
        val keyIndex: Int = -1,
        val currentSigner: SignerModel? = null,
        val existingSigners: List<SignerModel> = emptyList(),
        val replaceInfo: OnChainAddSignerParam.ReplaceInfo? = null,
        val inheritanceGuide: OnChainInheritanceGuide = OnChainInheritanceGuide.LEGACY,
        val showInheritanceGuide: Boolean = true,
        val showDeviceSafety: Boolean = false,
        val guideOnly: Boolean = false,
    ) : SignerIntroFlow()

    /** Owner setting up (or replacing) an off-chain inheritance key. */
    @Parcelize
    data class OffChainInheritanceKey(
        val existingSigners: List<SignerModel> = emptyList(),
        val replaceInfo: OnChainAddSignerParam.ReplaceInfo? = null,
    ) : SignerIntroFlow()

    @Parcelize
    data class ClaimInheritance(
        val magic: String,
        val isOnChain: Boolean,
        val keyIndex: Int = -1,
    ) : SignerIntroFlow()

    @Parcelize
    data class VerifyBackup(
        val currentSigner: SignerModel?,
        val replaceInfo: OnChainAddSignerParam.ReplaceInfo? = null,
        val claimOption: ClaimOption? = null,
    ) : SignerIntroFlow()
}

/** Compatibility boundary for the downstream device flows; never used to infer caller intent. */
private val SignerIntroFlow.deviceParams: OnChainAddSignerParam?
    get() = when (this) {
        SignerIntroFlow.AddKey, SignerIntroFlow.AddWalletKey,
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey,
        is SignerIntroFlow.OffChainAssistedKey -> null
        is SignerIntroFlow.OnChainTimelockKey -> OnChainAddSignerParam(
            flags = if (isInheritanceKey) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER
                else OnChainAddSignerParam.FLAG_ADD_SIGNER,
            keyIndex = keyIndex,
            currentSigner = currentSigner,
            existingSigners = existingSigners,
            replaceInfo = replaceInfo,
        )
        is SignerIntroFlow.OffChainInheritanceKey -> OnChainAddSignerParam(
            flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER or
                OnChainAddSignerParam.FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER,
            existingSigners = existingSigners,
            replaceInfo = replaceInfo,
        )
        is SignerIntroFlow.ClaimInheritance -> OnChainAddSignerParam(
            flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER or
                (if (isOnChain) 0 else OnChainAddSignerParam.FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER),
            magic = magic,
            keyIndex = keyIndex,
        )
        is SignerIntroFlow.VerifyBackup -> OnChainAddSignerParam(
            flags = OnChainAddSignerParam.FLAG_VERIFY_BACKUP_SEED_PHRASE,
            currentSigner = currentSigner,
            replaceInfo = replaceInfo,
            claimOption = claimOption,
        )
    }

/** Copy of the inheritance-key guide shown before an on-chain inheritance key is added. */
enum class OnChainInheritanceGuide {
    /** The 1-of-3 HB plan. */
    LEGACY,

    /** Configurable draft, guided default: the separate key + one owner key (2 of 4). */
    DEFAULT_PRIMARY,

    /** Configurable draft, customized primary (KEY-03). */
    PRIMARY,
}
