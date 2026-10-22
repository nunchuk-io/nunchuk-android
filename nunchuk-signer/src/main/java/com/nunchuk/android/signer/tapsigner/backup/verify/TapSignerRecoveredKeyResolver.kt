package com.nunchuk.android.signer.tapsigner.backup.verify

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.signer.toRecoveredKey
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.inheritance.RecoveredKey
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import javax.inject.Inject

/** What a TAPSIGNER backup verification may send for the draft it belongs to. */
sealed interface TapSignerVerifyAccount {
    /** A legacy draft: the server verifies by fingerprint. */
    data object NotRequired : TapSignerVerifyAccount

    /** A configurable draft: verify against this account. */
    data class Resolved(val key: RecoveredKey) : TapSignerVerifyAccount

    /** The account xpub is not cached on this phone: tap the card to read [derivationPath]. */
    data class NeedsTap(val derivationPath: String) : TapSignerVerifyAccount

    /** Could not tell or build what is required; nothing may be submitted. Retryable. */
    data class Failed(val error: Throwable) : TapSignerVerifyAccount
}

/**
 * The account a configurable on-chain draft verifies a TAPSIGNER backup against (NUN-10320 `key`).
 * The backup check proves the decrypted backup is this master signer, so the account is derived from
 * it at the path the draft imported — never left out: a configurable draft gets either a complete
 * account, a request to tap the card, or an error.
 */
class TapSignerRecoveredKeyResolver @Inject constructor(
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
) {
    suspend operator fun invoke(groupId: String, masterSignerId: String): TapSignerVerifyAccount =
        resolveTapSignerVerifyAccount(
            draft = syncDraftWalletUseCase(groupId),
            masterSignerId = masterSignerId,
        ) { account ->
            getSignerFromMasterSignerByIndexUseCase(
                GetSignerFromMasterSignerByIndexUseCase.Param(
                    masterSignerId = masterSignerId,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT,
                    index = account,
                )
            )
        }

    /** Reads [derivationPath] off the tapped card (and caches it) after a [TapSignerVerifyAccount.NeedsTap]. */
    suspend fun readWithTap(
        isoDep: IsoDep,
        cvc: String,
        masterSignerId: String,
        derivationPath: String,
    ): TapSignerVerifyAccount =
        getSignerFromTapsignerMasterSignerByPathUseCase(
            GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                isoDep = isoDep,
                masterSignerId = masterSignerId,
                path = derivationPath,
                cvc = cvc,
            )
        ).fold(
            onSuccess = { signer ->
                signer.toRecoveredKey()?.let { TapSignerVerifyAccount.Resolved(it) }
                    ?: TapSignerVerifyAccount.Failed(IllegalStateException(MISSING_ACCOUNT))
            },
            onFailure = { TapSignerVerifyAccount.Failed(it) },
        )

}

private const val XPUB_NOT_CACHED = "-1009"
private const val MISSING_ACCOUNT = "The account of this key could not be read. Try again."

/**
 * The decision behind [TapSignerRecoveredKeyResolver]: an unreadable draft fails (it might be
 * configurable), a legacy draft needs no account, and a configurable one gets the derived account,
 * a tap request when its xpub is not cached, or a failure — never "nothing".
 */
internal suspend fun resolveTapSignerVerifyAccount(
    draft: Result<DraftWallet>,
    masterSignerId: String,
    deriveAccount: suspend (account: Int) -> Result<SingleSigner?>,
): TapSignerVerifyAccount {
    val draftWallet = draft.getOrElse { return TapSignerVerifyAccount.Failed(it) }
    if (draftWallet.onChainInheritancePolicy?.isConfigurable != true) return TapSignerVerifyAccount.NotRequired
    val path = draftWallet.signers
        .filter { it.xfp.equals(masterSignerId, ignoreCase = true) }
        .firstNotNullOfOrNull { signer -> signer.derivationPath?.takeIf { bip32AccountIndex(it) != null } }
        ?: return TapSignerVerifyAccount.Failed(IllegalStateException(MISSING_ACCOUNT))
    val result = deriveAccount(bip32AccountIndex(path)!!)
    val error = result.exceptionOrNull()
    if (error is NCNativeException && error.message.contains(XPUB_NOT_CACHED)) {
        return TapSignerVerifyAccount.NeedsTap(path)
    }
    val key = result.getOrNull()?.toRecoveredKey()
        ?: return TapSignerVerifyAccount.Failed(error ?: IllegalStateException(MISSING_ACCOUNT))
    return TapSignerVerifyAccount.Resolved(key)
}
