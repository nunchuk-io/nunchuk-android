package com.nunchuk.android.wallet.personal.components.recover

sealed class RecoverWalletEvent {
    data class ImportWalletSuccessEvent(val walletId: String, val walletName: String): RecoverWalletEvent()
    data class ImportWalletErrorEvent(val message: String): RecoverWalletEvent()
    object WalletNameRequiredEvent : RecoverWalletEvent()
    data class WalletSetupDoneEvent(
        val walletName: String
    ) : RecoverWalletEvent()
}

data class RecoverWalletState(
    val walletName: String = ""
)