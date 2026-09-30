package com.nunchuk.android.main.membership.honey.distribution

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable

/**
 * The screens an off-chain inheritance key row can open, in one place.
 *
 * Both assisted key lists — personal ([com.nunchuk.android.main.membership.key.AddKeyListFragment])
 * and group
 * ([com.nunchuk.android.main.membership.byzantine.addKey.AddByzantineKeyListFragment]) — drive the
 * identical flow and differ only in which view model they hand the work to, so the navigation
 * lives here rather than being mirrored in each.
 *
 * Replacing an inheritance key
 * ([com.nunchuk.android.main.membership.replacekey.ReplaceKeysFragment]) runs the same screens
 * against the wallet's replacement rather than the draft wallet; that is what a non-empty
 * `walletId` selects, all the way down to which `claim-options` endpoint is written.
 */

/** Back up the seed phrase, then prove it by restoring onto a device and re-adding the key. */
internal fun Fragment.openInheritanceSeedPhraseBackup(
    navigator: NunchukNavigator,
    signer: SignerModel,
    groupId: String,
    walletId: String,
    launcher: ActivityResultLauncher<Intent>,
    replacedXfp: String? = null,
) {
    navigator.openBackUpSeedPhraseActivity(
        activityContext = requireActivity(),
        args = BackUpSeedPhraseArgs(
            type = BackUpSeedPhraseType.INTRO,
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            replacedXfp = replacedXfp,
            claimOption = ClaimOption.SEED_PHRASE,
            walletType = WalletType.MULTI_SIG,
        ),
        launcher = launcher,
    )
}

/** The restored device matched, so the seed-phrase backup of [signer] is proven. */
internal fun Fragment.openInheritanceSeedPhraseVerified(
    navigator: NunchukNavigator,
    signer: SignerModel?,
    groupId: String,
    walletId: String,
) {
    navigator.openBackUpSeedPhraseActivity(
        activityContext = requireActivity(),
        args = BackUpSeedPhraseArgs.verified(
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            claimOption = ClaimOption.SEED_PHRASE,
        ),
    )
}

/**
 * The checklist of what the owner chose: one card per sharing method, each verified on its own,
 * plus the way back to the choice itself. [claimOptions] is what the row already records, so the
 * screen draws the right cards before the server has been re-read.
 */
internal fun Fragment.openInheritanceVerifyBackups(
    signer: SignerModel,
    groupId: String,
    launcher: ActivityResultLauncher<Intent>,
    claimOptions: List<ClaimOption>,
    walletId: String = "",
) {
    launcher.launch(
        KeyDistributionActivity.buildIntent(
            activityContext = requireActivity(),
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            entry = KeyDistributionEntry.VERIFY_BACKUPS,
            claimOptions = claimOptions,
        )
    )
}

/** Straight after the key landed: confirm it, then pick how it reaches the Beneficiary. */
internal fun Fragment.openInheritanceKeyAdded(
    signer: SignerModel,
    groupId: String,
    launcher: ActivityResultLauncher<Intent>,
    walletId: String = "",
) {
    launcher.launch(
        KeyDistributionActivity.buildIntent(
            activityContext = requireActivity(),
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            entry = KeyDistributionEntry.KEY_ADDED,
        )
    )
}

/** The sharing-method choice, entered from the key row rather than opening itself. */
internal fun Fragment.openInheritanceSharingMethod(
    signer: SignerModel,
    groupId: String,
    launcher: ActivityResultLauncher<Intent>,
    walletId: String = "",
) {
    launcher.launch(
        KeyDistributionActivity.buildIntent(
            activityContext = requireActivity(),
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            entry = KeyDistributionEntry.SHARING_METHOD,
        )
    )
}

/**
 * Which half of a "do both" key the checklist asked the key list to verify, and on which key.
 * Null when the flow ended without asking for one.
 *
 * The checklist only chooses: the flows that verify an artifact need step state the key list
 * owns — the backup file path of a TAPSIGNER or Coldcard, for one — so it is the key list that
 * runs them.
 */
internal fun Intent.verifyClaimOptionRequest(): Pair<ClaimOption, SignerModel>? {
    val option = getStringExtra(KeyDistributionActivity.EXTRA_VERIFY_CLAIM_OPTION)
        ?.let { name -> ClaimOption.entries.firstOrNull { it.name == name } }
        ?: return null
    val signer = parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER) ?: return null
    return option to signer
}
