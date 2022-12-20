package com.nunchuk.android.model

data class TransactionAdditional(
    val psbt: String,
    val subAmount: Double = 0.0,
    val fee: Double = 0.0,
    val feeRate: Double = 0.0,
    val txId: String = ""
)