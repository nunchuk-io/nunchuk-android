package com.nunchuk.android.wallet.shared.components.recover

import com.nunchuk.android.model.Wallet

sealed class RecoverSharedWalletEvent {
    data class RecoverSharedWalletSuccess(
        val wallet: Wallet = Wallet(),
    ) : RecoverSharedWalletEvent()

    object WalletNameRequiredEvent : RecoverSharedWalletEvent()
    data class WalletSetupDoneEvent(
        val walletName: String
    ) : RecoverSharedWalletEvent()

    data class ShowError(val message: String) : RecoverSharedWalletEvent()
}

data class RecoverSharedWalletState(
    val walletName: String = "",
)