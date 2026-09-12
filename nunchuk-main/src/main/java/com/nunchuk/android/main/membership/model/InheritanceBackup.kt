package com.nunchuk.android.main.membership.model

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

/**
 * Which verification branch the inheritance key row's action opens. Null for anything that is not
 * an inheritance key with a sharing method recorded, which keeps the legacy backup flow.
 */
fun AddKeyData.inheritanceBackupBranch(): InheritanceBackupBranch? = when {
    !isInheritanceKey || signer == null || claimOptions.isEmpty() -> null
    claimOptions.size > 1 -> InheritanceBackupBranch.BOTH
    claimOptions.first() == ClaimOption.SEED_PHRASE -> InheritanceBackupBranch.SEED_PHRASE
    else -> InheritanceBackupBranch.ENCRYPTED_BACKUP
}

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
 * [ClaimOption.ENCRYPTED_BACKUP] when this key asked for one, otherwise null.
 *
 * The encrypted-backup screens of TAPSIGNER and Coldcard have no idea what a claim option is, so
 * the key list names the artifact they are about to resolve. Null keeps the legacy behaviour,
 * where the server infers a key's single verification.
 */
fun AddKeyData.encryptedBackupClaimOption(): ClaimOption? =
    ClaimOption.ENCRYPTED_BACKUP.takeIf { it in claimOptions }

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
