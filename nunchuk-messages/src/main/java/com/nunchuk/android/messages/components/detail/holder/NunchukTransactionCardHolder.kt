package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukTransactionMessage
import com.nunchuk.android.messages.databinding.ItemTransactionInfoBinding
import timber.log.Timber

internal class NunchukTransactionCardHolder(
    val binding: ItemTransactionInfoBinding,
    val signTransaction: () -> Unit = {},
    val viewDetails: () -> Unit = {}
) : RecyclerView.ViewHolder(binding.root) {

    private val gson = Gson()

    fun bind(model: NunchukTransactionMessage) {
        // TODO
        val map = model.timelineEvent.root.content?.toMap().orEmpty()
        val body = gson.toJson(map["body"])
        Timber.d("[NunchukTransactionMessage]::$body")
        val context = itemView.context
        binding.amount.text = "2.0000001 BTC"
        binding.address.text = getHtmlString(R.string.nc_message_transaction_sending_to, "FJDFSa...GKgo")
        binding.pendingSignatures.text = context.getString(R.string.nc_message_transaction_pending_signature, 1)
        binding.sign.setOnClickListener { signTransaction() }
        binding.viewDetails.setOnClickListener { viewDetails() }
    }

}