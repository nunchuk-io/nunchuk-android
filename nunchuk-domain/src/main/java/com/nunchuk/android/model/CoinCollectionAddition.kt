package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinCollectionAddition(val collection: CoinCollection, val numCoins: Int = 0) : Parcelable