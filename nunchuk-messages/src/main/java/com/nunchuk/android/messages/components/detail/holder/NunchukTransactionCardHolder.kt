package com.nunchuk.android.messages.components.detail.holder

import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemTransactionCardBinding
import com.nunchuk.android.messages.util.getBodyElementValueByKey
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionExtended

internal class NunchukTransactionCardHolder(
    val binding: ItemTransactionCardBinding,
    val signTransaction: () -> Unit = {},
    val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(transactions: List<TransactionExtended>, model: NunchukTransactionMessage) {
        var walletId = model.timelineEvent.getBodyElementValueByKey("wallet_id")
        if (walletId.isEmpty()) {
            walletId = transactions.firstOrNull { it.walletId.isNotEmpty() }?.walletId.orEmpty()
        }
        val initEventId = model.timelineEvent.eventId
        transactions.firstOrNull { it.initEventId == initEventId }?.let {
            bindTransaction(walletId = walletId, initEventId = initEventId, transaction = it.transaction)
        }
        CardHelper.adjustCardLayout(binding.root, binding.cardTopContainer, model.isOwner)
    }

    private fun bindTransaction(walletId: String, initEventId: String, transaction: Transaction) {
        val context = itemView.context
        binding.amount.text = transaction.totalAmount.getBTCAmount()
        binding.status.bindTransactionStatus(transaction)
        val resId = if (transaction.status.isConfirmed()) {
            R.string.nc_message_transaction_sent_to
        } else {
            R.string.nc_message_transaction_sending_to
        }
        binding.address.text = getHtmlString(resId, transaction.outputs.first().first)
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.text = context.getString(R.string.nc_message_transaction_pending_signature, pendingSigners)
        } else {
            binding.signatureStatus.text = context.getString(R.string.nc_message_transaction_enough_signature)
        }
        binding.signatureStatus.isInvisible = transaction.status.hadBroadcast()
        binding.sign.setOnClickListener { signTransaction() }
        binding.viewDetails.setOnClickListener { viewTransaction(walletId, transaction.txId, initEventId) }
    }

}