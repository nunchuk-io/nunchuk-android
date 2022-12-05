package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object InheritancePlanFlow {

    const val NONE = 0
    const val SETUP = 1
    const val VIEW = 2
    const val CLAIM = 3

    @IntDef(
        NONE,
        SETUP,
        VIEW,
        CLAIM
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class InheritancePlanFlowInfo

}