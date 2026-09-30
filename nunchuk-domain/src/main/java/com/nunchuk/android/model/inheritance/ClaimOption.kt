package com.nunchuk.android.model.inheritance

import androidx.annotation.Keep

/**
 * How the owner passes an off-chain inheritance key to their Beneficiary. The server models this as
 * a list, not a tri-state: "do both" is simply both entries, and an empty list means the choice has
 * not been made (or the plan predates it).
 *
 * On `supported_signers[]` it is the set a device is *capable* of; on a draft/replacement signer it
 * is the owner's actual selection.
 */
@Keep
enum class ClaimOption {
    /** Share the seed phrase directly, optionally on a pre-loaded device. */
    SEED_PHRASE,

    /** Upload an encrypted backup to the server and share its Backup Password. */
    ENCRYPTED_BACKUP,
}

fun String?.toClaimOptionOrNull(): ClaimOption? =
    ClaimOption.entries.firstOrNull { it.name == this }

fun List<String>?.toClaimOptions(): List<ClaimOption> =
    this?.mapNotNull { it.toClaimOptionOrNull() }.orEmpty()
