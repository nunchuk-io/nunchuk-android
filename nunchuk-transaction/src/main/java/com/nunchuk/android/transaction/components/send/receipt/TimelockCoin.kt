package com.nunchuk.android.transaction.components.send.receipt

import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput

data class TimelockCoin(
    val coins: List<UnspentOutput>,
    val timelock: Long,
    val lockedCoins: List<UnspentOutput>,
    val signingPath: SigningPath?
)