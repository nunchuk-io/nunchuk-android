package com.nunchuk.android.model

import com.nunchuk.android.type.Chain

data class ElectrumServer(
    val id: Long,
    val url: String,
    val chain: Chain
)