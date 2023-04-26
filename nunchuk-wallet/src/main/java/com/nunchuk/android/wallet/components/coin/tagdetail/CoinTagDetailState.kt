package com.nunchuk.android.wallet.components.coin.tagdetail

import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

data class CoinTagDetailState(
    val coinTag: CoinTag? = null,
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap(),
)

sealed class CoinTagDetailEvent {
    data class Loading(val show: Boolean) : CoinTagDetailEvent()
    data class Error(val message: String) : CoinTagDetailEvent()
    object DeleteTagSuccess : CoinTagDetailEvent()
    object UpdateTagColorSuccess : CoinTagDetailEvent()
    object RemoveCoinSuccess : CoinTagDetailEvent()
}
