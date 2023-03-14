package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinTagAddition(val coinTag: CoinTag, val numCoins: Int = 0) : Parcelable