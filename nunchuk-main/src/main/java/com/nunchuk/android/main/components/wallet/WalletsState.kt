package com.nunchuk.android.main.components.wallet

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet

internal data class WalletsState(
    val wallets: List<Wallet> = emptyList(),
    val signers: List<SingleSigner> = emptyList()
)
