package com.nunchuk.android.core.signer

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

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