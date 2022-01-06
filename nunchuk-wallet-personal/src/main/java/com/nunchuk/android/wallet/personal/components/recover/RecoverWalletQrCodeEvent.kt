package com.nunchuk.android.wallet.personal.components.recover

sealed class RecoverWalletQrCodeEvent {
    data class ImportQRCodeSuccess(val walletId: String) : RecoverWalletQrCodeEvent()
    data class ImportQRCodeError(val message: String) : RecoverWalletQrCodeEvent()
}