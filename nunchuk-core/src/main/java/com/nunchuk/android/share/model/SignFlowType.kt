package com.nunchuk.android.share.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SignFlowType : Parcelable {
    data object Normal : SignFlowType(), Parcelable
    data object NormalDummy : SignFlowType(), Parcelable
    data object SignInDummy : SignFlowType(), Parcelable
    data object ClaimDummy : SignFlowType(), Parcelable
}