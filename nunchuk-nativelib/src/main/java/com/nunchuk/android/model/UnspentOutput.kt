package com.nunchuk.android.model

data class UnspentOutput(
        val txid: String,
        val vout: Int,
        val amount: Double,
        val height: Int,
        val memo: String
)
