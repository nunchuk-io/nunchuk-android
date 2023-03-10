package com.nunchuk.android.model.coin

data class CoinCard(
    val amount: String,
    val isLock: Boolean,
    val isScheduleBroadCast: Boolean,
    val time: Long,
    val tags: List<CoinTag>,
    val note: String,
)