package com.nunchuk.android.core.data.model

import android.os.Parcelable
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import kotlinx.parcelize.Parcelize

@Parcelize
data class RollOverWalletParam(
    val newWalletId: String,
    val tags: List<CoinTag>,
    val collections: List<CoinCollection>
) : Parcelable