package com.nunchuk.android.auth.api.biometric

import com.google.gson.annotations.SerializedName

data class BiometricChallengeRequest(
    @SerializedName("user_id") val userId: String,
)