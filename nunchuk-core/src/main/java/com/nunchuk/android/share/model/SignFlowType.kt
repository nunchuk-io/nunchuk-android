package com.nunchuk.android.share.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SignFlowType : Parcelable {
    data object Normal : SignFlowType(), Parcelable
    data object NormalDummy : SignFlowType(), Parcelable
    data object SignInDummy : SignFlowType(), Parcelable
    data object ClaimDummy : SignFlowType(), Parcelable

    /**
     * Claim challenge signed by an air-gapped device over plain-text QR (Jade today): the request
     * is one static `signmessage <path> ascii:<message>` frame and the reply is the raw base64
     * signature, with none of the BBQR framing [ClaimDummy] uses for the Coldcard.
     */
    data object ClaimAirgapMessage : SignFlowType(), Parcelable
}