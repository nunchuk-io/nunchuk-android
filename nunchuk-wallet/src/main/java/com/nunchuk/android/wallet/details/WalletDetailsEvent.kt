package com.nunchuk.android.wallet.details

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.Wallet

sealed class WalletDetailsEvent {
    data class UpdateUnusedAddress(val address: String) : WalletDetailsEvent()
    data class SendMoneyEvent(val amount: Amount) : WalletDetailsEvent()
    data class WalletDetailsError(val message: String) : WalletDetailsEvent()
    data class OpenDynamicQRScreen(val descriptors: List<String> = emptyList()) : WalletDetailsEvent()
    data class UploadWalletEvent(val filePath: String) : WalletDetailsEvent()
    object DeleteWalletSuccess : WalletDetailsEvent()
}

data class WalletDetailsState(
    val wallet: Wallet = Wallet(),
    val transactions: List<Transaction> = emptyList()
)