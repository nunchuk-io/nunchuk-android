package com.nunchuk.android.model

import com.nunchuk.android.type.TransactionStatus

data class Transaction(
        val txid: String,
        val inputs: List<TxInput>,
        val outputs: List<TxOutput>,
        val userOutputs: List<TxOutput>,
        val receiveOutput: List<TxOutput>,
        val changeIndex: Int,
        val m: Int,
        val signers: Map<String, Boolean>,
        val memo: String,
        val status: TransactionStatus,
        val replacedByTxid: String,
        val fee: Double,
        val feeRate: Double,
        val blocktime: Long,
        val subtractFeeFromAmount: Boolean,
        val isReceive: Boolean,
        val subAmount: Double
)
