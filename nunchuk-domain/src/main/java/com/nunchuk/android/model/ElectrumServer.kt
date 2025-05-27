package com.nunchuk.android.model

import com.nunchuk.android.type.Chain

data class ElectrumServer(
    val id: Long = 0L,
    val name: String = "",
    val url: String,
    val chain: Chain
)