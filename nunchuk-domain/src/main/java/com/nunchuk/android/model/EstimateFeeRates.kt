package com.nunchuk.android.model

data class EstimateFeeRates(
    val priorityRate: Amount = Amount.ZER0,
    val standardRate: Amount = Amount.ZER0,
    val economicRate: Amount = Amount.ZER0,
)
