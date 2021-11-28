package com.nunchuk.android.messages.components.detail.holder

import android.view.Gravity
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.bindTransactionStatus
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getPendingSignatures
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemTransactionInfoBinding
import com.nunchuk.android.messages.util.getBodyElementValueByKey
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionExtended

internal class NunchukTransactionCardHolder(
    val binding: ItemTransactionInfoBinding,
    val signTransaction: () -> Unit = {},
    val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(transactions: List<TransactionExtended>, model: NunchukTransactionMessage) {
        val walletId = model.timelineEvent.getBodyElementValueByKey("wallet_id")
        val initEventId = model.timelineEvent.eventId
        transactions.firstOrNull { it.initEventId == initEventId }?.let {
            bindTransaction(walletId = walletId, initEventId = initEventId, transaction = it.transaction)
        }
        binding.root.gravity = if (model.isOwner) Gravity.END else Gravity.START
    }

    private fun bindTransaction(walletId: String, initEventId: String, transaction: Transaction) {
        val context = itemView.context
        binding.amount.text = transaction.totalAmount.getBTCAmount()
        binding.status.bindTransactionStatus(transaction.status)
        binding.address.text = getHtmlString(R.string.nc_message_transaction_sending_to, transaction.outputs.first().first)
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.text = context.getString(R.string.nc_message_transaction_pending_signature, pendingSigners)
        } else {
            binding.signatureStatus.text = context.getString(R.string.nc_message_transaction_enough_signature)
        }
        binding.sign.setOnClickListener { signTransaction() }
        binding.viewDetails.setOnClickListener { viewTransaction(walletId, transaction.txId, initEventId) }
    }

}