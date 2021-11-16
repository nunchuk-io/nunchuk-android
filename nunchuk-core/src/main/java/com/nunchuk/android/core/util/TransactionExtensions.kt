package com.nunchuk.android.core.util

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*

fun TransactionStatus.canBroadCast() = this == READY_TO_BROADCAST

fun TransactionStatus.hadBroadcast() = this == CONFIRMED || this == NETWORK_REJECTED || this == REPLACED || this == PENDING_CONFIRMATION

fun TransactionStatus.isPending() = this == PENDING_SIGNATURES || this == READY_TO_BROADCAST

fun Transaction.getPendingSignatures() = 0.coerceAtLeast(m - signers.count { it.value })
