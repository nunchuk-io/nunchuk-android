package com.nunchuk.android.auth.api.biometric

import com.google.gson.annotations.SerializedName

data class BiometricVerifyChallengeRequest(
    @SerializedName("challenge_id") val challengeId: String,
    @SerializedName("signature") val signature: String,
)