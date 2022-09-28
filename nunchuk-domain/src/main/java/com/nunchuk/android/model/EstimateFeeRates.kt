package com.nunchuk.android.model

data class EstimateFeeRates(
    val priorityRate: Int = 1000,
    val standardRate: Int = 1000,
    val economicRate: Int = 1000,
    val minimumFee: Int = 1000,
)

val EstimateFeeRates.defaultRate: Int
    get() = economicRate
