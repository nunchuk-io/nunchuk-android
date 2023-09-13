package com.nunchuk.android.model

import com.nunchuk.android.model.byzantine.DummyTransactionType

data class DummyTransaction(
    val psbt: String,
    val pendingSignature: Int,
    val dummyTransactionType: DummyTransactionType,
    val payload: String,
    val isDraft: Boolean,
)
