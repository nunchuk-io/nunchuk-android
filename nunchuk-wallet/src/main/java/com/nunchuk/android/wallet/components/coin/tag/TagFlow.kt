package com.nunchuk.android.wallet.components.coin.tag

import androidx.annotation.IntDef

object TagFlow {
    const val NONE = 0
    const val ADD = 1
    const val VIEW = 2

    @IntDef(
        NONE,
        ADD,
        VIEW,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class TagFlowInfo
}