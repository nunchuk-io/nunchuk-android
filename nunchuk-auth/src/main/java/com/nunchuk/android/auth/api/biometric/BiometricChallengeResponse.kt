package com.nunchuk.android.auth.api.biometric

import com.google.gson.annotations.SerializedName

class BiometricChallengeResponse(
    @SerializedName("challenge") val challenge: String?,
    @SerializedName("challenge_id") val challengeId: String?,
)