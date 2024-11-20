package com.nunchuk.android.model.setting

data class BiometricConfig(
    val enabled: Boolean,
    val email: String,
    val userId: String,
    val privateKey: String
) {
    companion object {
        val DEFAULT = BiometricConfig(
            enabled = false,
            userId = "",
            privateKey = "",
            email = ""
        )
    }
}