package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object UnlockPinSourceFlow {

    const val NONE = 0
    const val SIGN_IN_UNKNOWN_MODE = 1

    @IntDef(
        NONE,
        SIGN_IN_UNKNOWN_MODE,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class UnlockPinSourceFlowInfo
}