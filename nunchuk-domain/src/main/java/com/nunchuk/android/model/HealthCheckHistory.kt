package com.nunchuk.android.model

import androidx.annotation.Keep

data class HealthCheckHistory(
    val id: String,
    val type: String,
    val createdTimeMillis: Long,
    val walletLocalId: String,
    val walletId: String,
    val dummyTransactionId: String
)

@Keep
enum class KeyHealthType {
    HEALTH_CHECK,
    DUMMY_TRANSACTION,
    TRANSACTION
}