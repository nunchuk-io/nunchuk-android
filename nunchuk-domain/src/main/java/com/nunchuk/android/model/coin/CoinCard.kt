package com.nunchuk.android.model.coin

import com.nunchuk.android.model.Amount

data class CoinCard(
    val id: Int = 0,
    val txId: String = "",
    val amount: Amount,
    val isLocked: Boolean,
    val isChange: Boolean = false,
    val isScheduleBroadCast: Boolean,
    val time: Long,
    val tags: List<CoinTag>,
    val note: String,
)