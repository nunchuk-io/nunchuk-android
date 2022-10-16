package com.nunchuk.android.core.signer

import androidx.annotation.IntDef

object PrimaryKeyFlow {

    const val NONE = 0
    const val SIGN_UP = 1
    const val SIGN_IN = 2
    const val REPLACE = 3

    @IntDef(
        NONE,
        SIGN_UP,
        SIGN_IN,
        REPLACE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PrimaryFlowInfo

    fun Int.isPrimaryKeyFlow() = this != NONE
    fun Int.isSignUpFlow() = this == SIGN_UP
    fun Int.isSignInFlow() = this == SIGN_IN
    fun Int.isReplaceFlow() = this == REPLACE
}