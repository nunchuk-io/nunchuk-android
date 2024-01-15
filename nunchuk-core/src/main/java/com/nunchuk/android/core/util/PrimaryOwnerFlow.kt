package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object PrimaryOwnerFlow {

    const val NONE = 0
    const val SETUP = 1
    const val EDIT = 2

    @IntDef(
        NONE,
        SETUP,
        EDIT,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PrimaryOwnerFlowInfo

}