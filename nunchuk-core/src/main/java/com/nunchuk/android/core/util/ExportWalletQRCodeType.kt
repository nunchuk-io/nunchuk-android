package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object ExportWalletQRCodeType {
    const val BC_UR2_LEGACY = 0
    const val BC_UR2 = 1

    @IntDef(
        BC_UR2_LEGACY,
        BC_UR2,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ExportWalletQRCodeTypeInfo
}