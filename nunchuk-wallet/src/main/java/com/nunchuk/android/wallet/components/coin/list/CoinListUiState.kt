package com.nunchuk.android.wallet.components.coin.list

import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

data class CoinListUiState(
    val mode: CoinListMode = CoinListMode.NONE,
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap(),
    val collections: Map<Int, CoinCollection> = emptyMap(),
    val selectedCoins: Set<UnspentOutput> = setOf()
)