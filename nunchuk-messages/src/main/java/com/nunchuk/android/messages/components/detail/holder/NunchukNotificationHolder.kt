package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.WalletEventType

internal class NunchukNotificationHolder(val binding: ItemNunchukNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukWalletMessage) {
        val sender = model.sender
        when (model.msgType) {
            WalletEventType.JOIN -> {
                val map = model.timelineEvent.root.content?.toMap().orEmpty()
                val body = (map["body"] as Map<String, String>?).orEmpty()
                val fingerPrint = body["key"]?.toSigner()?.fingerPrint.orEmpty()
                binding.notification.text = getString(R.string.nc_message_wallet_join, sender, fingerPrint)
            }
            WalletEventType.CREATE -> {
                binding.notification.text = getString(R.string.nc_message_wallet_created)
            }
            WalletEventType.CANCEL -> {
                binding.notification.text = getString(R.string.nc_message_wallet_cancel, sender)
            }
            WalletEventType.READY -> {
                binding.notification.text = getString(R.string.nc_message_wallet_ready)
            }
            else -> {
            }
        }

    }

}