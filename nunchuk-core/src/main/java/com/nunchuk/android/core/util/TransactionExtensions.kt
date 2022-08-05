package com.nunchuk.android.core.util

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.TransactionStatus.*

fun TransactionStatus.canBroadCast() = this == READY_TO_BROADCAST

fun TransactionStatus.hadBroadcast() = this == CONFIRMED || this == NETWORK_REJECTED || this == REPLACED || this == PENDING_CONFIRMATION

fun TransactionStatus.isPending() = this == PENDING_SIGNATURES || this == READY_TO_BROADCAST

fun TransactionStatus.isPendingConfirm() = this == PENDING_CONFIRMATION

fun TransactionStatus.isShowMoreMenu() = isPending() || isPendingConfirm()

fun TransactionStatus.isConfirmed() = this == CONFIRMED

fun Transaction.getPendingSignatures() = 0.coerceAtLeast(m - signers.count(Map.Entry<String, Boolean>::value))

fun Transaction.isPendingSignatures() = status == PENDING_SIGNATURES

fun List<Transaction>.sorted(): List<Transaction> {
    return sortedWith(comparatorTransaction)
}

private val comparatorTransaction = Comparator<Transaction> { t1, t2 ->
    when {
        t1.txId == t2.replacedByTxid && t1.status == CONFIRMED && t2.status == REPLACED -> -1
        t1.txId != t2.replacedByTxid && t1.status == CONFIRMED && t2.status == REPLACED -> 1
        t1.status == CONFIRMED && t2.status == CONFIRMED -> t2.blockTime.compareTo(t1.blockTime)
        t1.status.compareTo(t2.status) == 0 -> t2.blockTime.compareTo(t1.blockTime)
        else -> t1.status.compareTo(t2.status)
    }
}

fun String.truncatedAddress(): String = if (length < PREFIX_LENGTH + SUFFIX_LENGTH) {
    this
} else {
    "${take(PREFIX_LENGTH)}...${takeLast(SUFFIX_LENGTH)}"
}

fun Transaction.hasChangeIndex() = outputs.isNotEmpty() && changeIndex >= 0 && changeIndex < outputs.size

internal const val PREFIX_LENGTH = 5
internal const val SUFFIX_LENGTH = 4
