package com.nunchuk.android.main.membership.wallet

sealed class CreateWalletEvent {
    data class Loading(val isLoading: Boolean) : CreateWalletEvent()
    data class ShowError(val message: String) : CreateWalletEvent()
    data class OnCreateWalletSuccess(val walletId: String, val hasColdcard: Boolean, val hasAirgap: Boolean,) : CreateWalletEvent()
}

data class CreateWalletState(
    val walletName: String = "",
) {
    companion object {
        val EMPTY = CreateWalletState()
    }
}