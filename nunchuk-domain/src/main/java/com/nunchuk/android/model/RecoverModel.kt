package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class RecoverWalletType {
    QR_CODE, FILE;
}

@Parcelize
data class RecoverWalletData(
    val type: RecoverWalletType,
    val filePath: String? = null,
    val walletId: String? = null
) : Parcelable
