package com.nunchuk.android.messages.components.detail.holder

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.WalletEventType

internal class NunchukNotificationHolder(
    val binding: ItemNunchukNotificationBinding,
    val viewConfig: () -> Unit,
    val finalizeWallet: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukWalletMessage) {
        val sender = model.sender
        when (model.msgType) {
            WalletEventType.JOIN -> {
                val map = model.timelineEvent.root.content?.toMap().orEmpty()
                val body = (map["body"] as Map<String, String>?).orEmpty()
                val fingerPrint = body["key"]?.toSigner()?.fingerPrint.orEmpty()
                binding.notification.text = getHtmlString(R.string.nc_message_wallet_join, sender, fingerPrint)
                binding.root.setOnClickListener { viewConfig() }
            }
            WalletEventType.CREATE -> {
                binding.notification.text = getString(R.string.nc_message_wallet_created)
            }
            WalletEventType.CANCEL -> {
                binding.notification.text = getHtmlString(R.string.nc_message_wallet_cancel, sender)
            }
            WalletEventType.READY -> {
                if (model.isOwner) {
                    binding.notification.text = getHtmlString(R.string.nc_message_wallet_finalize)
                    binding.root.setOnClickListener { finalizeWallet() }
                } else {
                    binding.notification.text = getHtmlString(R.string.nc_message_wallet_ready)
                }
            }
            else -> {
            }
        }
        if (model.msgType == WalletEventType.CREATE) {
            binding.root.background = ContextCompat.getDrawable(itemView.context, R.drawable.nc_slime_tint_background)
        } else {
            binding.root.background = ContextCompat.getDrawable(itemView.context, R.drawable.nc_rounded_whisper_disable_background)
        }

    }

}