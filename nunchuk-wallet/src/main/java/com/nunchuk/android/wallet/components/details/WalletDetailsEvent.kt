package com.nunchuk.android.wallet.components.details

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.WalletExtended

sealed class WalletDetailsEvent {
    data class Loading(val loading: Boolean) : WalletDetailsEvent()
    data class UpdateUnusedAddress(val address: String) : WalletDetailsEvent()
    data class SendMoneyEvent(val walletExtended: WalletExtended) : WalletDetailsEvent()
    data class WalletDetailsError(val message: String) : WalletDetailsEvent()
    data class OpenDynamicQRScreen(val descriptors: List<String> = emptyList()) : WalletDetailsEvent()
    data class UploadWalletConfigEvent(val filePath: String) : WalletDetailsEvent()
    data class BackupWalletDescriptorEvent(val descriptor: String) : WalletDetailsEvent()
    object DeleteWalletSuccess : WalletDetailsEvent()
    object ImportPSBTSuccess : WalletDetailsEvent()
}

data class WalletDetailsState(
    val walletExtended: WalletExtended = WalletExtended(),
    val transactions: List<Transaction> = emptyList()
)