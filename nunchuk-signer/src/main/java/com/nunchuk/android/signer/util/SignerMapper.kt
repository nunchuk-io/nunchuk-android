package com.nunchuk.android.signer.util

import com.nunchuk.android.model.SingleSigner

import java.util.regex.Pattern

internal data class SignerInput(val fingerPrint: String, val path: String, val xpub: String)

internal class InvalidSignerFormatException : Exception()

internal fun String.toSigner(): SignerInput {
    val pattern = Pattern.compile("^\\[([^/]+)/(.*)]([^/]+).*\$")
    val matcher = pattern.matcher(this)
    if (matcher.find()) {
        val fingerPrint = requireNotNull(matcher.group(1))
        val path = "m/${requireNotNull(matcher.group(2))}"
        val xpub = requireNotNull(matcher.group(3))
        return SignerInput(fingerPrint = fingerPrint, path = path, xpub = xpub)
    }
    throw InvalidSignerFormatException()
}

internal fun String.toSingleSigner(name: String): SingleSigner {
    val signerInput = toSigner()
    return SingleSigner(
        name = name,
        xpub = signerInput.xpub,
        derivationPath = signerInput.path,
        masterFingerprint = signerInput.fingerPrint
    )
}