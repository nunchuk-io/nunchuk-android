package com.nunchuk.android.core.signer

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import java.util.regex.Pattern

data class SignerModel(
    val id: String,
    val name: String,
    val fingerPrint: String,
    val used: Boolean = false,
    val software: Boolean = false
)

fun SingleSigner.toModel() = SignerModel(
    id = masterSignerId,
    name = name,
    used = used,
    fingerPrint = masterFingerprint
)

fun MasterSigner.toModel() = SignerModel(
    id = id,
    name = name,
    fingerPrint = device.masterFingerprint,
    software = true
)

data class SignerInput(
    val fingerPrint: String,
    val derivationPath: String,
    val xpub: String
)

class InvalidSignerFormatException : Exception()

fun String.toSigner(): SignerInput {
    val pattern = Pattern.compile("^\\[([^/]+)/(.*)]([^/]+).*\$")
    val matcher = pattern.matcher(this)
    if (matcher.find()) {
        val fingerPrint = requireNotNull(matcher.group(1))
        val derivationPath = "m/${requireNotNull(matcher.group(2))}"
        val xpub = requireNotNull(matcher.group(3))
        return SignerInput(fingerPrint = fingerPrint, derivationPath = derivationPath, xpub = xpub)
    }
    throw InvalidSignerFormatException()
}
