package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import com.nunchuk.android.model.VerifyType
import kotlinx.parcelize.Parcelize

/**
 * Verification state of one claim option of an off-chain inheritance key.
 *
 * The two artifacts fail independently — a seed phrase backup from a transcription error, an
 * encrypted backup from a mis-recorded Backup Password — so the server tracks one record per
 * selected option rather than a single verify flag per key.
 */
@Parcelize
data class InheritanceKeyVerification(
    val method: ClaimOption,
    val verifyType: VerifyType = VerifyType.NONE,
) : Parcelable

/** True once this option has been dealt with; a skipped verification counts as done. */
val InheritanceKeyVerification.isResolved: Boolean
    get() = verifyType != VerifyType.NONE
