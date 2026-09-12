package com.nunchuk.android.core.signer

import android.os.Parcelable
import com.nunchuk.android.core.R
import com.nunchuk.android.model.OnChainReplaceKeyStep
import com.nunchuk.android.model.inheritance.ClaimOption
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnChainAddSignerParam(
    val flags: Int = 0,
    val keyIndex: Int = -1,
    val currentSigner: SignerModel? = null,
    val magic: String = "",
    val replaceInfo: ReplaceInfo? = null,
    val existingSigners: List<SignerModel> = emptyList(),
    /**
     * Which sharing method of an off-chain inheritance key a [FLAG_VERIFY_BACKUP_SEED_PHRASE] run
     * is proving. The two artifacts of a "do both" key are verified independently server-side, so
     * the screens that mark the key verified have to name the one they just checked. Null on the
     * on-chain timelock flow, which keeps a single verification per key.
     */
    val claimOption: ClaimOption? = null,
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

/**
 * Returns the string resource id for the "select key type" subtitle on the signer intro screen.
 * Use with [androidx.compose.ui.res.stringResource] in Compose.
 */
fun OnChainAddSignerParam?.getSelectKeyTypeSubtitleRes(): Int = when {
    // Only the Beneficiary is "adding the key to Nunchuk"; the owner setting the plan up is
    // choosing which device the inheritance key will live on.
    this?.isAddInheritanceOffChainSigner() == true && isClaiming ->
        R.string.nc_add_inheritance_key_to_nunchuk

    else -> R.string.nc_select_your_key_type
}