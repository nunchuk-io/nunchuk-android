package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * One account read off a device restored from a seed-phrase backup. A configurable on-chain draft
 * verifies the backup against it (`key` on `draft-wallets/{xfp}/verify`, NUN-10320).
 */
@Parcelize
data class RecoveredKey(
    val xfp: String,
    val derivationPath: String,
    val xpub: String,
) : Parcelable
