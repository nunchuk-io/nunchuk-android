package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.databinding.ItemWalletInfoBinding
import com.nunchuk.android.model.toRoomWalletData

internal class NunchukWalletCardHolder(
    val binding: ItemWalletInfoBinding,
    val denyWallet: () -> Unit,
    val cancelWallet: () -> Unit,
    val viewConfig: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val gson = Gson()

    fun bind(model: NunchukWalletMessage) {
        val map = model.timelineEvent.root.content?.toMap().orEmpty()
        val body = gson.toJson(map["body"])
        val initData = body.toRoomWalletData(gson)
        val ratio = "${initData.requireSigners} / ${initData.totalSigners}"
        val context = itemView.context
        binding.cancelWallet.text = context.getString(
            if (model.isOwner) R.string.nc_message_cancel_wallet else R.string.nc_message_deny_wallet
        )

        binding.name.text = initData.name
        binding.configuration.text = context.getString(R.string.nc_message_creating_wallet, ratio)
        binding.pendingSignatures.text = context.getString(R.string.nc_message_pending_signers_to_assign, initData.requireSigners)
        binding.cancelWallet.setOnClickListener { if (model.isOwner) cancelWallet() else denyWallet() }
        binding.viewConfig.setOnClickListener { viewConfig() }
    }

}