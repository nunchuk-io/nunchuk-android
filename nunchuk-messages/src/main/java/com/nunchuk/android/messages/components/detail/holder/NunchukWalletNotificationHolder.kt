package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.toSigner
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.databinding.ItemNunchukNotificationBinding
import com.nunchuk.android.messages.util.WalletEventType
import com.nunchuk.android.messages.util.bindNotificationBackground
import com.nunchuk.android.messages.util.getBodyElementValueByKey

internal class NunchukWalletNotificationHolder(
    val binding: ItemNunchukNotificationBinding,
    val viewConfig: () -> Unit,
    val finalizeWallet: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: NunchukWalletMessage) {
        val sender = model.sender
        when (model.msgType) {
            WalletEventType.JOIN -> {
                val fingerPrint = try {
                    model.timelineEvent.getBodyElementValueByKey("key").toSigner().fingerPrint
                } catch (t: Throwable) {
                    CrashlyticsReporter.recordException(t)
                    ""
                }
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
        binding.root.bindNotificationBackground(model.msgType == WalletEventType.CREATE)
    }

}