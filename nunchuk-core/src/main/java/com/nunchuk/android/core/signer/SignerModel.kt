package com.nunchuk.android.core.signer

import com.nunchuk.android.model.JoinKey
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import java.util.regex.Pattern

data class SignerModel(
    val id: String,
    val name: String,
    val derivationPath: String,
    val fingerPrint: String,
    val used: Boolean = false,
    val type: SignerType = SignerType.AIRGAP,
    val software: Boolean = false,
    val localKey: Boolean = true
) {
    fun isSame(other: SignerModel) = fingerPrint == other.fingerPrint && derivationPath == other.derivationPath
}

fun SingleSigner.toModel() = SignerModel(
    id = masterSignerId,
    name = name,
    derivationPath = derivationPath,
    type = type,
    used = used,
    software = type == SignerType.SOFTWARE,
    fingerPrint = masterFingerprint
)

fun MasterSigner.toModel() = SignerModel(
    id = id,
    name = name,
    derivationPath = device.path,
    fingerPrint = device.masterFingerprint,
    type = type,
    software = software
)

fun JoinKey.toSignerModel() = SignerModel(
    id = chatId,
    name = name,
    derivationPath = derivationPath,
    fingerPrint = masterFingerprint,
    type = SignerType.valueOf(signerType)
)

data class SignerInput(
    val fingerPrint: String,
    val derivationPath: String,
    val xpub: String
)

class InvalidSignerFormatException(override val message: String) : Exception()

fun String.toSigner(): SignerInput {
    val trimmed = trim()
    val pattern = Pattern.compile("^\\[([0-9a-f]{8})/(.*)]([^/]+).*\$")
    val matcher = pattern.matcher(trimmed)
    if (matcher.find()) {
        val fingerPrint = requireNotNull(matcher.group(1))
        val derivationPath = "m/${requireNotNull(matcher.group(2))}"
        val xpub = requireNotNull(matcher.group(3))
        return SignerInput(fingerPrint = fingerPrint, derivationPath = derivationPath, xpub = xpub)
    }
    throw InvalidSignerFormatException(this)
}

fun List<SignerModel>.isContain(signer: SignerModel) = firstOrNull { it.isSame(signer) } != null
