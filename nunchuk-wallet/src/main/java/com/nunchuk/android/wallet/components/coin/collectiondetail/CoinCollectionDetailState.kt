package com.nunchuk.android.wallet.components.coin.collectiondetail

import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

data class CoinCollectionDetailState(
    val coinCollection: CoinCollection? = null,
    val coins: List<UnspentOutput> = emptyList(),
    val tags: Map<Int, CoinTag> = emptyMap()
)

sealed class CoinCollectionDetailEvent {
    data class Loading(val show: Boolean) : CoinCollectionDetailEvent()
    data class Error(val message: String) : CoinCollectionDetailEvent()
    object DeleteCollectionSuccess : CoinCollectionDetailEvent()
    object RemoveCoinSuccess : CoinCollectionDetailEvent()
}
