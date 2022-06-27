package com.nunchuk.android.model

data class TransactionExtended(
    val walletId: String,
    val initEventId: String,
    val transaction: Transaction
)

data class WalletExtended(
    val wallet: Wallet = Wallet(),
    val isShared: Boolean = true,
    val roomWallet: RoomWallet? = null
)
