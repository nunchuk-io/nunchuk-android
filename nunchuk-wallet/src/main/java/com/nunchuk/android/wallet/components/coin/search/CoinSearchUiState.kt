package com.nunchuk.android.wallet.components.coin.search

import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.components.coin.list.CoinListMode

data class CoinSearchUiState(
    val mode: CoinListMode = CoinListMode.NONE,
    val coins: List<UnspentOutput> = emptyList(),
    val selectedCoins: Set<UnspentOutput> = setOf()
)