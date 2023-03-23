package com.nunchuk.android.wallet.components.coin.tag

import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition

data class CoinTagListState(
    val tags: List<CoinTagAddition> = arrayListOf(),
    val selectedCoinTags: List<Int> = arrayListOf(),
    val coinTagInputHolder: CoinTag? = null
)

sealed class CoinTagListEvent {
    data class Loading(val show: Boolean) : CoinTagListEvent()
    data class Error(val message: String) : CoinTagListEvent()
    data class AddCoinToTagSuccess(val numsCoin: Int) : CoinTagListEvent()
}

