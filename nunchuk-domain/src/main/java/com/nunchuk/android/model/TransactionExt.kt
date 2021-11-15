package com.nunchuk.android.model

data class TransactionExt(val walletId: String, val initEventId: String, val transaction: Transaction)

fun List<Transaction>.sorted(): List<Transaction> {
    return sortedWith(compareBy<Transaction> { it.status }.thenByDescending { it.blockTime })
}
