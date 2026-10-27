package com.nunchuk.android.core.signer

import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.RecoveredKey
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import javax.inject.Inject

/**
 * The account a configurable on-chain key is marked verified against when the owner does not verify
 * it in the app ("Skip", "I'll do it myself", verify by myself). The server refuses
 * `SKIPPED_VERIFICATION` there (`VERIFICATION_EVIDENCE_REQUIRED`) and every key has to be verified
 * before the wallet can be created, so those choices are sent as `SELF_VERIFIED` with the account
 * the server already holds for the key.
 *
 * Each call returns null for a legacy draft / wallet, which keeps its old behavior, and a failure
 * when a configurable one has no account to send.
 */
class OnChainVerifyKeyResolver @Inject constructor(
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
) {
    suspend fun draftKey(groupId: String, xfp: String): Result<RecoveredKey?> =
        syncDraftWalletUseCase(groupId).mapCatching { draft ->
            if (draft.onChainInheritancePolicy?.isConfigurable != true) null
            else draft.signers.accountOf(xfp)
        }

    suspend fun replacementKey(groupId: String, walletId: String, xfp: String): Result<RecoveredKey?> =
        getReplaceWalletStatusUseCase(GetReplaceWalletStatusUseCase.Param(groupId, walletId)).mapCatching { status ->
            if (status.inheritanceCards.isEmpty()) null
            else status.replacements.values.flatten().accountOf(xfp)
        }

    private fun List<SignerServer>.accountOf(xfp: String): RecoveredKey =
        firstNotNullOfOrNull { signer ->
            val path = signer.derivationPath
            val xpub = signer.xpub
            if (signer.xfp.equals(xfp, ignoreCase = true) && !path.isNullOrEmpty() &&
                bip32AccountIndex(path) != null && !xpub.isNullOrEmpty()
            ) {
                RecoveredKey(xfp = xfp, derivationPath = path, xpub = xpub)
            } else {
                null
            }
        } ?: throw IllegalStateException(MISSING_ACCOUNT)
}

/** What a skip sends: verified against the account on a configurable draft / wallet, a skip on a legacy one. */
fun RecoveredKey?.skipVerifyType(): VerifyType =
    if (this != null) VerifyType.SELF_VERIFIED else VerifyType.SKIPPED_VERIFICATION

private const val MISSING_ACCOUNT = "The account of this key could not be read. Try again."
