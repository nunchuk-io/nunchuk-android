package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.TransactionEventType.*
import com.nunchuk.android.messages.util.displayNameOrId
import com.nunchuk.android.messages.util.getBodyElementValueByKey
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionExt
import com.nunchuk.android.model.toRoomWalletData

internal class NunchukTransactionNotificationHolder(
    val binding: ItemNunchukNotificationBinding,
    val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(roomWallet: RoomWallet?, transactions: List<TransactionExt>, model: NunchukTransactionMessage) {
        val context = itemView.context
        when (model.msgType) {
            SIGN -> {
                bindSignTransaction(model)
            }
            REJECT -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_rejected)
            }
            RECEIVE -> {
                bindReceiveTransaction(roomWallet = roomWallet, transactions = transactions, model = model)
            }
            CANCEL -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_canceled)
            }
            READY -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_ready)
            }
            BROADCAST -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_broadcast)
            }
            else -> {
                binding.notification.text = "${model.msgType}"
            }
        }
    }

    private fun bindSignTransaction(model: NunchukTransactionMessage) {
        val sender = model.sender.displayNameOrId()
        var fingerPrint = model.timelineEvent.getBodyElementValueByKey("key")
        if (fingerPrint.isEmpty()) {
            fingerPrint = model.timelineEvent.getBodyElementValueByKey("master_fingerprint")
        }
        fingerPrint = fingerPrint.replace("\"", "")
        binding.notification.text = getHtmlString(R.string.nc_message_transaction_sign, sender, fingerPrint)
    }

    private fun bindReceiveTransaction(
        roomWallet: RoomWallet?,
        transactions: List<TransactionExt>,
        model: NunchukTransactionMessage
    ) {
        val initEventId = model.timelineEvent.eventId
        transactions.firstOrNull { it.initEventId == initEventId }?.let {
            bindReceiveTransactionDetails(roomWallet = roomWallet, transaction = it.transaction)
        } ?: run {
            binding.notification.text = getString(R.string.nc_transaction_not_found)
        }
    }

    private fun bindReceiveTransactionDetails(roomWallet: RoomWallet?, transaction: Transaction) {
        val roomWalletData = roomWallet?.jsonContent?.toRoomWalletData()
        binding.notification.text = getHtmlString(
            R.string.nc_message_transaction_received,
            transaction.totalAmount.getBTCAmount(),
            roomWalletData?.name.orEmpty()
        )
        roomWallet?.let { rWallet ->
            binding.root.setOnClickListener {
                viewTransaction(
                    rWallet.walletId,
                    transaction.txId,
                    rWallet.initEventId
                )
            }
        }
    }

}