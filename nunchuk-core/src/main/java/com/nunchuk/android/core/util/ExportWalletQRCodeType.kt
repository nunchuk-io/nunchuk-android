package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object ExportWalletQRCodeType {
    const val BC_UR2_LEGACY = 0
    const val BC_UR2 = 1
    const val BBQR = 2
    const val DESCRIPTOR_QR = 3

    /** Not a wallet export: the reply handed back to a locked Jade during QR PIN unlock. */
    const val JADE_PIN = 4

    @IntDef(
        BC_UR2_LEGACY,
        BC_UR2,
        BBQR,
        DESCRIPTOR_QR,
        JADE_PIN
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ExportWalletQRCodeTypeInfo
}