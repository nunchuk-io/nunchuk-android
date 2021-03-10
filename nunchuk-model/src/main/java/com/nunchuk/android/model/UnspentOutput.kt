package com.nunchuk.android.model

data class UnspentOutput(
        val txid: String = "",
        val vout: Int = 0,
        val amount: Double = 0.0,
        val height: Int = 0,
        val memo: String = ""
)
