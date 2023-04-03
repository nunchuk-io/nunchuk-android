package com.nunchuk.android.wallet.components.coin.filter

sealed class CoinFilter {
    data class Collection(val ids: List<Int> = emptyList()) : CoinFilter()
    data class Amount(val min: Double = 0.0, val max: Double = 0.0, val isBtc: Boolean = true) :
        CoinFilter()

    data class Date(val min: Long = 0L, val max: Long = 0L) : CoinFilter()
    data class LockCoin(
        val showLockedCoin: Boolean = true,
        val showUnlockedCoin: Boolean = true,
    ) : CoinFilter()

    data class Sort(val isAscending: Boolean = false) : CoinFilter()
}