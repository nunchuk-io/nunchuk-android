package com.nunchuk.android.model.byzantine

import com.nunchuk.android.type.TransactionStatus

data class SignInDummyTransactionUpdate(
    val status: TransactionStatus,
    val pendingSignatures: Int,
    val userId: String,
    val tokenId: String,
    val deviceId: String
)