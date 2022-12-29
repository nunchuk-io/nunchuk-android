package com.nunchuk.android.main.nonsubscriber.intro

import com.nunchuk.android.main.nonsubscriber.intro.model.AssistedWalletPoint

data class NonSubscriberState(
    val title: String = "",
    val desc: String = "",
    val items: List<AssistedWalletPoint> = emptyList()
)