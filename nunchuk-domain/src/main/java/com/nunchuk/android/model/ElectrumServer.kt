package com.nunchuk.android.model

import com.nunchuk.android.type.Chain

data class ElectrumServer(
    val id: Long = 0L,
    val url: String,
    val chain: Chain
)