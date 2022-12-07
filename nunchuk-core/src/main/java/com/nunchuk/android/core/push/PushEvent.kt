package com.nunchuk.android.core.push

sealed class PushEvent {
    data class CosigningEvent(val walletId: String, val transactionId: String) : PushEvent()
}