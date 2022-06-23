package com.nunchuk.android.core.domain.data


const val BTC_AND_FIXED_PRECISION = 1
const val BTC = 2
const val SAT = 3

var CURRENT_DISPLAY_UNIT_TYPE = BTC_AND_FIXED_PRECISION


data class DisplayUnitSetting(
    val useBTC: Boolean = true,
    val showBTCPrecision : Boolean = true,
    val useSAT: Boolean = false
) {
    fun getCurrentDisplayUnitType() = when {
        useBTC && showBTCPrecision -> BTC_AND_FIXED_PRECISION
        useSAT -> SAT
        else -> BTC
    }
}
