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
) : Parcelable {
    val deviceParams: OnChainAddSignerParam?
        get() = flow.deviceParams
}

sealed class SignerIntroFlow : Parcelable {
    @Parcelize data object AddKey : SignerIntroFlow()
    @Parcelize data object AddWalletKey : SignerIntroFlow()
    @Parcelize data object AddAssistedWalletKey : SignerIntroFlow()
    @Parcelize data object ReplaceWalletKey : SignerIntroFlow()

    @Parcelize
    data class OnChainTimelockKey(
        val isInheritanceKey: Boolean = false,
        val keyIndex: Int = -1,
        val currentSigner: SignerModel? = null,
        val existingSigners: List<SignerModel> = emptyList(),
        val replaceInfo: OnChainAddSignerParam.ReplaceInfo? = null,
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
        SignerIntroFlow.AddAssistedWalletKey, SignerIntroFlow.ReplaceWalletKey -> null
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
