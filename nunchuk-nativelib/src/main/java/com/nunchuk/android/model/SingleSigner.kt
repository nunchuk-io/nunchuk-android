package com.nunchuk.android.model

data class SingleSigner(
        val name: String,
        val xpub: String,
        val publicKey: String,
        val derivationPath: String,
        val masterFingerprint: String,
        val lastHealthCheck: Long,
        val masterSignerId: String,
        val used: Boolean = false
)