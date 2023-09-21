package com.nunchuk.android.model.byzantine

import com.nunchuk.android.type.TransactionStatus

data class DummyTransactionUpdate(
    val status: TransactionStatus,
    val pendingSignatures: Int,
)