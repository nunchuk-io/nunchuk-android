package com.nunchuk.android.share.model

import com.nunchuk.android.model.Transaction

data class ExtendTransaction(
    val transaction: Transaction,
    val walletId: String? = null
)