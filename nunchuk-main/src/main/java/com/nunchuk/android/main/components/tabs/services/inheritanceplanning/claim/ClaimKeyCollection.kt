package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.multiSigAccountIndex
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.type.SignerType

/** What follows adding a key to an on-chain claim (duplicates are rejected before this). */
internal sealed interface OnChainKeyStep {
    /** A device with a requested account came back with another one: ask for [origin] again. */
    data class WrongAccount(val origin: KeyOrigin) : OnChainKeyStep

    /** Not a key of this plan (init `key_origins`). */
    data object NotInPlan : OnChainKeyStep

    /** This exact account is already in. */
    data object AlreadyAdded : OnChainKeyStep

    /** Submit [data]'s keys now: a requested account was imported, or enough keys are in. */
    data class Download(val data: ClaimData) : OnChainKeyStep

    /** Keep collecting keys. */
    data class AddMore(val data: ClaimData) : OnChainKeyStep
}

/**
 * Adds [signer]. A requested account is submitted straight away, with the accepted keys, so a route
 * it completes is not held up by another device's pending account (NUN-10320 §7).
 *
 * The wrong-account check runs before the duplicate check: a device asked for Account N usually
 * comes back with Account 0 again, which is already in, and must lead to CLM-16, not "already added".
 */
internal fun ClaimData.addOnChainSigner(signer: SignerModel): OnChainKeyStep {
    val requested = pendingOriginOf(signer)
    // A software key's accounts are derived in the app (nextAccountImport), never asked of the heir.
    if (requested == null && signer.type != SignerType.SOFTWARE && isPendingDevice(signer)) {
        return OnChainKeyStep.WrongAccount(pendingKeyOrigins.first { it.xfp.equals(signer.fingerPrint, ignoreCase = true) })
    }
    // A requested account comes from download-wallet, so it counts as part of the plan even when
    // init's `key_origins` lists only account 0.
    if (requested == null && !isInPlanOrigins(signer)) return OnChainKeyStep.NotInPlan
    if (signers.any { it.isSameAccount(signer) }) return OnChainKeyStep.AlreadyAdded
    val next = copy(signers = signers + signer, pendingKeyOrigins = pendingKeyOrigins - setOfNotNull(requested))
    return if (requested != null || next.addedKeyCount >= next.requiredKeyCount) {
        OnChainKeyStep.Download(next)
    } else {
        OnChainKeyStep.AddMore(next)
    }
}

/** The next requested account to import. */
internal sealed interface AccountImportStep {
    /** A software key on this phone: derive [origin] in the app. */
    data class Derive(val origin: KeyOrigin) : AccountImportStep

    /** A device: the heir adds [account] from [device] (CLM-16). */
    data class AskDevice(val origin: KeyOrigin, val device: SignerModel, val account: Int) : AccountImportStep

    /** Nothing left the app can import: another key is needed. */
    data object AddAnotherKey : AccountImportStep
}

/**
 * Software keys first, since they need no one. A software account already derived and sent once
 * ([submittedDerivations]) was rejected, so it is not derived again.
 */
internal fun ClaimData.nextAccountImport(submittedDerivations: Set<KeyOrigin>): AccountImportStep {
    pendingKeyOrigins.sortedBy { deviceOf(it)?.type != SignerType.SOFTWARE }.forEach { origin ->
        val device = deviceOf(origin) ?: return@forEach
        val account = origin.derivationPath.multiSigAccountIndex ?: return@forEach
        if (device.type == SignerType.SOFTWARE) {
            if (origin !in submittedDerivations) return AccountImportStep.Derive(origin)
        } else {
            return AccountImportStep.AskDevice(origin, device, account)
        }
    }
    return AccountImportStep.AddAnotherKey
}

/** CLM-16 "Show me how" / "Add Account N": that device's own flow at that account, no picker. */
internal fun AddAccountPrompt.signerIntroRequest(claimData: ClaimData) = SignerIntroRequest(
    flow = SignerIntroFlow.ClaimInheritance(
        isOnChain = true,
        magic = claimData.magic,
        keyIndex = account,
        accountDevice = device,
    ),
    walletType = claimData.walletType,
)

/**
 * Cancelling the key flow while download-wallet waits for an account lands on the Magic Phrase
 * (the screens before were popped for the download), so the claim returns to adding keys.
 */
internal val ClaimData.returnsToKeyCollectionOnCancel: Boolean
    get() = isOnChainClaim && pendingKeyOrigins.isNotEmpty()

/** Init `key_origins`; an empty list means the plan does not name them (configurable plans). */
internal fun ClaimData.isInPlanOrigins(signer: SignerModel): Boolean = keyOrigins.isEmpty() || keyOrigins.any {
    if (signer.isMasterSigner) it.xfp.equals(signer.fingerPrint, ignoreCase = true) else it.matches(signer)
}

internal fun SignerModel.isSameAccount(other: SignerModel): Boolean =
    fingerPrint.equals(other.fingerPrint, ignoreCase = true) &&
        derivationPath.normalizedPath() == other.derivationPath.normalizedPath()
