package com.nunchuk.android.transaction.details

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Transaction

sealed class TransactionDetailsEvent {
    object SignTransactionSuccess : TransactionDetailsEvent()
    object BroadcastTransactionSuccess : TransactionDetailsEvent()
    object DeleteTransactionSuccess : TransactionDetailsEvent()
    data class TransactionDetailsError(val message: String) : TransactionDetailsEvent()
    data class ViewBlockchainExplorer(val url: String) : TransactionDetailsEvent()
}

data class TransactionDetailsState(
    val viewMore: Boolean = false,
    val transaction: Transaction = Transaction(),
    val signers: List<SignerModel> = emptyList()
)