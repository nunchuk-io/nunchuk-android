package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.TransactionEventType.*

internal class NunchukTransactionNotificationHolder(
    val binding: ItemNunchukNotificationBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukTransactionMessage) {
        val sender = model.sender
        when (model.msgType) {
            SIGN -> {
                val map = model.timelineEvent.root.content?.toMap().orEmpty()
                val body = (map["body"] as Map<String, String>?).orEmpty()
                val fingerPrint = body["key"]?.toSigner()?.fingerPrint.orEmpty()
                binding.notification.text = getHtmlString(R.string.nc_message_transaction_sign, sender, fingerPrint)
            }
            REJECT -> {
            }
            CANCEL -> {
            }
            READY -> {
            }
            else -> {
            }
        }
    }

}