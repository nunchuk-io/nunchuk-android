package com.nunchuk.android.model

import com.nunchuk.android.type.TransactionStatus

data class Transaction(
        val txid: String = "",
        val inputs: List<TxInput> = emptyList(),
        val outputs: List<TxOutput> = emptyList(),
        val userOutputs: List<TxOutput> = emptyList(),
        val receiveOutput: List<TxOutput> = emptyList(),
        val changeIndex: Int = 0,
        val m: Int = 0,
        val signers: Map<String, Boolean> = emptyMap(),
        val memo: String = "",
        val status: TransactionStatus = TransactionStatus.PENDING_CONFIRMATION,
        val replacedByTxid: String = "",
        val fee: Double = 0.0,
        val feeRate: Double = 0.0,
        val blocktime: Long = 0L,
        val subtractFeeFromAmount: Boolean = false,
        val isReceive: Boolean = false,
        val subAmount: Double = 0.0
)
