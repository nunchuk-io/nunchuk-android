package com.nunchuk.android.wallet.details

sealed class WalletInfoEvent {

    data class UpdateNameSuccessEvent(val signerName: String) : WalletInfoEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletInfoEvent()
}