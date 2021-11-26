package com.nunchuk.android.model

import com.nunchuk.android.type.TransactionStatus

data class TransactionExt(val walletId: String, val initEventId: String, val transaction: Transaction)

fun List<Transaction>.sorted(): List<Transaction> {
    return sortedWith(comparatorTransaction)
}

private val comparatorTransaction = Comparator<Transaction> { t1, t2 ->
    when {
        t1.txId == t2.replacedByTxid && t1.status == TransactionStatus.CONFIRMED && t2.status == TransactionStatus.REPLACED -> -1
        t1.txId != t2.replacedByTxid && t1.status == TransactionStatus.CONFIRMED && t2.status == TransactionStatus.REPLACED -> 1
        t1.status == TransactionStatus.CONFIRMED && t2.status == TransactionStatus.CONFIRMED -> t2.blockTime.compareTo(t1.blockTime)
        t1.status.compareTo(t2.status) == 0 -> t2.blockTime.compareTo(t1.blockTime)
        else -> t1.status.compareTo(t2.status)
    }
}
