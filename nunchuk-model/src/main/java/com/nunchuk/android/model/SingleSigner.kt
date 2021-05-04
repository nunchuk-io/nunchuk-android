package com.nunchuk.android.model

data class SingleSigner(
    var name: String = "",
    var xpub: String = "",
    var publicKey: String = "",
    var derivationPath: String = "",
    var masterFingerprint: String = "",
    var lastHealthCheck: Long = 0L,
    var masterSignerId: String = "",
    var used: Boolean = false
)

fun SingleSigner.toSpec(): String {
    val newPath = derivationPath.replace("m/", "")
    return "[$masterFingerprint/$newPath]$xpub"
}