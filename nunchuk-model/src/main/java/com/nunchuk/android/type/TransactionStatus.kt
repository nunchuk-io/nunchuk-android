package com.nunchuk.android.type

enum class TransactionStatus {
    PENDING_SIGNATURES,
    READY_TO_BROADCAST,
    NETWORK_REJECTED,
    PENDING_CONFIRMATION,
    REPLACED,
    CONFIRMED,
}