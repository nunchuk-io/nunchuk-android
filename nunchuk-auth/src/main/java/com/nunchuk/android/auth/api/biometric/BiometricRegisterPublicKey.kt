package com.nunchuk.android.auth.api.biometric

import com.google.gson.annotations.SerializedName

data class BiometricRegisterPublicKey(
    @SerializedName("public_key") val publicKey: String,
    @SerializedName("register_verification_token") val registerVerificationToken: String,
)