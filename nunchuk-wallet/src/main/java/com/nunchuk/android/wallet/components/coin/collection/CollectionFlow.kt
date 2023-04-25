package com.nunchuk.android.wallet.components.coin.collection

import androidx.annotation.IntDef

object CollectionFlow {
    const val NONE = 0
    const val ADD = 1
    const val VIEW = 2
    const val MOVE = 3

    @IntDef(
        NONE,
        ADD,
        VIEW,
        MOVE,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class CollectionFlowInfo
}