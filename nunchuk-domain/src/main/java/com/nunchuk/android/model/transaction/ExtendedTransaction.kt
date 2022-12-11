package com.nunchuk.android.model.transaction

import com.nunchuk.android.model.Transaction

data class ExtendedTransaction(
    val serverTransaction: ServerTransaction? = null,
    val transaction: Transaction,
)