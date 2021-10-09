package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.TransactionEventType.*
import com.nunchuk.android.messages.util.displayNameOrId
import com.nunchuk.android.messages.util.getBodyElementValueByKey

internal class NunchukTransactionNotificationHolder(
    val binding: ItemNunchukNotificationBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukTransactionMessage) {
        val sender = model.sender.displayNameOrId()
        val context = itemView.context
        when (model.msgType) {
            SIGN -> {
                val fingerPrint = model.timelineEvent.getBodyElementValueByKey("master_fingerprint")
                binding.notification.text = getHtmlString(R.string.nc_message_transaction_sign, sender, fingerPrint)
            }
            REJECT -> {
                binding.notification.text = context.getString(R.string.nc_message_transaction_rejected)
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

}