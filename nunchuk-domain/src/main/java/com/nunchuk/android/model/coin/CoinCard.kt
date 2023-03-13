package com.nunchuk.android.model.coin

import kotlin.random.Random

data class CoinCard(
    val id: Long = Random(1000L).nextLong(), // TODO Hai
    val amount: String,
    val isLock: Boolean,
    val isScheduleBroadCast: Boolean,
    val time: Long,
    val tags: List<CoinTag>,
    val note: String,
)