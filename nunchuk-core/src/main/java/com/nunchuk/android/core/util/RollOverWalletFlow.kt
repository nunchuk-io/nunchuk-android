package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object RollOverWalletFlow {

    const val NONE = 0
    const val REFUND = 1
    const val PREVIEW = 2

    @IntDef(
        NONE,
        REFUND,
        PREVIEW,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class RollOverWalletFlowInfo

}