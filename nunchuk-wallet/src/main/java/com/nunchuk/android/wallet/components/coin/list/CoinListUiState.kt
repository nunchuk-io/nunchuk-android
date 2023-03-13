package com.nunchuk.android.wallet.components.coin.list

import com.nunchuk.android.model.coin.CoinCard

data class CoinListUiState(
    val mode: CoinListMode = CoinListMode.NONE,
    val coins: List<CoinCard> = emptyList(),
    val selectedCoins: Set<CoinCard> = setOf()
)