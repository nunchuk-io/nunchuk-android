package com.nunchuk.android.model

data class Amount(var value: Long = 0, var formattedValue: String = "0.00") {

    companion object {
        val ZER0 = Amount(0, "0.00")
    }

}