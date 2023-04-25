package com.nunchuk.android.wallet.components.coin.collection

import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.wallet.components.coin.tag.CoinTagListEvent

data class CoinCollectionListState(
    val collections: List<CoinCollectionAddition> = arrayListOf(),
    val selectedCoinCollections: MutableSet<Int> = hashSetOf(),
    val preSelectedCoinCollections: MutableSet<Int> = hashSetOf()
)

sealed class CoinCollectionListEvent {
    data class Loading(val show: Boolean) : CoinCollectionListEvent()
    data class Error(val message: String) : CoinCollectionListEvent()
    data class AddCoinToCollectionSuccess(val numsCoin: Int) : CoinCollectionListEvent()
}
