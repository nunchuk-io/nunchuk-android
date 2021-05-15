package com.nunchuk.android.model

import com.nunchuk.android.type.TransactionStatus

data class Transaction(
    var txId: String = "",
    var inputs: List<TxInput> = emptyList(),
    var outputs: List<TxOutput> = emptyList(),
    var userOutputs: List<TxOutput> = emptyList(),
    var receiveOutput: List<TxOutput> = emptyList(),
    var changeIndex: Int = 0,
    var m: Int = 0,
    var signers: Map<String, Boolean> = emptyMap(),
    var memo: String = "",
    var status: TransactionStatus = TransactionStatus.PENDING_SIGNATURES,
    var replacedByTxid: String = "",
    var fee: Amount = Amount.ZER0,
    var feeRate: Amount = Amount.ZER0,
    var blockTime: Long = 0L,
    var subtractFeeFromAmount: Boolean = false,
    var isReceive: Boolean = false,
    var subAmount: Amount = Amount.ZER0
)
