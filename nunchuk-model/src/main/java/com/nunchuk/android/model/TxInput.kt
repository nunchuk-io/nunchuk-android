package com.nunchuk.android.model

data class TxInput(
    var first: String = "",
    var second: Int = 0
)

data class TxOutput(
    var first: String = "",
    var second: Amount = Amount.ZER0
)