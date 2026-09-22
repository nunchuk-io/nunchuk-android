package com.nunchuk.android.main.membership.model

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.inheritance.InheritanceKeyVerification
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

/**
 * What the server records against one off-chain inheritance key: how the owner chose to pass it on,
 * and how far each of those artifacts has got.
 *
 * It is held apart from [AddKeyData] because the same key can be tracked in two places — on the
 * draft wallet while the wallet is being built, and on the wallet's replacement when the key is
 * being swapped out — and both rows render and dispatch identically off this state.
 */
data class InheritanceClaimState(
    /** The owner's selection. Empty means unset, or a legacy plan that predates the choice. */
    val claimOptions: List<ClaimOption> = emptyList(),
    /** One record per entry in [claimOptions]; empty on a legacy plan. */
    val verifications: List<InheritanceKeyVerification> = emptyList(),
    /**
     * Whether the encrypted backup file has reached the server. The verification record alone
     * cannot tell "no backup yet" from "backup uploaded, not verified yet", and the row
     * distinguishes them: the first offers "Backup", the second "Verify backup".
     */
    val hasEncryptedBackupFile: Boolean = false,
) {
    /** Whether the owner has recorded a sharing method at all. */
    val isUnset: Boolean
        get() = claimOptions.isEmpty()

    /**
     * Whether [option] has been dealt with — verified or deliberately skipped. The server keeps one
     * record per method and can still hold records for a method the owner has since dropped, so the
     * chosen options — not [verifications] — decide what counts.
     */
    fun isResolved(option: ClaimOption): Boolean =
        verifications.any { it.method == option && it.verifyType != VerifyType.NONE }

    /** Whether [option] was actually checked. A skipped verification is resolved but not verified. */
    fun isVerified(option: ClaimOption): Boolean = verifications.any {
        it.method == option &&
                (it.verifyType == VerifyType.APP_VERIFIED || it.verifyType == VerifyType.SELF_VERIFIED)
    }

    /** How many of the chosen sharing methods are verified (or deliberately skipped). */
    val resolvedCount: Int
        get() = claimOptions.count { isResolved(it) }

    /** True once every chosen sharing method has been dealt with. */
    val isSettled: Boolean
        get() = claimOptions.isNotEmpty() && resolvedCount == claimOptions.size

    /** True only once every chosen sharing method has actually been verified. */
    val isFullyVerified: Boolean
        get() = claimOptions.isNotEmpty() && claimOptions.all { isVerified(it) }

    /**
     * Still owes something the wizard insists on — a sharing method, or an untouched verification
     * of one it chose. A deliberately skipped verification counts as settled, so the owner is
     * never trapped; only silence blocks.
     */
    val isIncomplete: Boolean
        get() = isUnset || !isSettled

    /**
     * True while the owner asked for an encrypted backup and none has been uploaded yet, which is
     * what makes the row's action "Backup" rather than "Verify backup".
     */
    val needsEncryptedBackupUpload: Boolean
        get() = ClaimOption.ENCRYPTED_BACKUP in claimOptions &&
                stateOf(ClaimOption.ENCRYPTED_BACKUP) == ClaimOptionState.NOT_UPLOADED

    /**
     * [ClaimOption.ENCRYPTED_BACKUP] when this key asked for one, otherwise null.
     *
     * The encrypted-backup screens of TAPSIGNER and Coldcard have no idea what a claim option is,
     * so the caller names the artifact they are about to resolve. Null keeps the legacy behaviour,
     * where the server infers a key's single verification.
     */
    fun encryptedBackupClaimOption(): ClaimOption? =
        ClaimOption.ENCRYPTED_BACKUP.takeIf { it in claimOptions }

    /**
     * Which verification branch the row's action opens, or null when no sharing method is recorded
     * — which keeps the legacy backup flow.
     */
    fun branch(): InheritanceBackupBranch? = when {
        claimOptions.isEmpty() -> null
        claimOptions.size > 1 -> InheritanceBackupBranch.BOTH
        claimOptions.first() == ClaimOption.SEED_PHRASE -> InheritanceBackupBranch.SEED_PHRASE
        else -> InheritanceBackupBranch.ENCRYPTED_BACKUP
    }

    /**
     * The status line under an inheritance key: one entry per sharing method the owner chose, in
     * the order the design lists them. Empty while the choice has not been made — the row shows
     * "sharing method not set" instead.
     */
    fun statuses(): List<ClaimOptionStatus> = CLAIM_STATUS_ORDER.filter { it in claimOptions }
        .map { ClaimOptionStatus(option = it, state = stateOf(it)) }

    /**
     * How far [option] has got. The server records a verification only once there is something to
     * verify, so an encrypted backup with no record splits by whether its file has been uploaded.
     * Shared by the key row's status line and the verify-backups checklist, so both read one key
     * the same way.
     */
    fun stateOf(option: ClaimOption): ClaimOptionState {
        val verifyType =
            verifications.firstOrNull { it.method == option }?.verifyType ?: VerifyType.NONE
        return when {
            verifyType == VerifyType.SKIPPED_VERIFICATION -> ClaimOptionState.SKIPPED
            verifyType != VerifyType.NONE -> ClaimOptionState.VERIFIED
            option == ClaimOption.ENCRYPTED_BACKUP && !hasEncryptedBackupFile ->
                ClaimOptionState.NOT_UPLOADED

            else -> ClaimOptionState.PENDING
        }
    }
}

/**
 * The claim-side state the server records against this key, whether it sits on the draft wallet
 * or on a wallet's replacement. [hasLocalBackupFile] lets a caller count a backup the local step
 * already knows about before the server has caught up.
 */
fun SignerServer.toInheritanceClaimState(hasLocalBackupFile: Boolean = false) =
    InheritanceClaimState(
        claimOptions = claimOptions,
        verifications = verifications,
        hasEncryptedBackupFile = !userBackUpFileName.isNullOrEmpty() || hasLocalBackupFile,
    )

/**
 * Which verification branch the inheritance key row's action opens. Null for anything that is not
 * an inheritance key with a sharing method recorded, which keeps the legacy backup flow.
 */
fun AddKeyData.inheritanceBackupBranch(): InheritanceBackupBranch? =
    if (!isInheritanceKey || signer == null) null else claimState.branch()

/**
 * The vendor whose instructions the encrypted-backup screens should show.
 *
 * Coldcard is the fallback because it is the flow's first user and reports itself in several
 * shapes — `COLDCARD_NFC`, or air-gapped carrying the tag — while every other device that can
 * make an encrypted backup names itself with a tag.
 */
val SignerModel.backupVendorTag: SignerTag
    get() = tags.firstOrNull { it != SignerTag.INHERITANCE } ?: SignerTag.COLDCARD

/**
 * Whether the key still owes the encrypted backup that gates Continue on the key list.
 *
 * The old rule assumed an inheritance key was always a Coldcard on its way to an encrypted backup,
 * which under BYOH blocks the wizard forever on a device that has no such backup to make. Once the
 * owner has recorded a sharing method that method decides: only a key that asked for an encrypted
 * backup, and has not produced one, is missing anything. A legacy plan records no sharing method
 * and keeps the old rule.
 *
 * [extra] is the locally tracked step data; `userKeyFileName` is empty until a backup is uploaded.
 */
fun AddKeyData.owesEncryptedBackup(extra: SignerExtra?): Boolean = when {
    !type.isAddInheritanceKey || signer == null -> false
    claimOptions.isNotEmpty() -> ClaimOption.ENCRYPTED_BACKUP in claimOptions &&
            !isClaimOptionResolved(ClaimOption.ENCRYPTED_BACKUP)

    // On a legacy plan a TAPSIGNER always backed up while it was being added, so it was never
    // the one missing a backup.
    signer.type == SignerType.NFC -> false
    else -> extra != null && extra.userKeyFileName.isEmpty()
}

/**
 * How far one chosen sharing method of an off-chain inheritance key has got.
 *
 * The owner can settle a method without checking it, so "dealt with" and "verified" are separate
 * states: only [VERIFIED] turns the key row green.
 */
enum class ClaimOptionState {
    /** The encrypted backup file has not reached the server yet. Never a seed phrase state. */
    NOT_UPLOADED,

    /** The artifact exists but has not been checked. */
    PENDING,

    /** The owner deliberately passed on the verification. */
    SKIPPED,

    /** Checked, by the app or by the owner. */
    VERIFIED,
}

/** One sharing method of an inheritance key and how far it has got, as the key row renders it. */
data class ClaimOptionStatus(
    val option: ClaimOption,
    val state: ClaimOptionState,
)

/** Design order of the key row's status line, independent of how the server lists the options. */
private val CLAIM_STATUS_ORDER = listOf(ClaimOption.ENCRYPTED_BACKUP, ClaimOption.SEED_PHRASE)

/**
 * The status line under an inheritance key, or empty for every other key — which keeps its
 * existing row.
 */
fun AddKeyData.claimStatuses(): List<ClaimOptionStatus> {
    if (!showsClaimStatus) return emptyList()
    return claimState.statuses()
}

/** @see InheritanceClaimState.needsEncryptedBackupUpload */
val AddKeyData.needsEncryptedBackupUpload: Boolean
    get() = claimState.needsEncryptedBackupUpload

/** @see InheritanceClaimState.encryptedBackupClaimOption */
fun AddKeyData.encryptedBackupClaimOption(): ClaimOption? = claimState.encryptedBackupClaimOption()

/**
 * The three tails of the off-chain inheritance flow, one per sharing method the owner can choose.
 * The device does not decide this — a Coldcard whose owner chose the seed phrase goes down
 * [SEED_PHRASE], not down its own encrypted-backup flow.
 */
enum class InheritanceBackupBranch {
    /** Back up the seed phrase, then restore it onto a device and match the public key. */
    SEED_PHRASE,

    /** Create an encrypted backup on the device, upload it, then verify it. */
    ENCRYPTED_BACKUP,

    /** Both artifacts, tracked separately on a checklist. */
    BOTH,
}
