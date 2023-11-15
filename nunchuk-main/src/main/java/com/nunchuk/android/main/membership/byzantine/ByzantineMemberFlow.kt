package com.nunchuk.android.main.membership.byzantine

import androidx.annotation.IntDef

object ByzantineMemberFlow {

    const val NONE = 0
    const val SETUP = 1
    const val EDIT = 2

    @IntDef(
        NONE,
        SETUP,
        EDIT,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ByzantineMemberFlowInfo

}