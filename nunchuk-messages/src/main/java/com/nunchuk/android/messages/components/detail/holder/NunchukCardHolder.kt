package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NunchukWalletMessage
import com.nunchuk.android.messages.components.detail.toRoomWalletData
import com.nunchuk.android.messages.databinding.ItemWalletInfoBinding
import timber.log.Timber

internal class NunchukCardHolder(
    val binding: ItemWalletInfoBinding,
    val cancelWallet: () -> Unit,
    val viewConfig: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val gson = Gson()

    fun bind(model: NunchukWalletMessage) {
        val map = model.timelineEvent.root.content?.toMap().orEmpty()
        val body = gson.toJson(map["body"])
        Timber.d("body:${gson.toJson(map["body"])}")
        val initData = body.toRoomWalletData(gson)
        val ratio = "${initData.requireSigners} / ${initData.totalSigners}"

        binding.name.text = initData.name
        binding.configuration.text = itemView.context.getString(R.string.nc_message_creating_wallet, ratio)
        binding.pendingSignatures.text = itemView.context.getString(R.string.nc_message_pending_signers_to_assign, initData.requireSigners)
        binding.cancelWallet.setOnClickListener { cancelWallet() }
        binding.viewConfig.setOnClickListener { viewConfig() }
    }

}