package com.nunchuk.android.transaction.components.details

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Transaction

sealed class TransactionDetailsEvent {
    data class SignTransactionSuccess(val roomId: String = "") : TransactionDetailsEvent()
    data class BroadcastTransactionSuccess(val roomId: String = "") : TransactionDetailsEvent()
    object DeleteTransactionSuccess : TransactionDetailsEvent()
    data class PromptInputPassphrase(val func: (String) -> Unit) : TransactionDetailsEvent()
    data class TransactionDetailsError(val message: String) : TransactionDetailsEvent()
    data class ViewBlockchainExplorer(val url: String) : TransactionDetailsEvent()
    data class PromptTransactionOptions(val shouldShowCancel: Boolean) : TransactionDetailsEvent()
    object LoadingEvent : TransactionDetailsEvent()
}

data class TransactionDetailsState(
    val viewMore: Boolean = false,
    val transaction: Transaction = Transaction(),
    val signers: List<SignerModel> = emptyList()
)