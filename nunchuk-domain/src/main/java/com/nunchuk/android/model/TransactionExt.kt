package com.nunchuk.android.model

data class TransactionExt(
    val walletId: String,
    val initEventId: String,
    val transaction: Transaction
)
