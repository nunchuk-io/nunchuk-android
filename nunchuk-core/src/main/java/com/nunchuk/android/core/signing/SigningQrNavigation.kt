package com.nunchuk.android.core.signing

import com.nunchuk.android.share.model.SignFlowType

data class SigningQrNavigation(val flow: SignFlowType, val isBBQR: Boolean = false)

/** Import activities already unwrap QR messages into a bare signature. */
fun ExportRoute.Qr.navigation(): SigningQrNavigation = when (framing) {
    QrFraming.PLAIN -> {
        require(codec == PayloadCodecKind.SPECTER_MESSAGE)
        SigningQrNavigation(SignFlowType.ClaimAirgapMessage)
    }
    QrFraming.BBQR_JSON -> {
        require(codec == PayloadCodecKind.COLDCARD_JSON)
        SigningQrNavigation(SignFlowType.ClaimDummy)
    }
    QrFraming.UR -> {
        require(codec == PayloadCodecKind.RAW_PSBT)
        SigningQrNavigation(SignFlowType.NormalDummy)
    }
    QrFraming.BBQR -> {
        require(codec == PayloadCodecKind.RAW_PSBT)
        SigningQrNavigation(SignFlowType.NormalDummy, isBBQR = true)
    }
}
