package com.nunchuk.android.transaction.components.details

sealed class TransactionDetailsEvent {
    data class SignTransactionSuccess(val roomId: String = "") : TransactionDetailsEvent()

    data class BroadcastTransactionSuccess(val roomId: String = "") : TransactionDetailsEvent()

    data class DeleteTransactionSuccess(val isCancel: Boolean = false) : TransactionDetailsEvent()

    data class PromptInputPassphrase(val func: (String) -> Unit) : TransactionDetailsEvent()

    data class TransactionDetailsError(val message: String, val e: Throwable? = null) :
        TransactionDetailsEvent()

    data class ViewBlockchainExplorer(val url: String) : TransactionDetailsEvent()

    data class PromptTransactionOptions(
        val isPendingTransaction: Boolean,
        val isPendingConfirm: Boolean,
        val isRejected: Boolean,
        val masterFingerPrint: String = ""
    ) : TransactionDetailsEvent()

    data class ExportToFileSuccess(val filePath: String) : TransactionDetailsEvent()

    data class ExportTransactionError(val message: String) : TransactionDetailsEvent()

    data class UpdateTransactionMemoSuccess(val newMemo: String) : TransactionDetailsEvent()

    data class UpdateTransactionMemoFailed(val message: String) : TransactionDetailsEvent()

    object ImportTransactionFromMk4Success : TransactionDetailsEvent()

    object ExportTransactionToMk4Success : TransactionDetailsEvent()

    object LoadingEvent : TransactionDetailsEvent()

    data class NfcLoadingEvent(val isColdcard: Boolean = false) : TransactionDetailsEvent()
}