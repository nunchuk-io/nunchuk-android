package com.nunchuk.android.core.signer

import android.os.Parcelable
import com.nunchuk.android.core.R
import com.nunchuk.android.model.OnChainReplaceKeyStep
import com.nunchuk.android.model.toIndex
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
    /**
     * Set for a key of a configurable draft's primary-inheritance or fallback card. Those routes hold
     * a single account of the device, so the device intro names the route and the account instead of
     * "each device must be added twice … (n/2)" (Figma KEY-05b / KEY-05c). Null for a spending key,
     * which is added twice around the timelock, and for every legacy-wallet key.
     */
    val inheritanceRoute: OnChainInheritanceRoute? = null,
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

    /**
     * @property keyIndex a configurable wallet's server slot (`key_index`) the replacement fills;
     *   null on a legacy wallet, whose slot comes from [step].
     */
    @Parcelize
    data class ReplaceInfo(
        val replacedXfp: String,
        val step: OnChainReplaceKeyStep?,
        val keyIndex: Int? = null,
    ) : Parcelable {
        fun replaceKeyIndex(isGroupWallet: Boolean): Int? = keyIndex ?: step?.toIndex(isGroupWallet)
    }
}

/** A single-account inheritance route of a configurable draft: [fallbackNumber] is 1-based, 0 = primary. */
@Parcelize
data class OnChainInheritanceRoute(val fallbackNumber: Int = 0) : Parcelable

/** The single-account route of an on-chain timelock key being added, or null when it is added twice. */
fun OnChainAddSignerParam?.singleAccountRoute(): OnChainInheritanceRoute? =
    this?.inheritanceRoute?.takeIf { isOnChainTimelockKey() }

/** 1-based number of the fallback whose key this run verifies, or null for any other key. */
fun OnChainAddSignerParam?.verifiedFallbackNumber(): Int? =
    this?.inheritanceRoute?.fallbackNumber?.takeIf { isVerifyBackupSeedPhrase() && it > 0 }

/**
 * True only for a key of the on-chain timelock wallet, the one flow that adds each device twice
 * (Acct X / Acct Y) around a timelocked spending path. That pair is what earns the "(n/2)" title
 * and the "select account N for this spending path" copy on the add-key intro screens. An
 * off-chain inheritance key is an ordinary single add and must not show either.
 */
fun OnChainAddSignerParam?.isOnChainTimelockKey(): Boolean =
    this != null && !isVerifyBackupSeedPhrase() && !isClaiming && !isAddInheritanceOffChainSigner()

/**
 * True while verifying the seed-phrase backup of an on-chain timelock key — the only verification
 * whose key sits on a timelocked spending path, so the only one that may ask for an account on it.
 * [claimOption] is set by the off-chain inheritance flow alone, so a null one marks the on-chain
 * run; the off-chain key is simply re-added like any other.
 */
fun OnChainAddSignerParam?.isVerifyOnChainTimelockBackup(): Boolean =
    this != null && isVerifyBackupSeedPhrase() && claimOption == null

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