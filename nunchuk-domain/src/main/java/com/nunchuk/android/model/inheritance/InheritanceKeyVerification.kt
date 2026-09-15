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

/**
 * True once this option has been dealt with; a skipped verification counts as done.
 *
 * This is what lets the owner move on — it is not the same as [isVerified], which is what the key
 * row needs before it can go green.
 */
val InheritanceKeyVerification.isResolved: Boolean
    get() = verifyType != VerifyType.NONE

/** True only when the artifact was actually checked; a skipped verification is not verified. */
val InheritanceKeyVerification.isVerified: Boolean
    get() = verifyType == VerifyType.APP_VERIFIED || verifyType == VerifyType.SELF_VERIFIED
