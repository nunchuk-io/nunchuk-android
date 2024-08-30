package com.nunchuk.android.app.referral

import androidx.annotation.Keep

@Keep
enum class ReferralAction(val value: String) {
    VIEW("VIEW"),
    CHANGE("CHANGE"),
    PICK("PICK")
}