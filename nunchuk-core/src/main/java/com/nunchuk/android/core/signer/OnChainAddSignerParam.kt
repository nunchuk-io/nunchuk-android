package com.nunchuk.android.core.signer

import android.os.Parcelable
import com.nunchuk.android.model.OnChainReplaceKeyStep
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnChainAddSignerParam(
    val flags: Int = 0,
    val keyIndex: Int = -1,
    val currentSigner: SignerModel? = null,
    val magic: String = "",
    val replaceInfo: ReplaceInfo? = null,
    val existingSigners: List<SignerModel> = emptyList()
) : Parcelable {

    val isClaiming: Boolean
        get() = magic.isNotEmpty()
    companion object {
        const val FLAG_ADD_INHERITANCE_SIGNER = 0x01 // Binary: 0001
        const val FLAG_VERIFY_BACKUP_SEED_PHRASE = 0x02 // Binary: 0010
        const val FLAG_ADD_SIGNER = 0x04 // Binary: 0100
        const val FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER = 0x08 // Binary: 1000
    }

    fun isAddInheritanceSigner(): Boolean {
        return flags and FLAG_ADD_INHERITANCE_SIGNER != 0
    }

    fun isAddInheritanceOffChainSigner(): Boolean {
        return flags and FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER != 0
    }

    fun isVerifyBackupSeedPhrase(): Boolean {
        return flags and FLAG_VERIFY_BACKUP_SEED_PHRASE != 0
    }

    fun isReplaceKeyFlow(): Boolean {
        return replaceInfo != null && replaceInfo.replacedXfp.isNotEmpty()
    }

    @Parcelize
    data class ReplaceInfo(
        val replacedXfp: String,
        val step: OnChainReplaceKeyStep?
    ) : Parcelable
}