package com.nunchuk.android.core.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InheritanceClaimTxDetailInfo(
    val changePos: Int
) : Parcelable