package com.nunchuk.android.messages.components.detail.holder

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.core.util.isConfirmed
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.TransactionEventType.*
import com.nunchuk.android.messages.util.displayNameOrId
import com.nunchuk.android.messages.util.getBodyElementValueByKey
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.toRoomWalletData

internal class NunchukTransactionNotificationHolder(
    val binding: ItemNunchukNotificationBinding,
    val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukTransactionMessage) {
        binding.root.minLines = 1
        val context = itemView.context
        binding.notification.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_whisper_color)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_info_small, 0, R.drawable.ic_arrow, 0)
        }
        when (model.msgType) {
            SIGN -> {
                bindSignTransaction(model)
            }
            REJECT -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_rejected)
            }
            RECEIVE -> {
                binding.root.minLines = 2
                bindReceiveTransaction(roomWallet = model.roomWallet, model = model)
            }
            CANCEL -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_canceled)
            }
            READY -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_ready)
            }
            BROADCAST -> {
                bindBroadcastOrConfirmedTransaction(model = model)
            }
            else -> {
                binding.notification.text = "${model.msgType}"
            }
        }
    }

    private fun bindBroadcastOrConfirmedTransaction(
        model: NunchukTransactionMessage
    ) {
        if (model.transaction != null) {
            val confirmed = model.transaction.status.isConfirmed()
            if (confirmed) {
                bindConfirmedTransaction()
            } else {
                bindBroadcastTransaction()
            }
        } else {
            bindBroadcastTransaction()
        }
    }

    private fun bindBroadcastTransaction() {
        binding.notification.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_whisper_color)
            text = getString(R.string.nc_message_transaction_broadcast)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_info_small, 0, R.drawable.ic_arrow, 0)
        }
    }

    private fun bindConfirmedTransaction() {
        binding.notification.apply {
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_green_color)
            text = getString(R.string.nc_message_transaction_confirmed)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_small, 0, R.drawable.ic_arrow, 0)
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
        model: NunchukTransactionMessage
    ) {
        model.transaction?.let {
            bindReceiveTransactionDetails(roomWallet = roomWallet, transaction = it)
        } ?: run {
            binding.notification.text = getString(R.string.nc_transaction_not_found)
        }
    }

    private fun bindReceiveTransactionDetails(roomWallet: RoomWallet?, transaction: Transaction) {
        val roomWalletData = roomWallet?.jsonContent?.toRoomWalletData()
        val messageId = if (transaction.status.isConfirmed()) R.string.nc_message_transaction_received else R.string.nc_message_transaction_receiving
        binding.notification.text = getHtmlString(
            messageId,
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