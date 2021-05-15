package com.nunchuk.android.model

data class Amount(var value: Long = 0) {

    companion object {
        val ZER0 = Amount(0)
    }

}