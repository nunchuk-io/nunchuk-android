package com.nunchuk.android.wallet.components.coin.collection

data class CoinCollectionUiState(
    val selectedTags: Set<Int> = emptySet(),
    val isExist: Boolean = false,
)