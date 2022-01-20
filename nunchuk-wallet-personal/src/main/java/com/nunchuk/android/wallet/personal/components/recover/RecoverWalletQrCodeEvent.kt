package com.nunchuk.android.wallet.personal.components.recover

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class RecoverWalletQrCodeEvent {
    data class ImportQRCodeSuccess(val walletId: String) : RecoverWalletQrCodeEvent()
    data class ImportQRCodeError(val message: String) : RecoverWalletQrCodeEvent()
}